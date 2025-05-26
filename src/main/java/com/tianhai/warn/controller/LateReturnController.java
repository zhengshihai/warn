package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.enums.UserRole;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.model.LateReturn;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.query.LateReturnQuery;
import com.tianhai.warn.service.LateReturnService;
import com.tianhai.warn.service.DormitoryManagerService;
import com.tianhai.warn.service.StudentService;
import com.tianhai.warn.service.SysUserClassService;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.utils.Result;

import com.tianhai.warn.utils.RoleObjectCaster;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 晚归记录控制器
 */
@Controller
@RequestMapping("/late-return")
public class LateReturnController {

    private static final Logger logger = LoggerFactory.getLogger(LateReturnController.class);

    @Autowired
    private LateReturnService lateReturnService;

    @Autowired
    private DormitoryManagerService dormitoryManagerService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private SysUserClassService sysUserClassService;


    @GetMapping("/{lateReturnId}")
    @ResponseBody
    @RequirePermission
    public Result<LateReturn> searchByLateReturnId(@PathVariable String lateReturnId) {
        if (StringUtils.isBlank(lateReturnId)) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        LateReturnQuery lateReturnQuery = new LateReturnQuery();
        lateReturnQuery.setLateReturnId(lateReturnId);
        LateReturn lateReturn = lateReturnService.selectByCondition(lateReturnQuery).get(0);

        return Result.success(lateReturn);
    }


    @PostMapping("/pageList")
    @ResponseBody
    @RequirePermission
    public Result<PageResult<LateReturn>> pageList(@RequestBody LateReturnQuery query) {
        try {
            // 参数校验
            if (query == null) {
                return Result.error(ResultCode.VALIDATE_FAILED);
            }

            // 时间范围校验
            if (query.getStartLateTime() != null && query.getEndLateTime() != null) {
                if (query.getStartLateTime().after(query.getEndLateTime())) {
                    return Result.error("开始时间不能晚于结束时间");
                }
            }

            // 分页参数校验
            if (query.getPageNum() == null || query.getPageNum() < 1) {
                query.setPageNum(Constants.DEFAULT_PAGE_NUM);
            }
            if (query.getPageSize() == null || query.getPageSize() < 1) {
                query.setPageSize(Constants.DEFAULT_PAGE_SIZE);
            }

            // 执行查询
            PageResult<LateReturn> pageResult = lateReturnService.selectByPageQuery(query);
            return Result.success(pageResult);

        } catch (Exception e) {
            logger.error("查询晚归记录失败", e);
            return Result.error("查询失败：" + e.getMessage());
        }
    }

    @PostMapping("/time")
    @ResponseBody
    @RequirePermission
    public Result<List<LateReturn>> searchByTimeRange(@RequestBody LateReturnQuery lateReturnQuery) {
        Date startTime = lateReturnQuery.getStartLateTime();
        Date endTime = lateReturnQuery.getEndLateTime();
        if (startTime == null) {
            logger.error("起始时间不正确");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }
        if (endTime == null) {
            endTime = new Date();
        }

        List<LateReturn> lateReturnList = lateReturnService.selectByTimeRange(startTime, endTime);

        return Result.success(lateReturnList);
    }

