package com.atguigu.yygh.hosp.util;

import com.atguigu.yygh.common.utils.MD5;
import com.atguigu.yygh.hosp.service.HospitalSetService;

import java.util.Map;

public class Valid {
        public static boolean isValid(Map<String,Object> paramMap, HospitalSetService hospitalSetService){
            var hoscode = (String) paramMap.get("hoscode");
            //签名校验
            var sign = (String) paramMap.get("sign");
            //2.根据传递过来的医院编号
            String signKey= hospitalSetService.getSignKey(hoscode);
            //3.把数据库进行MD5加密
            String signKeyMd5= MD5.encrypt(signKey);
            return sign.equals(signKeyMd5);
        }

}
