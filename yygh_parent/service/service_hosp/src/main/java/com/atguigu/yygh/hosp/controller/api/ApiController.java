package com.atguigu.yygh.hosp.controller.api;

import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.helper.HttpRequestHelper;
import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.common.result.ResultCodeEnum;
import com.atguigu.yygh.common.utils.MD5;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.HospitalSetService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.hosp.util.Valid;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.DepartmentQueryVo;
import com.atguigu.yygh.vo.hosp.ScheduleQueryVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/hosp")
public class ApiController {
    @Autowired
    private HospitalService hospitalService;
    @Autowired
    private HospitalSetService hospitalSetService;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private ScheduleService scheduleService;

    //查询医院的接口
    @PostMapping("hospital/show")
    public Result getHosp(HttpServletRequest request){
        //获取传递过来的医院信息
        var requestMap = request.getParameterMap();
        var paramMap = HttpRequestHelper.switchMap(requestMap);
        //获取传递过来的医院的编号
        var hoscode = (String) paramMap.get("hoscode");
        //获取医院医院系统传递过来的签名，签名进行MD5加密
        var sign = (String) paramMap.get("sign");
        //2.根据传递过来的医院编号
        String signKey= hospitalSetService.getSignKey(hoscode);
        //3.把数据库进行MD5加密
        String signKeyMd5= MD5.encrypt(signKey);
        //判断签名是否一致
        if(!sign.equals(signKeyMd5)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        //调用service方法实现根据医院编号查询
        Hospital hospital=hospitalService.getByHoscode(hoscode);
        return Result.ok(hospital);
    }




    //上传医院接口
    @PostMapping("saveHospital")
    public Result saveHosp(HttpServletRequest request){
        //获取传递过来的医院信息
        var requestMap = request.getParameterMap();
        var paramMap = HttpRequestHelper.switchMap(requestMap);
        //1.获取医院系统传递过来的签名,进行了MD5加密
        var sign = (String)paramMap.get("sign");
       //2.根据传递过来的医院编号
        String hosCode = (String) paramMap.get("hoscode");
        String signKey= hospitalSetService.getSignKey(hosCode);
        //3.把数据库进行MD5加密
        String signKeyMd5= MD5.encrypt(signKey);
        //判断签名是否一致
        if(!sign.equals(signKeyMd5)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        //传输过程中“+” 转换为了“”，因此我们要转换回来
        String logoData = (String) paramMap.get("logoData");
        logoData=logoData.replaceAll(" ","+");
        paramMap.put("logoData",logoData);
        //调用service的方法
        hospitalService.save(paramMap);
        return Result.ok();


    }
   //上传科室
    @PostMapping("saveDepartment")
    public Result saveDepartment(HttpServletRequest request){
        //获取传递过来的医院信息
        var requestMap = request.getParameterMap();
        var paramMap = HttpRequestHelper.switchMap(requestMap);
        //获取传递过来的医院的编号
        var hoscode = (String) paramMap.get("hoscode");
        //获取医院医院系统传递过来的签名，签名进行MD5加密
        var sign = (String) paramMap.get("sign");
        //2.根据传递过来的医院编号
        String signKey= hospitalSetService.getSignKey(hoscode);
        //3.把数据库进行MD5加密
        String signKeyMd5= MD5.encrypt(signKey);
        //判断签名是否一致
        if(!sign.equals(signKeyMd5)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        departmentService.save(paramMap);
        return Result.ok();
    }

    //查询科室的接口
    @PostMapping("department/list")
    public Result finddepartment(HttpServletRequest request){
        //获取传递过来的医院信息
        var requestMap = request.getParameterMap();
        var paramMap = HttpRequestHelper.switchMap(requestMap);
        //医院编号
        var hoscode = (String) paramMap.get("hoscode");
        //当前页和每页记录数
        int page=StringUtils.isEmpty((String)paramMap.get("page"))?1:Integer.parseInt((String) paramMap.get("page"));
        int limit=StringUtils.isEmpty((String)paramMap.get("limit"))?1:Integer.parseInt((String) paramMap.get("limit"));
        //签名校验
        var sign = (String) paramMap.get("sign");
        //2.根据传递过来的医院编号
        String signKey= hospitalSetService.getSignKey(hoscode);
        //3.把数据库进行MD5加密
        String signKeyMd5= MD5.encrypt(signKey);
        //判断签名是否一致
        if(!sign.equals(signKeyMd5)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        DepartmentQueryVo departmentQueryVo=new DepartmentQueryVo();
        departmentQueryVo.setHoscode(hoscode);
        Page<Department> pageDepartment=departmentService.findPageDepartment(page,limit,departmentQueryVo);
        return Result.ok(pageDepartment);
    }

    //删除科室
    @PostMapping("department/remove")
    public Result removeDepartment(HttpServletRequest request){
        //获取传递过来的医院信息
        var requestMap = request.getParameterMap();
        var paramMap = HttpRequestHelper.switchMap(requestMap);
        //医院编号
        var hoscode = (String) paramMap.get("hoscode");
        //科室编号
        var depcode = (String) paramMap.get("depcode");
        //判断签名是否一致
        if(!Valid.isValid(paramMap,hospitalSetService)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        departmentService.remove(hoscode,depcode);
        return Result.ok();
    }
    //上传排班的接口
    @PostMapping("saveSchedule")
    public Result saveSchedule(HttpServletRequest request){
        //获取传递过来的医院信息
        var requestMap = request.getParameterMap();
        var paramMap = HttpRequestHelper.switchMap(requestMap);
        if(!Valid.isValid(paramMap,hospitalSetService)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        scheduleService.save(paramMap);
        return Result.ok();
    }
    //查询接口
    @PostMapping("schedule/list")
    public Result findSchedule(HttpServletRequest request){
        var requestMap = request.getParameterMap();
        var paramMap = HttpRequestHelper.switchMap(requestMap);
        if(!Valid.isValid(paramMap,hospitalSetService)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        var hoscode = (String) paramMap.get("hoscode");
        var depcode = (String) paramMap.get("depcode");
        //当前页和每页记录数
        int page=StringUtils.isEmpty((String)paramMap.get("page"))?1:Integer.parseInt((String) paramMap.get("page"));
        int limit=StringUtils.isEmpty((String)paramMap.get("limit"))?1:Integer.parseInt((String) paramMap.get("limit"));
        ScheduleQueryVo scheduleQueryVo=new ScheduleQueryVo();
        scheduleQueryVo.setHoscode(hoscode);
        scheduleQueryVo.setDepcode(depcode);
        Page<Schedule> pageDepartment=scheduleService.findPageSchedule(page,limit,scheduleQueryVo);
        return Result.ok(pageDepartment);
    }

    //删除排班
    @PostMapping("schedule/remove")
    public Result remove(HttpServletRequest request){
        var requestMap = request.getParameterMap();
        var paramMap = HttpRequestHelper.switchMap(requestMap);
        if(!Valid.isValid(paramMap,hospitalSetService)){
            throw new YyghException(ResultCodeEnum.SIGN_ERROR);
        }
        var hoscode = (String) paramMap.get("hoscode");
        var hosScheduleId = (String) paramMap.get("hosScheduleId");
        scheduleService.remove(hoscode,hosScheduleId);
        return Result.ok();
    }
}
