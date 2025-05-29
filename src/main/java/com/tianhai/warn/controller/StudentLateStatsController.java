package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.*;
import com.tianhai.warn.query.StudentLateStatsQuery;
import com.tianhai.warn.service.LateReturnService;
import com.tianhai.warn.service.StudentLateStatsService;
import com.tianhai.warn.service.WarningRuleService;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.utils.Result;
import com.tianhai.warn.utils.RoleObjectCaster;
import jakarta.servlet.http.HttpSession;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/period-stats")
public class StudentLateStatsController {
    private static final Logger logger = LoggerFactory.getLogger(StudentLateStatsController.class);

    @Autowired
    private StudentLateStatsService studentLateStatsService;

    @Autowired
    private LateReturnService lateReturnService;

    @Autowired
    private WarningRuleService warningRuleService;

    /**
     * 根据主键ID获取学生晚归统计详情
     * @param id 统计记录的主键ID
     */
    @GetMapping("/{id}")
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("获取晚归统计详情")
    public Result<StudentLateStats> getStatsById(@PathVariable Integer id) {
        if (id == null || id == 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        StudentLateStats studentLateStats = studentLateStatsService.getById(id);

        return Result.success(studentLateStats);
    }

    /**
     * 根据统计记录ID (statsId) 获取学生晚归统计详情
     * @param statsId 统计记录的统计记录ID（非主键）
     */
    @GetMapping("/{statsId}")
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("获取晚归统计详情")
    public Result<StudentLateStats> getStatsByStatsId(@PathVariable String statsId) {
        if (StringUtils.isBlank(statsId) || statsId.startsWith("ST")) {
            logger.error("statsId:{}不合法", statsId);
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        StudentLateStats studentLateStats = studentLateStatsService.getByStatsId(statsId);

        return Result.success(studentLateStats);
    }

    /**
     * 分页查询学生晚归统计列表
     * @param query 查询条件 (包含分页参数，如 pageNum, pageSize，以及其他过滤条件)
     *              Spring MVC 会自动将请求参数绑定到 StudentLateStatsQuery 对象
     */
    @PostMapping("/pageList")
    @ResponseBody
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("分页查询晚归统计")
    public Result<PageResult<StudentLateStats>> pageList(StudentLateStatsQuery query) {
        if (query.getPageNum() == null || query.getPageNum() <= 0) {
            query.setPageNum(Constants.DEFAULT_PAGE_NUM);
        }
        if (query.getPageSize() == null || query.getPageSize() <= 0) {
            query.setPageSize(Constants.DEFAULT_PAGE_SIZE);
        }

        PageResult<StudentLateStats> pageResult = studentLateStatsService.selectByPageQuery(query);

        return Result.success(pageResult);
    }

    /**
     * 手动触发周期性统计任务
     * @return      触发结果
     */
    @PostMapping("/trigger-stats")
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("手动触发周期性统计任务")
    public Result<Void> triggerPeriodicStats(HttpSession session) {
        Object currentUser = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);
        SysUser currentSysUser = RoleObjectCaster.cast(Constants.SYSTEM_USER, currentUser);
        String sysUserNo = currentSysUser.getSysUserNo();

        logger.info("手动触发晚归统计任务，触发人工号：{}", sysUserNo);

        studentLateStatsService.generateAndStorePeriodicLateReturnStats();

        return Result.success();
    }

    /**
     * 创建单条晚归统计记录
     * @param stats     统计记录
     * @return          创建结果
     */
    @PostMapping("/create")
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("创建晚归统计记录")
    public Result<StudentLateStats> createStatsRecord(@RequestBody StudentLateStats stats) {
        if (stats == null) {
            logger.error("提交的统计记录不能为空");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        StudentLateStats createdStats = studentLateStatsService.createStats(stats);

        return Result.success();
    }

    /**
     * 批量创建晚归统计记录
     * @param statsList     统计记录列表
     * @return              创建结果
     */
    @PostMapping("/batch-create")
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("批量创建晚归统计记录")
    public Result<Void> batchCreateStatsRecords(@RequestBody List<StudentLateStats> statsList) {
        if (statsList == null || statsList.isEmpty()) {
            logger.error("提交数据不能为空");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        studentLateStatsService.batchCreateStats(statsList);
        return Result.success();
    }

    /**
     *更新一条学生晚归统计记录
     * @param stats 要更新的记录的统计记录
     * @return      更新结果
     */
    @PutMapping("/update/{statsId}")
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("更新晚归统计记录")
    public Result<StudentLateStats> updateStatsRecord(@RequestBody StudentLateStats stats) {
        if (stats == null || StringUtils.isBlank(stats.getStatsId())) {
            logger.error("修改的统计记录不合法");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        StudentLateStats updatedStats = studentLateStatsService.updateStats(stats);

        return Result.success(updatedStats);
    }

    //todo
    @PostMapping("/batch-update")
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("批量更新晚归统计记录")
    public Result<Integer> batchUpdateStatsRecords(@RequestBody List<StudentLateStats> statsList) {
        if (statsList == null || statsList.isEmpty()) {
            logger.error("批量修改的统计记录不合法");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        return Result.success(0);
    }







}
