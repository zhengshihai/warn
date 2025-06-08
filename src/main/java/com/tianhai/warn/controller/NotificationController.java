package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.model.Notification;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.query.NotificationQuery;
import com.tianhai.warn.utils.IdValidator;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.utils.Result;
import com.tianhai.warn.utils.RoleObjectCaster;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.tianhai.warn.service.NotificationService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 通知信息控制器
 */
@Controller
@RequestMapping("/notification")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 分页获取当前用户端额未读通知
     * @param query 查询条件
     * @param session   会话
     * @return          通知列表
     */
    @PostMapping("/unread-page-list")
    @ResponseBody
    @RequirePermission
    @LogOperation("分页查询通知")
    public Result<PageResult<Notification>> getUnreadNotificationsPage(@RequestBody NotificationQuery query,
                                                                       HttpSession session) {
        if (query == null) {
            return Result.error(ResultCode.VALIDATE_FAILED);
        }

        // 筛选最近一个月的通知
        if (query.getStartNoticeTime() == null || query.getEndNoticeTime() == null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = now.minusMonths(Constants.NOTIFICATION_PERIOD_MONTH);
            query.setStartNoticeTime(Timestamp.valueOf(startTime));
            query.setEndNoticeTime(Timestamp.valueOf(now));
        }

        // 分页参数校验
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(Constants.DEFAULT_PAGE_NUM);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(Constants.DEFAULT_PAGE_SIZE);
        }

        String userRoleStr = (String) session.getAttribute(Constants.SESSION_ATTRIBUTE_ROLE);
        Object currentUser = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);
        if (userRoleStr == null || currentUser == null) {
            return Result.error(ResultCode.UNAUTHORIZED);
        }

        query.setStatus(Constants.UNREAD);

        PageResult<Notification> notificationList =
                notificationService.selectByPageQuery(userRoleStr, currentUser, query);
        if (notificationList == null || notificationList.getData() == null
               || notificationList.getData().isEmpty()) {
            return Result.success(new PageResult<>());
        }

        return Result.success(notificationList);
    }

    /**
     * 获取学生的未读通知
     * @param studentNo
     * @param session
     * @return
     */
    @GetMapping("/student/unread/{studentNo}")
    @ResponseBody
    @RequirePermission(roles = {Constants.STUDENT})
    @LogOperation("获取学生未读通知")
    public Result<List<Notification>> getStudentUnreadNotifications(@PathVariable String studentNo,
                                                            HttpSession session) {
        // 验证当前登录用户是否为该学生
        Object currentUser = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);
        Student currentStudent = RoleObjectCaster.cast(Constants.STUDENT, currentUser);
        if (!currentStudent.getStudentNo().equals(studentNo)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        NotificationQuery query = new NotificationQuery();
        query.setTargetId(studentNo);
        query.setStatus(Constants.UNREAD);
        //todo 实现读取all类型的通知
        List<Notification> notificationList = notificationService.selectByCondition(query);

        return Result.success(notificationList);
    }

    /**
     * 获取系统用户（辅导员 班主任等）的未读通知
     * @param sysUserNo
     * @param session
     * @return
     */
    @GetMapping("sys-user/unread/{sysUserNo}")
    @ResponseBody
    @RequirePermission(roles = {Constants.SYSTEM_USER})
    @LogOperation("获取学生未读通知")
    public Result<List<Notification>> getSysUserUnreadNotifications(@PathVariable String sysUserNo,
                                                                    HttpSession session) {
        // 验证当前登录用户是否为该系统用户
        Object currentUser = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);
        SysUser currentSysUser = RoleObjectCaster.cast(Constants.SYSTEM_USER, currentUser);
        if (!currentSysUser.getSysUserNo().equals(sysUserNo)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        NotificationQuery query = new NotificationQuery();
        query.setTargetId(sysUserNo);
        query.setStatus(Constants.UNREAD);
        List<Notification> notificationList = notificationService.selectByCondition(query);

        return Result.success(notificationList);
    }

    /**
     * 获取宿管的未读通知
     * @param managerId
     * @param session
     * @return
     */
    @GetMapping("/dor-man/unread/{managerId}")
    @ResponseBody
    @RequirePermission(roles = {Constants.DORMITORY_MANAGER})
    @LogOperation("获取宿管未读通知")
    public Result<List<Notification>> getDormitoryUnreadNotifications(@PathVariable String managerId,
                                                              HttpSession session) {
        // 验证当前登录用户是否为该宿管
        Object currentUser = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);
        DormitoryManager curDormitoryManager = RoleObjectCaster.cast(Constants.DORMITORY_MANAGER, currentUser);
        if (!curDormitoryManager.getManagerId().equals(managerId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        NotificationQuery query = new NotificationQuery();
        query.setTargetId(managerId);
        query.setStatus(Constants.UNREAD);
        List<Notification> notificationList = notificationService.selectByCondition(query);

        return Result.success(notificationList);
    }


    /**
     * 标记通知为已读
     * @param noticeId
     * @param session
     * @return
     */
    @GetMapping("/mark-read/{noticeId}")
    @ResponseBody
    @RequirePermission
    @LogOperation("标记通知为已读")
    public Result<String> markAsRead(@PathVariable String noticeId, HttpSession session) {
        Object currentUser = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);
        String userRoleStr = (String) session.getAttribute(Constants.SESSION_ATTRIBUTE_ROLE);

        System.out.println("currentUser: " + currentUser);
        System.out.println("userRoleStr: " + userRoleStr);

        // 验证通知id形式是否正确
        boolean validNoticeId = IdValidator.isValid(noticeId);
        if (!validNoticeId) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        // 验证用户是否有权限操作这些通知
        if (!notificationService.hasPermissionToRead(userRoleStr, currentUser, noticeId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        Integer affectRows = notificationService.updateStatus(noticeId, Constants.READ);

        if (affectRows <= 0) {
            return Result.error(ResultCode.NOTIFICATION_UPDATE_FAILED);
        } else {
            return Result.success();
        }
    }

    /**
     * 批量标记通知已读
     * @param noticeIds
     * @param httpSession
     * @return
     */
    @PostMapping("/batch-mark-read")
    @ResponseBody
    @RequirePermission
    @LogOperation("批量更新通知为已读")
    public Result<String> batchMarkAsRead(@RequestBody List<String> noticeIds,
                                          HttpSession httpSession) {
        Object currentUser = httpSession.getAttribute(Constants.SESSION_ATTRIBUTE_USER);
        String userRoleStr = (String) httpSession.getAttribute(Constants.SESSION_ATTRIBUTE_USER);

        // 验证用户是否有权限操作这些通知
        if (!notificationService.hasPermissionToReadBatch(userRoleStr, currentUser, noticeIds)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }

        notificationService.batchUpdateStatus(noticeIds, Constants.READ);

        return Result.success();
    }

    /**
     * 获取通知统计信息
     */
    @GetMapping("/stats")
    @ResponseBody
    @RequirePermission
    @LogOperation("获取通知统计信息")
    public Result<Map<String, Object>> getNotificationsStats(HttpSession session) {
        Object currentUser = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);
        if (currentUser == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        Map<String, Object> statsMap = notificationService.getNotificationStats(currentUser);

        return Result.success(statsMap);
    }


    /**
     * 跳转到通知列表页面
     */
    @GetMapping("/list")
    public String list(Model model) {
        List<Notification> notifications = notificationService.selectAll();
        model.addAttribute("notifications", notifications);
        return "notification/list";
    }

    /**
     * 跳转到添加通知页面
     */
    @GetMapping("/add")
    public String toAdd() {
        return "notification/add";
    }

    /**
     * 添加通知
     */
    @PostMapping("/add")
    @ResponseBody
    public String add(@RequestBody Notification notification) {
        try {
            notificationService.insert(notification);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 跳转到编辑通知页面
     */
    @GetMapping("/edit/{id}")
    public String toEdit(@PathVariable Integer id, Model model) {
        Notification notification = notificationService.selectById(id);
        model.addAttribute("notification", notification);
        return "notification/edit";
    }

    /**
     * 更新通知
     */
    @PostMapping("/update")
    @ResponseBody
    public String update(@RequestBody Notification notification) {
        try {
            notificationService.update(notification);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 删除通知
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public String delete(@PathVariable Integer id) {
        try {
            notificationService.deleteById(id);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 根据条件查询通知
     */
    @PostMapping("/search")
    @ResponseBody
    public List<Notification> search(@RequestBody NotificationQuery notificationQuery) {
        return notificationService.selectByCondition(notificationQuery);
    }

    /**
     * 根据目标类型和目标ID查询通知
     */
    @GetMapping("/target")
    @ResponseBody
    public List<Notification> getByTarget(@RequestParam String targetType, @RequestParam String targetId) {
        return notificationService.selectByTarget(targetType, targetId);
    }

    /**
     * 根据通知类型查询通知
     */
    @GetMapping("/type/{type}")
    @ResponseBody
    public List<Notification> getByType(@PathVariable String type) {
        return notificationService.selectByType(type);
    }

    /**
     * 根据状态查询通知
     */
    @GetMapping("/status/{status}")
    @ResponseBody
    public List<Notification> getByStatus(@PathVariable String status) {
        return notificationService.selectByStatus(status);
    }

    /**
     * 更新通知状态
     */
    @PostMapping("/update-status")
    @ResponseBody
    public String updateStatus(@RequestParam String notificationId, @RequestParam String status) {
        try {
            notificationService.updateStatus(notificationId, status);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 批量更新通知状态
     */
    @PostMapping("/batch-update-status")
    @ResponseBody
    public String batchUpdateStatus(@RequestParam List<String> notificationIds, @RequestParam String status) {
        try {
            notificationService.batchUpdateStatus(notificationIds, status);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }
}