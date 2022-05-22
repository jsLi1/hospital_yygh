package com.atguigu.yygh.user.service.impl;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.helper.JwtHelper;
import com.atguigu.yygh.common.result.ResultCodeEnum;
import com.atguigu.yygh.enums.AuthStatusEnum;
import com.atguigu.yygh.model.user.Patient;
import com.atguigu.yygh.model.user.UserInfo;
import com.atguigu.yygh.user.mapper.UserInfoMapper;
import com.atguigu.yygh.user.service.UserInfoService;
import com.atguigu.yygh.vo.user.LoginVo;
import com.atguigu.yygh.vo.user.UserAuthVo;
import com.atguigu.yygh.vo.user.UserInfoQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
   @Autowired
   private PatientServiceImpl patientService;
    @Override
    public Map<String, Object> loginUser(LoginVo loginVo) {
        //从loginvo获取输入的手机号和验证码
        String phone=loginVo.getPhone();
        var code = loginVo.getCode();
        //判断是否为空
        if(StringUtils.isEmpty(phone)||StringUtils.isEmpty(code)){
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }
        //TODO 判断手机验证码和输入验证码是否一致
        var rediscode = redisTemplate.opsForValue().get(phone);
        if(!code.equals(rediscode)){
            throw new YyghException(ResultCodeEnum.CODE_ERROR);
        }
        //绑定手机号码
        UserInfo userInfo = null;
        if(!StringUtils.isEmpty(loginVo.getOpenid())) {
            userInfo = this.selectWxInfoOpenId(loginVo.getOpenid());
            if(null != userInfo) {
                userInfo.setPhone(loginVo.getPhone());
                this.updateById(userInfo);
            } else {
                throw new YyghException(ResultCodeEnum.DATA_ERROR);
            }
        }
        //如果userinfo为空，正常手机登录
        if(userInfo==null){
            //判断是否是第一次登录,根据手机号查询数据库
            QueryWrapper<UserInfo> wrapper=new QueryWrapper<>();
            wrapper.eq("phone",phone);
             userInfo = baseMapper.selectOne(wrapper);
            //判断userinfo是否为空
            if(userInfo==null){
                //第一次，插入数据库
                userInfo=new UserInfo();
                userInfo.setName("");
                userInfo.setPhone(phone);
                userInfo.setStatus(1);
                baseMapper.insert(userInfo);
            }
        }
        //校验是否被禁用
        if(userInfo.getStatus() == 0) {
            throw new YyghException(ResultCodeEnum.LOGIN_DISABLED_ERROR);
        }
        //返回页面显示名称
        Map<String, Object> map = new HashMap<>();
        String name = userInfo.getName();
        if(StringUtils.isEmpty(name)) {
            name = userInfo.getNickName();
        }
        if(StringUtils.isEmpty(name)) {
            name = userInfo.getPhone();
        }
        map.put("name",name);
        //jwt token生成
        String token= JwtHelper.createToken(userInfo.getId(),name);
        map.put("token",token);
       return map;
    }

    @Override
    public UserInfo selectWxInfoOpenId(String openid) {
        QueryWrapper<UserInfo> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("openid",openid);
        var userInfo = baseMapper.selectOne(queryWrapper);
        return userInfo;
    }

    @Override
    public void userAuth(Long userId, UserAuthVo userAuthVo) {
        //用户认证
        //1.根据用户id查询用户信息
        var userInfo = baseMapper.selectById(userId);
        //2.设置认证信息
        userInfo.setName(userAuthVo.getName());
        //其他认证信息
        userInfo.setCertificatesType(userAuthVo.getCertificatesType());
        userInfo.setCertificatesNo(userAuthVo.getCertificatesNo());
        userInfo.setCertificatesUrl(userAuthVo.getCertificatesUrl());
        userInfo.setAuthStatus(AuthStatusEnum.AUTH_RUN.getStatus());
        //3.进行信息更新
        baseMapper.updateById(userInfo);
    }

    @Override
    public IPage<UserInfo> selectPage(Page<UserInfo> pageParam, UserInfoQueryVo userInfoQueryVo) {
         //通过userinfoqueryvo获取条件值
        var name = userInfoQueryVo.getKeyword(); //用户名称
        var status = userInfoQueryVo.getStatus();//用户状态
        var authStatus = userInfoQueryVo.getAuthStatus();//认证状态
        var createTimeBegin = userInfoQueryVo.getCreateTimeBegin();//开始时间
        var createTimeEnd = userInfoQueryVo.getCreateTimeEnd();//结束时间
        //对条件值进行非空判断
        QueryWrapper<UserInfo> queryWrapper=new QueryWrapper<>();
        if(!StringUtils.isEmpty(name)){
            queryWrapper.like("name",name);
        }
        if(status!=null){
            queryWrapper.eq("status",status);
        }
        if(authStatus!=null){
            queryWrapper.eq("auth_status",authStatus);
        }
        if(!StringUtils.isEmpty(createTimeBegin)){
            queryWrapper.gt("create_time",createTimeBegin);
        }
        if(!StringUtils.isEmpty(createTimeEnd)){
            queryWrapper.lt("create_time",createTimeEnd);
        }
        //调用mapper
        var userInfoPage = baseMapper.selectPage(pageParam, queryWrapper);
        //编号变成对应的值
        userInfoPage.getRecords().stream().forEach(item->{
            this.packageUserInfo(item);
        });
        return userInfoPage;
    }

    @Override
    public void lock(Long userId, Integer status) {
        if(status.intValue()==0||status.intValue()==1){
            var userInfo = baseMapper.selectById(userId);
            userInfo.setStatus(status);
            baseMapper.updateById(userInfo);
        }
    }

    @Override
    public Map<String, Object> show(Long userId) {
        Map<String,Object> map=new HashMap<>();
        //根据userid查询用户信息
        var userInfo = baseMapper.selectById(userId);
        userInfo=this.packageUserInfo(userInfo);
        map.put("userInfo",userInfo);
        //根据userid查询就诊人信息
        var patientList = patientService.findAllUserId(userId);
        map.put("patientList",patientList);
        return map;
    }

    @Override
    public void approval(Long userId, Integer authStatus) {
        if(authStatus.intValue()==2||authStatus.intValue()==-1){
            var userInfo = baseMapper.selectById(userId);
            userInfo.setAuthStatus(authStatus);
            baseMapper.updateById(userInfo);
        }
    }

    private UserInfo packageUserInfo(UserInfo userInfo) {
        //处理认证状态的编码
        userInfo.getParam().put("authStatusString",AuthStatusEnum.getStatusNameByStatus(userInfo.getAuthStatus()));
        //处理用户状态 0 1
        String statusString=userInfo.getStatus().intValue()==0?"锁定":"正常";
        userInfo.getParam().put("statusString",statusString);
        return userInfo;
    }
}
