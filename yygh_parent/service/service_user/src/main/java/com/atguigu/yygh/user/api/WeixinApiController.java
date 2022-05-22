package com.atguigu.yygh.user.api;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.helper.JwtHelper;
import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.user.utils.ConstantWxPropertiesUtils;
import com.atguigu.yygh.user.utils.HttpClientUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/api/ucenter/wx")
public class WeixinApiController {
    @Autowired
    private UserInfoService userInfoService;
    //1.生成微信扫描二维码
    //返回生成二维码参数
    @GetMapping("getLoginParam")
    @ResponseBody
    public Result genQrConnect(){
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("appid", ConstantWxPropertiesUtils.WX_OPEN_APP_ID);
            map.put("scope","snsapi_login");
            var wxOpenRedirectUrl = ConstantWxPropertiesUtils.WX_OPEN_REDIRECT_URL;
            URLEncoder.encode(wxOpenRedirectUrl,"utf-8");
            map.put("redirect_uri",wxOpenRedirectUrl);
            map.put("state",System.currentTimeMillis()+"");
            return Result.ok(map);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }
    //2.回调的方法，得到用户信息
    @RequestMapping("callback")
    public String callback(String code,String state){
        //code 临时票据
        //使用code和appid以及appscrect换取access_token
        StringBuffer baseAccessTokenUrl = new StringBuffer()
                .append("https://api.weixin.qq.com/sns/oauth2/access_token")
                .append("?appid=%s")
                .append("&secret=%s")
                .append("&code=%s")
                .append("&grant_type=authorization_code");

        String accessTokenUrl = String.format(baseAccessTokenUrl.toString(),
                ConstantWxPropertiesUtils.WX_OPEN_APP_ID,
                ConstantWxPropertiesUtils.WX_OPEN_APP_SECRET,
                code);
         //使用httpclient请求这个地址
        try {
            var accessTokenInfo = HttpClientUtils.get(accessTokenUrl);
            System.out.println(accessTokenInfo);
            //从返回的字符串中获取 openid，access_token
            JSONObject jsonObject=JSONObject.parseObject(accessTokenInfo);
            var access_token = jsonObject.getString("access_token");
            var openid = jsonObject.getString("openid");
            //判断数据库是否存在危险的扫描人信息,根据openid判断
            UserInfo userInfo= userInfoService.selectWxInfoOpenId(openid);
            if(userInfo==null){
                //拿着这两个值去获取扫描人信息
                String baseUserInfoUrl = "https://api.weixin.qq.com/sns/userinfo" +
                        "?access_token=%s" +
                        "&openid=%s";
                String userInfoUrl = String.format(baseUserInfoUrl, access_token, openid);
                var resultInfo = HttpClientUtils.get(userInfoUrl);
                System.out.println(resultInfo);
                JSONObject resultUserInfoJson = JSONObject.parseObject(resultInfo);
                //解析用户信息 用户昵称 用户头像
                String nickname = resultUserInfoJson.getString("nickname");
                String headimgurl = resultUserInfoJson.getString("headimgurl");
                //获取扫码人信息添加到数据库
                userInfo=new UserInfo();
                userInfo.setNickName(nickname);
                userInfo.setOpenid(openid);
                userInfo.setStatus(1);
                userInfoService.save(userInfo);
            }

            //返回name和token的字符串
            Map<String,Object> map=new HashMap<>();
            String name = userInfo.getName();
            if(StringUtils.isEmpty(name)) {
                name = userInfo.getNickName();
            }
            if(StringUtils.isEmpty(name)) {
                name = userInfo.getPhone();
            }
            map.put("name", name);
            //判断userinfo是否有手机号，如果手机号为空，返回openid，如不为空，返回空值
            //前端判断，如果openid不为空，绑定手机号，如果为空，则不需要
            if(StringUtils.isEmpty(userInfo.getPhone())) {
                map.put("openid", userInfo.getOpenid());
            } else {
                map.put("openid", "");
            }
            String token = JwtHelper.createToken(userInfo.getId(), name);
            map.put("token", token);
            //跳转到前端
            return "redirect:" + ConstantWxPropertiesUtils.YYGH_BASE_URL + "/weixin/callback?token="+
                    map.get("token")+"&openid="+map.get("openid")+"&name="
                    +URLEncoder.encode((String) map.get("name"),"utf-8");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }
}
