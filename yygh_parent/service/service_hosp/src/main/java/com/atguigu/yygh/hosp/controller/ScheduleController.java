package com.atguigu.yygh.hosp.controller;

import com.atguigu.yygh.common.result.Result;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.Schedule;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/hosp/schedule")
public class ScheduleController {
    @Autowired
    private ScheduleService scheduleService;

    //根据医院编号查询排班规则数据
    @ApiOperation(value = "查询排班规则数据")
    @GetMapping("getScheduleRule/{page}/{limit}/{hoscode}/{depcode}")
    public Result getScheduleRuleo(@PathVariable Integer page,
                                   @PathVariable Integer limit,
                                   @PathVariable String hoscode,
                                   @PathVariable String depcode){
        Map<String,Object> map= scheduleService.getRuleSchedule(page,limit,hoscode,depcode);
        return Result.ok(map);
    }

    //排班详情，根据医院，科室，工作日期
    @ApiOperation(value = "排班详情")
    @GetMapping("getScheduleDetail/{hoscode}/{depcode}/{workDate}")
    public Result getScheduleDetail(@PathVariable String hoscode,@PathVariable String depcode,
                                      @PathVariable String workDate){
       List<Schedule> list= scheduleService.getDetailSchedule(hoscode,depcode,workDate);
       return Result.ok(list);
    }
}
