package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.LateReturnReportChartDTO;
import com.tianhai.warn.enums.CollegeEnum;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.CalculationResult;
import com.tianhai.warn.service.LateReturnService;
import com.tianhai.warn.service.ReportService;
import com.tianhai.warn.service.WarningRuleService;
import com.tianhai.warn.utils.DateUtils;
import com.tianhai.warn.utils.RedisLockUtils;
import com.tianhai.warn.utils.Result;
import com.tianhai.warn.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/reports")
public class ReportsController {
    private static final Logger logger = LoggerFactory.getLogger(ReportsController.class);

    @Autowired
    private WarningRuleService warningRuleService;

    @Autowired
    private ReportService reportService;

    /**
     * 页面跳转
     * 
     * @return 跳转页面
     */
    @GetMapping
    public String reports() {
        return "reports";
    }

    /**
     * 获取报告卡片数据
     * 如果最近天数和时间段都不为空，默认选择最近天数作为时间条件
     *
     * @param reportChartDTO        晚归图标查询DTO
     * @return 卡片数据
     */
    @ResponseBody
    @GetMapping("/cardData")
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("获取报告卡片数据")
    public Result<ReportCardStatVO> getReportCardDataExcludeHighRisk(LateReturnReportChartDTO reportChartDTO) {
        // 校验参数
        validateCollegeAndDorm(reportChartDTO.getCollege(), reportChartDTO.getDormitoryBuilding());

        // 获取含有当天起始和截止的时分秒时间
        Map<String, Date> timeRange = DateUtils.resolveSingleDayRange(
                reportChartDTO.getStartDate(), reportChartDTO.getEndDate());

        ReportCardStatVO reportCardStatVO = reportService.statsReportCardDataExcludeHighRisk(
                timeRange.get(DateUtils.START_TIME),
                timeRange.get(DateUtils.END_TIME),
                reportChartDTO.getCollege(),
                reportChartDTO.getDormitoryBuilding());

        return Result.success(reportCardStatVO);
    }

    /**
     * 获取高危预警人数计算结果
     * 高危预警是指在选择的时间范围内，在warning_rule表中违约的规则数，等于或大于system_rule表中的阈值
     * eg. 在一定时间范围内的滑动时间窗口中，学生在warning_rule表违反了超过3条规则，而system_rule规定的高危预警阈值是2 那就触发
     * todo 此处前端时间形式需要适配 以及处理学院 宿舍参数
     *
     * @param reportChartDTO        晚归图标查询DTO
     * @return 计算结果
     */
    @ResponseBody
    @GetMapping("/high-risk-count")
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("获取高危预警人数")
    public Result<CalculationResult> getHighRiskCount(LateReturnReportChartDTO reportChartDTO) {
        // 校验参数
        validateCollegeAndDorm(reportChartDTO.getCollege(), reportChartDTO.getDormitoryBuilding());

        // 获取含有当天起始和截止的时分秒时间
        Map<String, Date> timeRange = DateUtils.resolveSingleDayRange(
                reportChartDTO.getStartDate(), reportChartDTO.getEndDate());

        CalculationResult calculationResult = warningRuleService.calHighRiskStudents(
                timeRange.get(DateUtils.START_TIME), timeRange.get(DateUtils.END_TIME));

        // 根据状态设置相应的消息
        String message = switch (calculationResult.getStatus()) {
            case CALCULATING -> "计算中";
            case COMPLETED -> "计算完成";
            case FAILED -> "计算失败";
        };

        return Result.success(calculationResult, message);
    }

    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态
     */
    @ResponseBody
    @GetMapping("/task-status/{taskId}")
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("获取任务状态")
    public Result<CalculationResult> getTaskStats(@PathVariable String taskId) {
        if (StringUtils.isBlank(taskId)) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        CalculationResult calculationResult = warningRuleService.getTaskStatus(taskId);

        // 根据状态设置相应的信息
        String message = switch (calculationResult.getStatus()) {
            case CALCULATING -> "计算中";
            case COMPLETED -> "计算完成";
            case FAILED -> "计算失败";
        };

        return Result.success(calculationResult, message);
    }


    // todo 待实现
    @ResponseBody
    @GetMapping("/chart/week/warn")
    @RequirePermission(roles = Constants.SYSTEM_USER)
    public Result<List<WeekWarnStatVO>> getWarningChartStatsData(LateReturnReportChartDTO reportChartDTO) {

        return Result.success(new ArrayList<>());
    }

