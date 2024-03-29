package com.atguigu.yygh.hosp.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.yygh.common.exception.YyghException;
import com.atguigu.yygh.common.result.ResultCodeEnum;
import com.atguigu.yygh.hosp.mapper.ScheduleMapper;
import com.atguigu.yygh.hosp.repository.ScheduleRepository;
import com.atguigu.yygh.hosp.service.DepartmentService;
import com.atguigu.yygh.hosp.service.HospitalService;
import com.atguigu.yygh.hosp.service.ScheduleService;
import com.atguigu.yygh.model.hosp.BookingRule;
import com.atguigu.yygh.model.hosp.Department;
import com.atguigu.yygh.model.hosp.Hospital;
import com.atguigu.yygh.model.hosp.Schedule;
import com.atguigu.yygh.vo.hosp.BookingScheduleRuleVo;
import com.atguigu.yygh.vo.hosp.ScheduleOrderVo;
import com.atguigu.yygh.vo.hosp.ScheduleQueryVo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl extends ServiceImpl<ScheduleMapper,Schedule> implements ScheduleService {
    @Autowired
    private ScheduleRepository scheduleRepository;

     @Autowired
     private MongoTemplate mongoTemplate;

     @Autowired
     private HospitalService hospitalService;

     @Autowired
     private DepartmentService departmentService;
    //上传排班
    @Override
    public void save(Map<String, Object> paramMap) {
        //把map转化成department对象
        var jsonString = JSONObject.toJSONString(paramMap);
        Schedule schedule= JSONObject.parseObject(jsonString, Schedule.class);
        Schedule scheduleExist= scheduleRepository
                .getScheduleByHoscodeAndHosScheduleId(schedule.getHoscode(),schedule.getHosScheduleId());
        if(scheduleExist!=null){
            scheduleExist.setUpdateTime(new Date());
            scheduleExist.setStatus(1);
            scheduleRepository.save(scheduleExist);
        }else{
            schedule.setCreateTime(new Date());
            schedule.setUpdateTime(new Date());
            schedule.setIsDeleted(0);
            schedule.setStatus(1);
            scheduleRepository.save(schedule);
        }
    }

    @Override
    public Page<Schedule> findPageSchedule(int page, int limit, ScheduleQueryVo scheduleQueryVo) {
        //创建pageable对象，设置当前页和每页记录数
        Pageable pageable= PageRequest.of(page-1,limit);
        //创建example对象
        Schedule schedule=new Schedule();
        BeanUtils.copyProperties(scheduleQueryVo,schedule);
        schedule.setIsDeleted(0);
        schedule.setStatus(1);
        ExampleMatcher matcher=ExampleMatcher.matching().withStringMatcher(ExampleMatcher
                .StringMatcher.CONTAINING).withIgnoreCase(true);
        Example<Schedule> example=Example.of(schedule,matcher);
        var all = scheduleRepository.findAll(example, pageable);
        return all;
    }

    @Override
    public void remove(String hoscode, String hosScheduleId) {
        //根据医院编号和排班编号查询信息
        var sc = scheduleRepository.getScheduleByHoscodeAndHosScheduleId(hoscode, hosScheduleId);
        if(sc!=null){
            scheduleRepository.deleteById(sc.getId());
        }
    }

    @Override
    public Map<String, Object> getRuleSchedule(Integer page, Integer limit, String hoscode, String depcode) {
        //根据医院和科室编号查询
        Criteria criteria=Criteria.where("hoscode").is(hoscode).and("depcode").is(depcode);

        //根据工作日期分组
        Aggregation agg=Aggregation.newAggregation(
                Aggregation.match(criteria),//匹配条件
                Aggregation.group("workDate")//分组字段
                        .first("workDate").as("workDate")
                        //统计号员的数量
                        .count().as("docCount")
                        .sum("reservedNumber").as("reservedNumber")
                        .sum("availableNumber").as("availableNumber"),
                //排序
                Aggregation.sort(Sort.Direction.ASC,"workDate"),
                //实现分页
                Aggregation.skip((page-1)*limit),
                Aggregation.limit(limit)
        );
        //调用方法最终执行
        var aggResults = mongoTemplate.
                aggregate(agg, Schedule.class, BookingScheduleRuleVo.class);
        var bookingScheduleRuleVoList = aggResults.getMappedResults();
        //分组查询的总记录数
        Aggregation totalAgg=Aggregation.newAggregation(
          Aggregation.match(criteria),
           Aggregation.group("workDate")

        );
        var totalAggResults = mongoTemplate.
                aggregate(totalAgg, Schedule.class, BookingScheduleRuleVo.class);
        var total = totalAggResults.getMappedResults().size();
        //把日期转换成星期
        for(BookingScheduleRuleVo bookingScheduleRuleVo:bookingScheduleRuleVoList){
            var workDate = bookingScheduleRuleVo.getWorkDate();
            var week = this.getDayOfWeek(new DateTime(workDate));
            bookingScheduleRuleVo.setDayOfWeek(week);
        }
        //设置最终数据返回
        Map<String,Object> result=new HashMap<>();
        result.put("bookingScheduleRuleVoList",bookingScheduleRuleVoList);
        result.put("total",total);
        //获取医院名称
        String hosName= hospitalService.getHospName(hoscode);
        //其他基础数据
        Map<String,String> baseMap=new HashMap<>();
        baseMap.put("hosname",hosName);
        result.put("baseMap",baseMap);
        return result;
    }

    @Override
    public List<Schedule> getDetailSchedule(String hoscode, String depcode, String workDate) {
        //根据参数查询mongodb
        List<Schedule> scheduleList= scheduleRepository.findScheduleByHoscodeAndDepcodeAndWorkDate(hoscode,depcode
                 ,new DateTime(workDate).toDate());
        //把得到的list集合遍历，向其中设置其他值
        scheduleList.stream().forEach(item->{
            this.packageSchedule(item);
        });
       return scheduleList;
    }

    @Override
    public Map<String,Object> getBookingScheduleRule(Integer page, Integer limit, String hoscode, String depcode) {
       //获取可预约的排班数据
        Map<String,Object> result=new HashMap<>();
        //获取预约规则
        //1.根据医院编号获取医院预约规则
        var hospital = hospitalService.getByHoscode(hoscode);
        if(hospital==null) throw new YyghException(ResultCodeEnum.DATA_ERROR);
        var bookingRule = hospital.getBookingRule();
        //获取可预约日期的数据(分页)
        IPage iPage= this.getListDate(page,limit,bookingRule);
        //获取当前可预约的日期
        List<Date> dateList= iPage.getRecords();
        //获取可预约日期里面科室的剩余预约数
        Criteria criteria=Criteria.where("hoscode").is(hoscode).and("depcode").is(depcode)
                .and("workDate").in(dateList);
        Aggregation agg=Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("workDate")
                        .first("workDate").as("workDate")
                        .count().as("docCount")
                        .sum("availableNumber").as("availableNumber")
                        .sum("reservedNumber").as("reservedNumber")

        );
        var aggregate = mongoTemplate.aggregate(agg, Schedule.class, BookingScheduleRuleVo.class);
        var scheduleRuleVoList = aggregate.getMappedResults();
        //合并数据 map集合 key日期 value预约规则和剩余数量等
        Map<Date,BookingScheduleRuleVo> scheduleRuleVoMap=new HashMap<>();//根据日期查数据方便，所以转成map
        if(!CollectionUtils.isEmpty(scheduleRuleVoList)){
            scheduleRuleVoMap=scheduleRuleVoList.stream().collect(
                    Collectors.toMap(BookingScheduleRuleVo::getWorkDate
                            ,BookingScheduleRuleVo->BookingScheduleRuleVo)
            );
        }
        //获取可预约排班规则
        List<BookingScheduleRuleVo> bookingScheduleRuleVoList=new ArrayList<>();
        for(int i=0,len=dateList.size();i<len;i++){
            Date date=dateList.get(i);
            //从map集合根据key日期获取value值
            var bookingScheduleRuleVo = scheduleRuleVoMap.get(date);
            //如果当天没有排班医生
            if(bookingScheduleRuleVo==null){
                bookingScheduleRuleVo=new BookingScheduleRuleVo();
                bookingScheduleRuleVo.setDocCount(0);
                bookingScheduleRuleVo.setAvailableNumber(-1);
            }
            bookingScheduleRuleVo.setWorkDate(date);
            bookingScheduleRuleVo.setWorkDateMd(date);
            //计算当前预约日期
            var dayOfWeek = this.getDayOfWeek(new DateTime(date));
            bookingScheduleRuleVo.setDayOfWeek(dayOfWeek);
            //最后一页最后一条记录为即将预约 状态 0：正常  1；即将放号 -1：当天已停止挂号
            if(i==len-1 && page==iPage.getPages()){
                bookingScheduleRuleVo.setStatus(1);
            }else{
                bookingScheduleRuleVo.setStatus(0);
            }
            //如果过了挺好时间，不能预约
            if(i==0 && page==1){
                DateTime stopTime=this.getDateTime(new Date(),bookingRule.getStopTime());
                if(stopTime.isBeforeNow()){
                    //停止预约
                    bookingScheduleRuleVo.setStatus(-1);
                }
            }
            bookingScheduleRuleVoList.add(bookingScheduleRuleVo);
        }
        //可预约日期规则数据
        result.put("bookingScheduleList", bookingScheduleRuleVoList);
        result.put("total", iPage.getTotal());
        //其他基础数据
        Map<String, String> baseMap = new HashMap<>();
        //医院名称
        baseMap.put("hosname", hospitalService.getHospName(hoscode));
        //科室
        Department department =departmentService.getDepartment(hoscode, depcode);
        //大科室名称
        baseMap.put("bigname", department.getBigname());
        //科室名称
        baseMap.put("depname", department.getDepname());
//月
        baseMap.put("workDateString", new DateTime().toString("yyyy年MM月"));
//放号时间
        baseMap.put("releaseTime", bookingRule.getReleaseTime());
//停号时间
        baseMap.put("stopTime", bookingRule.getStopTime());
        result.put("baseMap", baseMap);

        return result;
    }

    @Override
    public Schedule getScheduleId(String scheduleId) {
        var schedule = scheduleRepository.findById(scheduleId).get();
        this.packageSchedule(schedule);
        return schedule;
    }

    @Override
    public ScheduleOrderVo getScheduleOrderVo(String scheduleId) {
        var scheduleOrderVo = new ScheduleOrderVo();
        //获取排班信息
        Schedule schedule = scheduleRepository.getScheduleById(scheduleId);
        if(schedule==null){
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }
        //获取预约规则信息
        var hospital = hospitalService.getByHoscode(schedule.getHoscode());
        if(hospital==null){
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }
        var bookingRule = hospital.getBookingRule();
        if(bookingRule==null){
            throw new YyghException(ResultCodeEnum.PARAM_ERROR);
        }
        //获取的数据设置到scheduleOrdervo
        scheduleOrderVo.setHoscode(schedule.getHoscode());
        scheduleOrderVo.setHosname(hospitalService.getHospName(schedule.getHoscode()));
        scheduleOrderVo.setDepcode(schedule.getDepcode());
        scheduleOrderVo.setDepname(departmentService.getDepName(schedule.getHoscode(), schedule.getDepcode()));
        scheduleOrderVo.setHosScheduleId(schedule.getHosScheduleId());
        scheduleOrderVo.setAvailableNumber(schedule.getAvailableNumber());
        scheduleOrderVo.setTitle(schedule.getTitle());
        scheduleOrderVo.setReserveDate(schedule.getWorkDate());
        scheduleOrderVo.setReserveTime(schedule.getWorkTime());
        scheduleOrderVo.setAmount(schedule.getAmount());
        //退号截止天数（如：就诊前一天为-1，当天为0）
        int quitDay = bookingRule.getQuitDay();
        DateTime quitTime = this.getDateTime(new DateTime(schedule.getWorkDate()).plusDays(quitDay).toDate(), bookingRule.getQuitTime());
        scheduleOrderVo.setQuitTime(quitTime.toDate());

        //预约开始时间
        DateTime startTime = this.getDateTime(new Date(), bookingRule.getReleaseTime());
        scheduleOrderVo.setStartTime(startTime.toDate());

        //预约截止时间
        DateTime endTime = this.getDateTime(new DateTime().plusDays(bookingRule.getCycle()).toDate(), bookingRule.getStopTime());
        scheduleOrderVo.setEndTime(endTime.toDate());

        //当天停止挂号时间
        DateTime stopTime = this.getDateTime(new Date(), bookingRule.getStopTime());
        scheduleOrderVo.setStopTime(stopTime.toDate());
    return  scheduleOrderVo;
    }
  //更新排班信息，用于rabbitmq
    @Override
    public void update(Schedule schedule) {
        schedule.setUpdateTime(new Date());
        scheduleRepository.save(schedule);
    }

    /**
     * 将Date日期（yyyy-MM-dd HH:mm）转换为DateTime
     */
    private DateTime getDateTime(Date date, String timeString) {
        String dateTimeString = new DateTime(date).toString("yyyy-MM-dd") + " "+ timeString;
        DateTime dateTime = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm").parseDateTime(dateTimeString);
        return dateTime;
    }

    //获取可预约数据分页数据
    private IPage getListDate(Integer page, Integer limit, BookingRule bookingRule) {
        //获取当天放号时间， 年 月 日 小时 分钟
        var releaseTime = this.getDateTime(new Date(), bookingRule.getReleaseTime());
        //获取预约周期
        var cycle = bookingRule.getCycle();
        //如果当天放号时间已经过去了，预约周期从后一天开始计算，周期加一
        if(releaseTime.isBeforeNow()) cycle+=1;
        //获取可预约所有日期，最后一天显示即将放号
        List<Date> dateList=new ArrayList<>();
        for(int i=0;i<cycle;i++){
            var curDateTime = new DateTime().plusDays(i);
            var dateString = curDateTime.toString("yyyy-MM-dd");
            dateList.add(new DateTime(dateString).toDate());
        }
        //因为预约周期不同，每页显示日期最多七天数据，超过七天分页
        List<Date> pageDateList=new ArrayList<>();
        int start=(page-1)*limit;
        int end=(page-1)*limit+limit;
        //如果可以显示的数据小于7，
         if(end>dateList.size()){
             end=dateList.size();
         }
         for(int i=start;i<end;i++){
             pageDateList.add(dateList.get(i));
         }
        // 直接显示。如果大于7，进行分页
        IPage<Date> iPage=new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page,7,dateList.size());
        iPage.setRecords(pageDateList);
        return iPage;
    }

    //封装排班详情其他值 医院名称，科室名称，日期对应星期
    private void packageSchedule(Schedule item) {
        //设置医院名称
        item.getParam().put("hosname",hospitalService.getHospName(item.getHoscode()));
        //设置科室名称
        item.getParam().put("depname",departmentService.getDepName(item.getHoscode(),item.getDepcode()));
        //设置日期对应的星期
        item.getParam().put("dayOfWeek",this.getDayOfWeek(new DateTime(item.getWorkDate())));
    }

    /**
     * 根据日期获取周几数据
     * @param dateTime
     * @return
     */
    private String getDayOfWeek(DateTime dateTime) {
        String dayOfWeek = "";
        switch (dateTime.getDayOfWeek()) {
            case DateTimeConstants.SUNDAY:
                dayOfWeek = "周日";
                break;
            case DateTimeConstants.MONDAY:
                dayOfWeek = "周一";
                break;
            case DateTimeConstants.TUESDAY:
                dayOfWeek = "周二";
                break;
            case DateTimeConstants.WEDNESDAY:
                dayOfWeek = "周三";
                break;
            case DateTimeConstants.THURSDAY:
                dayOfWeek = "周四";
                break;
            case DateTimeConstants.FRIDAY:
                dayOfWeek = "周五";
                break;
            case DateTimeConstants.SATURDAY:
                dayOfWeek = "周六";
            default:
                break;
        }
        return dayOfWeek;
    }

    @Override
    public boolean save(Schedule entity) {
        return super.save(entity);
    }
}
