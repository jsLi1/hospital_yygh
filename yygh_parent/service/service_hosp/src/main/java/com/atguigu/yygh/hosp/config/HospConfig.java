package com.atguigu.yygh.hosp.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.CrossOrigin;

@Configuration
@MapperScan("com.atguigu.yygh.hosp.mapper")
public class HospConfig {
 @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
     MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
     mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
     return mybatisPlusInterceptor;
 }
}