    // 1 按照时间进行筛选
    @ResponseBody
    @GetMapping("/chart/week/late-return")
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("获取统计图表数据-时间维度")
    public Result<List<WeekLateReturnStatVO>> getWeekChartLateReturnStatData(LateReturnReportChartDTO reportChartDTO) {
        // 校验参数
        validateCollegeAndDorm(reportChartDTO.getCollege(), reportChartDTO.getDormitoryBuilding());

        // 获取含有当天起始和截止的时分秒时间
        Map<String, Date> timeRange = DateUtils.resolveSingleDayRange(
                reportChartDTO.getStartDate(), reportChartDTO.getEndDate());

        List<WeekLateReturnStatVO> weekLateReturnStatVOList =
                reportService.calWeekLateReturnStat(
                        timeRange.get(DateUtils.START_TIME),
                        timeRange.get(DateUtils.END_TIME),
                        reportChartDTO.getCollege(),
                        reportChartDTO.getDormitoryBuilding());

        return Result.success(weekLateReturnStatVOList);
    }

    // 2 按照学院进行筛选
    // CollegeLateReturnStatVO的percentage字段已经保留两位小数，并且是类似40.20的形式，而不是0.42
    @ResponseBody
    @GetMapping("/chart/college")
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("获取统计图表数据-学院维度")
    public Result<List<CollegeLateReturnStatVO>> getCollegeChartLateReturnStatData(LateReturnReportChartDTO reportChartDTO) {
        // 校验参数
        validateCollegeAndDorm(reportChartDTO.getCollege(), reportChartDTO.getDormitoryBuilding());

        // 获取含有当天起始和截止的时分秒时间
        Map<String, Date> timeRange = DateUtils.resolveSingleDayRange(
                reportChartDTO.getStartDate(), reportChartDTO.getEndDate());

        List<CollegeLateReturnStatVO> collegeLateReturnStatVOList =
                reportService.calCollegeLateReturnStat(
                        timeRange.get(DateUtils.START_TIME),
                        timeRange.get(DateUtils.END_TIME),
                        reportChartDTO.getCollege(),
                        reportChartDTO.getDormitoryBuilding());

        return Result.success(collegeLateReturnStatVOList);
    }

    // 3 按照晚归时间段进行筛选
    @ResponseBody
    @GetMapping("/chart/time")
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("获取统计图表数据-晚归时间段维度")
    public Result<List<TimeRangeLateReturnStatVO>> getTimeChartLateReturnStatData(LateReturnReportChartDTO reportChartDTO) {
        // 校验参数
        validateCollegeAndDorm(reportChartDTO.getCollege(), reportChartDTO.getDormitoryBuilding());

        // 获取含有当天起始和截止的时分秒时间
        Map<String, Date> timeRange = DateUtils.resolveSingleDayRange(
                reportChartDTO.getStartDate(), reportChartDTO.getEndDate());

        List<TimeRangeLateReturnStatVO> timeRangeLateReturnStatVOList =
                reportService.calTimeLateReturnStat(
                        timeRange.get(DateUtils.START_TIME),
                        timeRange.get(DateUtils.END_TIME),
                        reportChartDTO.getCollege(),
                        reportChartDTO.getDormitoryBuilding());

        return Result.success(timeRangeLateReturnStatVOList);
    }

    // 4 按照宿舍楼进行晚归统计 (这里分为两个维度 一个是宿舍，一个是宿舍楼栋）
    @ResponseBody
    @GetMapping("/chart/dormitory")
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("获取统计图表数据-宿舍维度")
    public Result<Map<String, List<DormitoryLateReturnStatVO>>> getDormitoryChartLateReturnStatData(
            LateReturnReportChartDTO reportChartDTO) {
        // 校验参数
        validateCollegeAndDorm(reportChartDTO.getCollege(), reportChartDTO.getDormitoryBuilding());

        // 获取含有当天起始和截止的时分秒时间
        Map<String, Date> timeRange = DateUtils.resolveSingleDayRange(
                reportChartDTO.getStartDate(), reportChartDTO.getEndDate());

        Map<String, List<DormitoryLateReturnStatVO>> dormitoryLateReturnStatMap =
                reportService.calDormitoryLateReturnStat(
                        timeRange.get(DateUtils.START_TIME),
                        timeRange.get(DateUtils.END_TIME),
                        reportChartDTO.getCollege(),
                        reportChartDTO.getDormitoryBuilding());

        return Result.success(dormitoryLateReturnStatMap);
    }


    /**
     * 校验参数合法性
     * @param college               学院 ALL代表全部学院
     * @param dormitoryBuilding     宿舍楼栋 ALL代表全部宿舍楼
     */
    private void validateCollegeAndDorm(String college, String dormitoryBuilding) {

        if (!college.equalsIgnoreCase(Constants.ALL) && CollegeEnum.getCodeByName(college) == null) {
            logger.error("学院参数不合法:{}", college);
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        if (StringUtils.isBlank(dormitoryBuilding)) {
            logger.error("宿舍楼参数为空");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        if (!dormitoryBuilding.equalsIgnoreCase(Constants.ALL) && dormitoryBuilding.matches("^[A-Za-z]栋$")) {
            logger.error("宿舍楼参数不合法:{}", dormitoryBuilding);
        }
    }

}
