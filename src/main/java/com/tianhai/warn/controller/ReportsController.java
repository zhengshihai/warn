package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.CollegeEnum;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.CalculationResult;
import com.tianhai.warn.model.LateReturn;
import com.tianhai.warn.query.LateReturnQuery;
import com.tianhai.warn.service.LateReturnService;
import com.tianhai.warn.service.ReportService;
import com.tianhai.warn.service.WarningRuleService;
import com.tianhai.warn.utils.DateUtils;
import com.tianhai.warn.utils.Result;
import com.tianhai.warn.vo.ReportVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/reports")
public class ReportsController {
    private static final Logger logger = LoggerFactory.getLogger(ReportsController.class);

    @Autowired
    private LateReturnService lateReturnService;

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
     * @param startDate         起始日期
     * @param endDate           终止日期
     * @param college           学院
     * @param dormitoryBuilding 宿舍楼
     * @return 卡片数据
     */
    @PostMapping("/cardData") // todo 前端的all和全部需要适配 处理率前端要加 %
    @ResponseBody
    @RequirePermission(roles = Constants.SYSTEM_USER)
    public Result<ReportVO> getReportCardDataExcludeHighRisk(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam String college,
            @RequestParam String dormitoryBuilding) {
        // 校验参数
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

        ReportVO reportVO = reportService.statsReportCardData(startDate, endDate, college, dormitoryBuilding);

        return Result.success(reportVO);


    }

    /**
     * 获取高危预警人数计算结果
     * 
     * @param startDate 起始时间
     * @param endDate   终止时间
     * @return 计算结果
     */
    @ResponseBody
    @GetMapping("/high-risk-count") // todo 此处前端时间形式需要适配 以及处理学院 宿舍参数
    public Result<CalculationResult> getHighRiskCount(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam String college,
            @RequestParam String dormitoryBuilding) {
        // 获取精确到时分秒的时间范围
        Map<String, Date> timeRangeMap = DateUtils.resolveDateRange(startDate, endDate);
        Date startTime = timeRangeMap.get(Constants.START_TIME);
        Date endTime = timeRangeMap.get(Constants.END_TIME);

        CalculationResult calculationResult = warningRuleService.calculateHighRiskStudents(startTime, endTime);
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
}
