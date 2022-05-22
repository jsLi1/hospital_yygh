package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.cmn.client.DictFeignClient;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.ResultCodeEnum;
import com.atguigu.yygh.hosp.repository.HospitalRepository;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.HospitalSet;
import com.atguigu.yygh.vo.hosp.HospitalQueryVo;
import com.atguigu.yygh.vo.order.SignInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HospitalServiceImpl implements HospitalService {

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private DictFeignClient dictFeignClient;

    @Override
    public void save(Map<String, Object> paramMap) {
        //把参数map集合转换对象
        var jsonString = JSONObject.toJSONString(paramMap);
        var hospital = JSONObject.parseObject(jsonString, Hospital.class);
        //判断是否存在数据
        var hoscode = hospital.getHoscode();
       Hospital hospitalExist= hospitalRepository.getHospitalByHoscode(hoscode);
        //如果存在，进行修改
       if(hospitalExist!=null){
         hospitalExist.setStatus(0);
           hospitalExist.setCreateTime(hospital.getCreateTime());
           hospitalExist.setUpdateTime(new Date());
           hospitalExist.setIsDeleted(0);
         hospitalRepository.save(hospitalExist);
       }else{
           //如果不存在，进行添加
           hospital.setStatus(0);
           hospital.setCreateTime(new Date());
           hospital.setUpdateTime(new Date());
           hospital.setIsDeleted(0);
           hospitalRepository.save(hospital);
       }

    }

    @Override
    public Hospital getByHoscode(String hoscode) {
        var hospital = hospitalRepository.getHospitalByHoscode(hoscode);
        return hospital;
    }
   //更新医院状态
    @Override
    public void updateStatus(String id, Integer status) {
        //先查询医院的信息
        var hospital = hospitalRepository.findById(id).get();
        //设置修改的值
        hospital.setStatus(status);
        hospital.setUpdateTime(new Date());
        hospitalRepository.save(hospital);
    }

    @Override
    public Map<String, Object> getHospById(String id) {
        var hospital = hospitalRepository.findById(id).get();
        var hospital1 = this.setHospitalHosType(hospital);
        Map<String,Object> result=new HashMap<>();
        result.put("hospital",hospital1);
        result.put("bookingRule",hospital1.getBookingRule());
        hospital1.setBookingRule(null);
        return result;
    }
   //获取医院名称
    @Override
    public String getHospName(String hoscode) {
        var hospital = hospitalRepository.getHospitalByHoscode(hoscode);
        if(hospital!=null){
            return hospital.getHosname();
        }
        return null;
    }
  //根据医院名称模糊查询
    @Override
    public List<Hospital> findByHosname(String hosname) {
        return hospitalRepository.findHospitalByHosnameLike(hosname);
    }
  //根据医院编号获取医院预约挂号详情
    @Override
    public Map<String, Object> item(String hoscode) {
        Map<String, Object> result = new HashMap<>();
        //医院详情
        Hospital hospital = this.setHospitalHosType(this.getByHoscode(hoscode));
        result.put("hospital", hospital);
        //预约规则
        result.put("bookingRule", hospital.getBookingRule());
        //不需要重复返回
        hospital.setBookingRule(null);
        return result;

    }


    @Override
    public Page<Hospital> selectHospPage(Integer page, Integer limit, HospitalQueryVo hospitalQueryVo) {
        Pageable pageable= PageRequest.of(page-1,limit);
        ExampleMatcher matcher=ExampleMatcher.matching()
                .withStringMatcher(ExampleMatcher.StringMatcher.CONTAINING)
                .withIgnoreCase(true);
        //hospitalQueryVo转换成hospital
        Hospital hospital=new Hospital();
        BeanUtils.copyProperties(hospitalQueryVo,hospital);
        Example<Hospital> example=Example.of(hospital,matcher);
        //调用方法实现查询
        var pages = hospitalRepository.findAll(example, pageable);
        //获取查询list集合，遍历进行医院等级封装
        pages.getContent().stream().forEach(item->{
            this.setHospitalHosType(item);
        });

        return pages;
    }
    //获取查询list集合，遍历进行医院等级封装
    private Hospital setHospitalHosType(Hospital hospital) {
        //根据dictcode和value获取医院等级名称
        var hostypeString = dictFeignClient.getName("Hostype", hospital.getHostype());
        //查询省市地区
        var provinceString = dictFeignClient.getValue(hospital.getProvinceCode());
        var cityString = dictFeignClient.getValue(hospital.getCityCode());
        var districtString = dictFeignClient.getValue(hospital.getDistrictCode());
        hospital.getParam().put("fullAddress",provinceString+cityString+districtString);
        hospital.getParam().put("hostypeString",hostypeString);
        return hospital;
    }
}
