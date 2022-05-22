package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.hosp.repository.DepartmentRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.vo.hosp.DepartmentQueryVo;
import com.atguigu.yygh.vo.hosp.DepartmentVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DepartmentServiceImpl implements DepartmentService {
    @Autowired
    private DepartmentRepository departmentRepository;

    @Override
    public void save(Map<String, Object> paramMap) {
        //吧map转化成department对象
        var jsonString = JSONObject.toJSONString(paramMap);
        Department department= JSONObject.parseObject(jsonString,Department.class);
       Department departmentExist= departmentRepository
               .getDepartmentByHoscodeAndDepcode(department.getHoscode(),department.getDepcode());
       if(departmentExist!=null){
           departmentExist.setUpdateTime(new Date());
           departmentExist.setIsDeleted(0);
           departmentRepository.save(departmentExist);
       }else{
           department.setCreateTime(new Date());
           department.setUpdateTime(new Date());
           department.setIsDeleted(0

           );
           departmentRepository.save(department);
       }
    }

    @Override
    public Page<Department> findPageDepartment(int page, int limit, DepartmentQueryVo departmentQueryVo) {
         //创建pageable对象，设置当前页和每页记录数
        Pageable pageable= PageRequest.of(page-1,limit);
        //创建example对象
        Department department=new Department();
        BeanUtils.copyProperties(departmentQueryVo,department);
        department.setIsDeleted(0);
        ExampleMatcher matcher=ExampleMatcher.matching().withStringMatcher(ExampleMatcher
                .StringMatcher.CONTAINING).withIgnoreCase(true);
        Example<Department> example=Example.of(department,matcher);
        var all = departmentRepository.findAll(example, pageable);
        return all;
    }

     //查询医院所有的科室
    @Override
    public List<DepartmentVo> findDeptTree(String hoscode) {
        //创建list集合，用于最终数据封装
        List<DepartmentVo> result=new ArrayList<>();

        //根据医院编号查询所有科室信息
        Department departmentQuery=new Department();
        departmentQuery.setHoscode(hoscode);
        var example = Example.of(departmentQuery);
        var departmentList = departmentRepository.findAll(example);
        //根据大科室编号进行分组 bigcode ，获取没饿过大科室下级子科室
        var departmentMap = departmentList.stream().collect(Collectors.groupingBy(Department::getBigcode));
        //遍历map集合
        for(Map.Entry<String,List<Department>> entry:departmentMap.entrySet()){
            var key = entry.getKey();//大科室编号
            var value = entry.getValue();//大科室集合
            //封装大科室
            DepartmentVo departmentVo=new DepartmentVo();
            departmentVo.setDepcode(key);
            departmentVo.setDepname(value.get(0).getBigname());
            //封装小科室
            List<DepartmentVo> child=new ArrayList<>();
            for(Department department:departmentList){
                DepartmentVo departmentVo1=new DepartmentVo();
                departmentVo1.setDepcode(department.getDepcode());
                departmentVo1.setDepname(department.getDepname());
                child.add(departmentVo1);
            }
            //把小科室list集合放到大科室children里面
            departmentVo.setChildren(child);
            //放到result里面
            result.add(departmentVo);
        }

        return result;
    }

    @Override
    public String getDepName(String hoscode, String depcode) {
        var department = departmentRepository.getDepartmentByHoscodeAndDepcode(hoscode, depcode);
        if(department!=null){
            return department.getDepname();
        }
        return null;
    }

    @Override
    public Department getDepartment(String hoscode, String depcode) {
        return departmentRepository.getDepartmentByHoscodeAndDepcode(hoscode,depcode);
    }

    @Override
    public void remove(String hoscode, String depcode) {
        //删除科室
        var departmentByHoscodeAndDepcode = departmentRepository.getDepartmentByHoscodeAndDepcode(hoscode, depcode);
        if(departmentByHoscodeAndDepcode!=null){
            departmentRepository.deleteById(departmentByHoscodeAndDepcode.getId());
        }
    }
}