    /**
     * 获取晚归统计数据
     */
    @PostMapping("/stats")
    @ResponseBody
    @RequirePermission
    public Result<Map<String, Object>> getStatistics(
            @RequestParam(required = false) String startDateStr,
            @RequestParam(required = false) String endTimeStr,
            HttpSession session) {

        // 参数校验
        if (session == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        String userRoleStr = (String) session.getAttribute(Constants.SESSION_ATTRIBUTE_ROLE);
        Object currentUserObj = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);

        if (userRoleStr == null || currentUserObj == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        // 调用Service层获取统计数据
        Map<String, Object> statistics = lateReturnService.getStatistics(
                startDateStr, endTimeStr, userRoleStr, currentUserObj);

        return Result.success(statistics);
    }

   

    @PostMapping("/list")
    @ResponseBody
    @RequirePermission
    public Result<List<LateReturn>> searchConditionalList(@RequestBody LateReturnQuery lateReturnQuery) {
        List<LateReturn> lateReturnList = lateReturnService.selectByCondition(lateReturnQuery);

        return Result.success(lateReturnList);
    }

    /**
     * 根据处理状态查询晚归记录
     */
    @GetMapping("/process-status/{processStatus}")
    @ResponseBody
    public List<LateReturn> getByProcessStatus(@PathVariable String processStatus) {
        return lateReturnService.selectByProcessStatus(processStatus);
    }

    /**
     * 根据处理结果查询晚归记录
     */
    @GetMapping("/process-result/{processResult}")
    @ResponseBody
    public List<LateReturn> getByProcessResult(@PathVariable String processResult) {
        return lateReturnService.selectByProcessResult(processResult);
    }

    /**
     * 根据时间范围查询晚归记录
     */
    @GetMapping("/time-range")
    @ResponseBody
    public List<LateReturn> getByTimeRange(@RequestParam Date startTime, @RequestParam Date endTime) {
        return lateReturnService.selectByTimeRange(startTime, endTime);
    }

    /**
     * 更新晚归记录处理状态
     */
    @PostMapping("/update")
    @ResponseBody
    @RequirePermission( roles = {Constants.SYSTEM_USER, Constants.DORMITORY_MANAGER})
    public String update(@RequestParam String lateReturnId,
            @RequestParam String processStatus,
            @RequestParam String processResult,
            @RequestParam String processRemark) {
        try {
            lateReturnService.updateProcessStatus(lateReturnId, processStatus, processResult, processRemark);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 跳转到添加晚归记录页面
     */
    @GetMapping("/add")
    public String toAdd() {
        return "late-return/add";
    }

    /**
     * 添加晚归记录
     */
    @PostMapping("/add")
    @ResponseBody
    public String add(@RequestBody LateReturn lateReturn) {
        try {
            lateReturn.setProcessStatus("PENDING"); // 设置初始状态为待处理
            lateReturnService.insert(lateReturn);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 跳转到编辑晚归记录页面
     */
    // @GetMapping("/edit/{id}")
    // public String toEdit(@PathVariable String lateReturnId, Model model) {
    // LateReturn lateReturn = lateReturnService.selectBy
    // model.addAttribute("lateReturn", lateReturn);
    // return "late-return/edit";
    // }


    /**
     * 删除晚归记录
     */
    @PostMapping("/delete/{lateReturnId}")
    @ResponseBody
    public String delete(@PathVariable String lateReturnId) {
        try {
            lateReturnService.deleteByLateReturnId(lateReturnId);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 跳转到处理晚归记录页面
     */
    @GetMapping("/handle/{lateReturnId}")
    @RequirePermission(roles = {Constants.DORMITORY_MANAGER, Constants.SYSTEM_USER})
    public String toHandle(@PathVariable String lateReturnId, Model model, HttpSession session) {
        // 获取当前用户角色和对象
        String userRoleStr = (String) session.getAttribute(Constants.SESSION_ATTRIBUTE_ROLE);
        Object currentUserObj = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);

        // 添加调试日志
        logger.debug("Current user role: {}", userRoleStr);
        logger.debug("Current user object: {}", currentUserObj);

        // 检查权限
        if (!UserRole.DORMITORY_MANAGER.getCode().equalsIgnoreCase(userRoleStr)) {
            throw new BusinessException("无权限访问");
        }

        // 获取用户信息
        if (userRoleStr.equalsIgnoreCase(Constants.DORMITORY_MANAGER)) {
            DormitoryManager dormitoryManager = RoleObjectCaster.cast(userRoleStr, currentUserObj);
            model.addAttribute(Constants.DORMITORY_MANAGER, dormitoryManager);
            model.addAttribute(Constants.SESSION_ATTRIBUTE_AUDIT_PERSON, dormitoryManager.getManagerId());
        }
        if (userRoleStr.equalsIgnoreCase(Constants.SYSTEM_USER)) {
            SysUser sysUser = RoleObjectCaster.cast(userRoleStr, currentUserObj);
            model.addAttribute(Constants.SYSTEM_USER, sysUser);
            model.addAttribute(Constants.SESSION_ATTRIBUTE_AUDIT_PERSON, sysUser.getSysUserNo());
        }

        // 获取晚归记录
        LateReturn lateReturn = lateReturnService.getByLateReturnId(lateReturnId);
        if (lateReturn == null) {
            throw new BusinessException("晚归记录不存在");
        }
        model.addAttribute(Constants.SESSION_ATTRIBUTE_LATE_RETURN, lateReturn);

        // 获取学生信息
        Student student = studentService.selectByStudentNo(lateReturn.getStudentNo());
        if (student != null) {
            model.addAttribute(Constants.STUDENT, student);
        }

        return "deal-late-record";
    }

}