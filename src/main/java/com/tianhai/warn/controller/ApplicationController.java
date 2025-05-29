package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.Application;
import com.tianhai.warn.query.ApplicationQuery;
import com.tianhai.warn.service.ApplicationService;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.utils.Result;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 晚归报备申请控制器
 */
@Controller
@RequestMapping("/application")
public class ApplicationController {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationController.class);

    @Autowired
    private ApplicationService applicationService;

    @Value("E:/Warning/Warn/uploads")
    private String uploadPath;

    /**
     * 根据申请ID查询晚归申请
     */
    @GetMapping("/{applicationId}")
    @ResponseBody
    @RequirePermission
    @LogOperation("查询晚归申请")
    public Result<Application> getByApplicationId(@PathVariable String applicationId) {
        if (applicationId == null) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        Application returnApplication = applicationService.selectByApplicationId(applicationId);
        Result<Application> result = new Result<>();
        result.setData(returnApplication);

        return result;
    }

    /**
     * 根据学号查询晚归申请
     */
    @GetMapping("/student/{studentNo}")
    @ResponseBody
    @RequirePermission
    @LogOperation("查询晚归申请")
    public List<Application> getByStudentNo(@PathVariable String studentNo) {
        return applicationService.selectByStudentNo(studentNo);
    }

    @PostMapping("/pageList")
    @ResponseBody
    @RequirePermission
    @LogOperation("分页查询晚归申请")
    public Result<PageResult<Application>> searchPageList(@RequestBody ApplicationQuery query) {
        if (query.getPageNum() == null || query.getPageNum() <= 0) {
            query.setPageNum(1);
        }
        if (query.getPageSize() == null || query.getPageSize() <= 0) {
            query.setPageSize(10);
        }

        PageResult<Application> applications = applicationService.selectByPageQuery(query);
        Result<PageResult<Application>> result = Result.success(applications);

        return result;
    }

    @PostMapping("/list")
    @ResponseBody
    @RequirePermission
    @LogOperation("查询晚归申请")
    public Result<List<Application>> searchConditionalList(@RequestBody ApplicationQuery query) {
        List<Application> applicationList = applicationService.selectByCondition(query);

        return Result.success(applicationList);
    }

    // /**
    // * 查询所有晚归申请
    // */
    // @GetMapping("/list")
    // @ResponseBody
    // public List<Application> getAll() {
    // return applicationService.selectAll();
    // }

    /**
     * 根据条件查询晚归申请
     */
    // @PostMapping("/search")
    // @ResponseBody
    // public List<Application> search(@RequestBody Application application) {
    // return applicationService.selectByCondition(application);
    // }

    /**
     * 添加晚归申请
     */
    @PostMapping("/add")
    @ResponseBody
    @RequirePermission
    @LogOperation("添加晚归申请")
    public Result<String> add(@RequestParam("expectedReturnTime") String expectedReturnTime,
            @RequestParam("reason") String reason,
            @RequestParam("destination") String destination,
            @RequestParam("studentNo") String studentNo,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        // 参数校验
        if (StringUtils.isBlank(studentNo)) {
            return Result.error("学号不能为空");
        }
        if (StringUtils.isBlank(expectedReturnTime)) {
            return Result.error("预计返回时间不能为空");
        }
        if (StringUtils.isBlank(reason)) {
            return Result.error("晚归原因不能为空");
        }
        if (StringUtils.isBlank(destination)) {
            return Result.error("出行目的地不能为空");
        }

        // 验证日期格式和有效性
        Date returnTime;
        try {
            // 使用完整的日期时间格式解析，包含秒
            returnTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(expectedReturnTime);
            // 验证日期是否合法（不能是过去的日期）
            if (returnTime.before(new Date())) {
                return Result.error("预计返回时间不能是过去的日期");
            }
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

        // 调用服务层处理业务逻辑
        Integer affectRows = applicationService.submitApplication(studentNo, returnTime, reason, destination, file);

        if (affectRows == 0) {
            return Result.error(ResultCode.APPLICATION_SAVE_FAILED);
        } else {
            return Result.success("提交成功");
        }
    }

    /**
     * 更新晚归申请
     */
    @PostMapping("/update")
    @ResponseBody
    public String update(@RequestBody Application application) {
        try {
            applicationService.update(application);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 删除晚归申请
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public String delete(@PathVariable Long id) {
        try {
            applicationService.deleteById(id);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 根据审核状态查询晚归申请
     */
    @GetMapping("/audit-status/{auditStatus}")
    @ResponseBody
    public List<Application> getByAuditStatus(@PathVariable Integer auditStatus) {
        return applicationService.selectByAuditStatus(auditStatus);
    }

    /**
     * 根据预期回校时间范围查询晚归申请
     */
    @GetMapping("/time-range")
    @ResponseBody
    public List<Application> getByTimeRange(@RequestParam Date startTime, @RequestParam Date endTime) {
        return applicationService.selectByExpectedReturnTimeRange(startTime, endTime);
    }

    /**
     * 审核晚归申请
     */
    @PostMapping("/audit")
    @ResponseBody
    @RequirePermission(roles = {Constants.SYSTEM_USER})
    @LogOperation("审核晚归申请")
    public String audit(@RequestParam Long id,
            @RequestParam Integer auditStatus,
            @RequestParam String auditPerson,
            @RequestParam String auditRemark) {
        try {
            applicationService.auditApplication(id, auditStatus, auditPerson, auditRemark);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    // 校验提交的晚归申请信息
    private void validateApplication(Application application) {
        // 创建错误信息列表
        List<String> errors = new ArrayList<>();

        // 验证必填字段
        if (StringUtils.isBlank(application.getStudentNo())) {
            errors.add("姓名不能为空");
        }

        if (application.getExpectedReturnTime().before(new Date())) {
            errors.add("返校日期有误");
        }

        if (StringUtils.isBlank(application.getReason())) {
            errors.add("晚归简要原因不能为空");
        }

        if (StringUtils.isBlank(application.getDestination())) {
            errors.add("晚归详细原因不能为空");
        }

        // todo 晚归材料判空 晚归材料视频内容审核

        if (!errors.isEmpty()) {
            throw new BusinessException(String.join("; ", errors));
        }
    }
}