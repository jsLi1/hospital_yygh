package com.atguigu.yygh.msm.service;

import com.atguigu.yygh.vo.msm.MsmVo;

public interface MsmService {
    boolean send(String phone, String code);

    //mq使用发送短信的接口
    boolean send(MsmVo msmVo);
}
