package com.atguigu.yygh.msm.controller;

import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.msm.service.MsmService;
import com.atguigu.yygh.msm.utils.RandomUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/msm")
public class MsmApiController {
    @Autowired
    private MsmService msmService;
    @Autowired
    private RedisTemplate<String,String> redisTemplate;
    //发送手机验证码码
    @GetMapping("send/{phone}")
    public Result sendCode(@PathVariable String phone){
        //从redis获取验证码 手机号是key  value是验证码
        var code = redisTemplate.opsForValue().get(phone);
        if(!StringUtils.isEmpty(code)){
            return Result.ok();
        }
        //如果没有，通过整合短信服务发送
        code= RandomUtil.getSixBitRandom();
        boolean isSend= msmService.send(phone,code);
        if(isSend==true){
            redisTemplate.opsForValue().set(phone,code,2, TimeUnit.MINUTES);
            return Result.ok();
        }else{
            return Result.fail().message("发送短信失败");
        }
    }
}
