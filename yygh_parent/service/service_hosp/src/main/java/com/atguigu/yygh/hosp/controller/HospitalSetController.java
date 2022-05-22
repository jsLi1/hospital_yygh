package com.atguigu.yygh.hosp.controller;


import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.common.utils.MD5;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.model.hosp.HospitalSet;
import com.atguigu.yygh.vo.hosp.HospitalSetQueryVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Random;


@Api(tags = "医院设置管理")
@RestController
@RequestMapping("/admin/hosp/hospitalSet")
public class HospitalSetController {

    //注入service
    @Autowired
    private HospitalSetService hospitalSetService;

    // http://localhost:8201/admin/hosp/hospitalSet/findAll

    //1 查询医院设置表所有信息
    @ApiOperation(value = "获取所有医院设置")
    @GetMapping("findAll")
    public Result findAllHospitalSet() {
        //调用service的方法
        List<HospitalSet> list = hospitalSetService.list();
        var ok = Result.ok(list);
        return ok;
    }

     //2.删除医院设置
    @DeleteMapping("{id}")
    @ApiOperation(value = "删除指定医院设置")
    public Result removeHopSet(@PathVariable Long id){
        var flag = hospitalSetService.removeById(id);
         if(flag==true){
             return Result.ok();
         }else{
             return Result.fail();
         }
    }
  //3.条件查询带分页
   @PostMapping("findPage/{current}/{limit}")
   public Result  findPageHospSet(@PathVariable long current, @PathVariable long limit,@RequestBody(required = false) HospitalSetQueryVo hospitalSetQueryVo){
        //创建page对象，传递当前页，每页记录数
        Page<HospitalSet> page=new Page<>(current,limit);
        //调用方法实现分页查询
       QueryWrapper<HospitalSet> wrapper=new QueryWrapper<>();
       String hosname= hospitalSetQueryVo.getHosname();
       String hoscode=hospitalSetQueryVo.getHoscode();
       if(!StringUtils.isEmpty(hosname)){
           wrapper.like("hosname",hosname);
       }
       if(!StringUtils.isEmpty(hoscode)){
           wrapper.eq("hoscode",hospitalSetQueryVo.getHoscode());
       }
       var hospitalSetPage = hospitalSetService.page(page, wrapper);
      return Result.ok(hospitalSetPage);

   }

  //4.添加医院设置
  @PostMapping("saveHospitalSet")
    public  Result saveHospital(@RequestBody HospitalSet hospitalSet){
        //设置状态 1使用 0不能使用
      hospitalSet.setStatus(1);
      //签名密钥
      Random random=new Random();
      String encrypt = MD5.encrypt(System.currentTimeMillis() + "" + random.nextInt(1000));
      hospitalSet.setSignKey(encrypt);//签名密钥
      var save = hospitalSetService.save(hospitalSet);
      if(save){
          return Result.ok();
      }else {
         return Result.fail();
      }
  }


  //5.根据id获取医院设置
  @GetMapping("getHospSet/{id}")
    public  Result getHospSet(@PathVariable Long id){
      var byId = hospitalSetService.getById(id);
      return  Result.ok(byId);
  }


  //6.修改医院设置
   @PostMapping("updateHospitalSet")
    public Result updateHospitalSet(@RequestBody HospitalSet hospitalSet){
       var flag = hospitalSetService.updateById(hospitalSet);
       if(flag){
           return Result.ok();
       }else {
          return   Result.fail();
       }

   }

  //7.批量删除医院设置
  @DeleteMapping("batchRemove")
    public  Result batchRemove(@RequestBody List<Long> idList){
      var flag = hospitalSetService.removeByIds(idList);
      return Result.ok();
  }
 //8.医院设置锁定和解锁
  @PutMapping("lockHospitalSet/{id}/{status}")
  public  Result lockHospitalSet(@PathVariable Long id,@PathVariable Integer status){
        //根据id查询医院设置信息
      var hospitalSet = hospitalSetService.getById(id);
      //设置状态
      hospitalSet.setStatus(status);
      //调用方法
      hospitalSetService.updateById(hospitalSet);
      return Result.ok();

  }
 //9.发送签名密钥
    @PutMapping("sendKey/{id}")
    public Result sendKey(@PathVariable Long id){
        var hospitalSet = hospitalSetService.getById(id);
        var signKey = hospitalSet.getSignKey();
        var hoscode = hospitalSet.getHoscode();
        //发送短信
        return Result.ok();
    }

}
