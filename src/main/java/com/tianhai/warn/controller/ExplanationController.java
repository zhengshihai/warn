package com.tianhai.warn.controller;

import cn.hutool.core.bean.BeanUtil;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.AuditActionDTO;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.enums.UserRole;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.Explanation;
import com.tianhai.warn.query.ExplanationQuery;
import com.tianhai.warn.query.LateReturnQuery;
import com.tianhai.warn.service.ExplanationService;
import com.tianhai.warn.service.LateReturnService;
import com.tianhai.warn.utils.DateUtils;
import com.tianhai.warn.utils.Result;
import jakarta.validation.Valid;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 晚归情况说明控制器
 */
@Controller
@RequestMapping("/explanation")
public class ExplanationController {

    private static final Logger logger = LoggerFactory.getLogger(ExplanationController.class);

    @Autowired
    private ExplanationService explanationService;

    @Autowired
    private LateReturnService lateReturnService;

    /**
     * 根据晚归记录id查询晚归说明
     */
    @GetMapping("/late-return/{lateReturnId}")
    @ResponseBody
    @RequirePermission
    public Result<Explanation> getByLateReturnId(@PathVariable String lateReturnId) {
        if (StringUtils.isBlank(lateReturnId)) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        ExplanationQuery explanationQuery = new ExplanationQuery();
        explanationQuery.setLateReturnId(lateReturnId);

        List<Explanation> explanations = explanationService.selectByCondition(explanationQuery);
        if (!explanations.isEmpty()) {
            return Result.success(explanations.get(0));
        }

        return Result.success();
    }


    /**
     * 根据学号查询晚归说明
     */
    @GetMapping("/student/{studentNo}")
    @ResponseBody
    public List<Explanation> getByStudentNo(@PathVariable String studentNo) {
        return explanationService.selectByStudentNo(studentNo);
    }

    /**
     * 查询所有晚归说明
     */
    @GetMapping("/list")
    @ResponseBody
    public List<Explanation> getAll() {
        return explanationService.selectAll();
    }

    /**
     * 根据条件查询晚归说明
     */
    @PostMapping("/search")
    @ResponseBody
    public List<Explanation> search(@RequestBody ExplanationQuery explanationQuery) {
        return explanationService.selectByCondition(explanationQuery);
    }

    /**
     * 添加晚归说明
     */
    @PostMapping("/add")
    @ResponseBody
    @RequirePermission(roles = {Constants.STUDENT})
    public Result<String> add(@RequestParam("studentNo") String studentNo,
            @RequestParam("reason") String reason,
            @RequestParam("description") String description,
            @RequestParam("lateReturnDate") String lateReturnDate,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        // 添加日志
        logger.info("Received parameters: studentNo={}, reason={}, description={}, lateReturnDate={}, file={}",
                studentNo, reason, description, lateReturnDate, file != null ? file.getOriginalFilename() : "null");

        // 参数校验
        if (StringUtils.isBlank(studentNo)) {
            return Result.error("学号不能为空");
        }
        if (StringUtils.isBlank(reason)) {
            return Result.error("晚归简要原因不能为空");
        }
        if (StringUtils.isBlank(description)) {
            return Result.error("晚归详细原因不能为空");
        }
        if (StringUtils.isBlank(lateReturnDate)) {
            return Result.error("晚归日期不能为空");
        }

        Date lateTime;
        LateReturnQuery lateReturnQuery;
        try {
            lateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .parse(lateReturnDate);
            if (lateTime.after(new Date())) {
                return Result.error("已经晚归的时间不能是将来的日期");
            }

            // 获取当天的开始时间和结束时间
            Date[] dayRange = DateUtils.getDayRange(lateTime);
            lateReturnQuery = new LateReturnQuery();
            lateReturnQuery.setStartLateTime(dayRange[0]);
            lateReturnQuery.setEndLateTime(dayRange[1]);
            lateReturnQuery.setStudentNo(studentNo);
        } catch (ParseException e) {
            return Result.error("日期格式不正确，请使用yyyy-MM-dd HH:mm:ss格式");
        }

        // 验证文件
        if (file != null && !file.isEmpty()) {
            // 验证文件大小（限制为5MB）
            if (file.getSize() > 5 * 1024 * 1024) {
                logger.error("文件大小不能超过5MB");
                throw new BusinessException(ResultCode.FILE_SIZE_ERROR);
            }
            // 验证文件类型
            String contentType = file.getContentType();
            if (contentType == null ||
                    (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                logger.error("只支持图片和PDF格式的文件");
                throw new BusinessException(ResultCode.FILE_TYPE_ERROR);
            }
        }

        Integer affectRows = explanationService.submitExplanation(
                lateReturnQuery, studentNo, reason, description, file);

        if (affectRows == 0) {
            return Result.error(ResultCode.EXPLANATION_SAVE_FAILED);
        } else {
            return Result.success("提交成功");
        }
    }

    @PostMapping("/audit-action")
    @ResponseBody
    @RequirePermission(roles = {Constants.DORMITORY_MANAGER, Constants.SYSTEM_USER})
    public Result<?> auditAction(@Valid  @RequestBody AuditActionDTO auditActionDTO) {
        Integer updateResult = explanationService.auditExplanation(auditActionDTO);
        if (updateResult < 2) {
            return Result.error(ResultCode.ERROR);
        }

        return Result.success();
    }

    /**
     * 更新晚归说明
     */
    @PostMapping("/update")
    @ResponseBody
    public Result<?> update(@RequestBody ExplanationQuery explanationQuery) {
        try {
            explanationService.update(explanationQuery);
            return Result.success();
        } catch (Exception e) {
            return Result.error(ResultCode.ERROR);
        }
    }

    /**
     * 删除晚归说明
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public String delete(@PathVariable Long id) {
        try {
            explanationService.deleteById(id);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 根据审核状态查询晚归说明
     */
    @GetMapping("/audit-status/{auditStatus}")
    @ResponseBody
    public List<Explanation> getByAuditStatus(@PathVariable Integer auditStatus) {
        return explanationService.selectByAuditStatus(auditStatus);
    }

}