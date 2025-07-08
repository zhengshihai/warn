package com.tianhai.warn.listeners;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.AuditActionDTO;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.events.AuditEvent;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.Notification;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.model.SysUserClass;
import com.tianhai.warn.query.SysUserQuery;
import com.tianhai.warn.service.*;
import com.tianhai.warn.utils.NoticeIdGenerator;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 审核事件监听类
 */
@Component
public class AuditEventListener implements ApplicationListener<AuditEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AuditEventListener.class);

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    @Autowired
    private StudentService studentService;

    @Autowired
    private SysUserClassService sysUserClassService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private Environment environment;

    /**
     * 监听审核事件
     * 晚归说明第一审核人为宿管， 第二审核人为辅导员， 第三审核人为班主任
     * 当进入onApplicationEvent()时，说明第一审核人无法确定审核，只能转发给第二审核人
     * 
     * @param event 审核事件
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void onApplicationEvent(@NonNull AuditEvent event) {
        // 使用配置的异步执行器
        asyncTaskExecutor.execute(() -> {
            try {
                processAuditEvent(event);
            } catch (Exception e) {
                logger.error("响应审核事件失败", e);
                throw new BusinessException(ResultCode.ERROR);
            }
        });
    }

    /**
     * 处理审核事件
     * 
     * @param event 审核事件
     */
    @Transactional(rollbackFor = Exception.class)
    protected void processAuditEvent(AuditEvent event) {
        AuditActionDTO auditActionDTO = event.getAuditActionDTO();
        Student student = studentService.selectByStudentNo(auditActionDTO.getStudentNo());

        String nextAuditPersonEmail = null;
        Notification notification = new Notification();

        String currentJobRole = auditActionDTO.getJobRole();
        SysUser nextAuditor = getNextAuditor(student, currentJobRole);

        // 验证收到该晚归说明审核的人是否有审核权限
        validateAuditPermission(nextAuditor, student, auditActionDTO.getAuditPerson());

        // 创建并保存通知记录
        String nextJobRole = getJobRoleFromNextAuditor(currentJobRole);
        notification.setTargetType(nextJobRole);
        notification = createAndSaveNotification(student, nextAuditor);


        String[] profiles = environment.getActiveProfiles();
        if (Arrays.asList(profiles).contains("prod")) {
            // 生产环境直接通过nextAuditor获取目标邮件地址
            nextAuditPersonEmail = nextAuditor.getEmail();
        } else if (Arrays.asList(profiles).contains("dev")){
            // 开发环境下使用此邮箱进行接收测试
            nextAuditPersonEmail = Constants.EMAIL_DEV_RECEIVER;
        }

        // 发送邮件通知 方案一
        emailService.send(nextAuditPersonEmail,
                notification.getTitle(),
                buildNotificationContent(student),
                false);

        //发送邮件通知 方案二 注册事务提交后的邮件发送回调
//        if (nextAuditPersonEmail != null) {
//            logger.info("After commit: 准备邮件发送工作，目标邮件:{}", nextAuditPersonEmail);
//            if (TransactionSynchronizationManager.isSynchronizationActive()) {
//                TransactionSynchronizationManager.registerSynchronization(
//                        new EmailSendAfterCommit(
//                                emailService,
//                                nextAuditPersonEmail,
//                                notification.getTitle(),
//                                buildNotificationContent(student)
//                        )
//                );
//            }
//            logger.info("After commit: 邮件发送成功");
//        }

        // 统计对应周期内学生不正当晚归是否超过阈值 并发送预警通知


    }

    /**
     * 确定下一个审核人的职位角色
     * 
     * @param currentJobRole 当前审核人的职位角色
     * @return 下一个审核人的职位角色
     */
    private String getJobRoleFromNextAuditor(String currentJobRole) {
        String nextJobRole;
        if (StringUtils.isBlank(currentJobRole)) {
            // 当前是宿管，下一个是辅导员
            nextJobRole = Constants.JOB_ROLE_COUNSELOR;
        } else if (Constants.JOB_ROLE_COUNSELOR.equalsIgnoreCase(currentJobRole)) {
            // 当前是辅导员，下一个是班主任
            nextJobRole = Constants.JOB_ROLE_CLASS_TEACHER;
        } else {
            logger.error("当前审核人角色不合法: {}", currentJobRole);
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        return nextJobRole;
    }

    /**
     * 获取下一个审核人的信息
     * 
     * @param student        学生信息
     * @param currentJobRole 当前审核人的角色
     * @return 下一个审核人
     */
    private SysUser getNextAuditor(Student student, String currentJobRole) {
        // 确定下一个审核人的职位角色
        String nextJobRole = getJobRoleFromNextAuditor(currentJobRole);

        // 获取SysUserClass列表
        List<SysUserClass> sysUserClassList = sysUserClassService.getSysUserClassListByClassName(student.getClassName());

        // 查找对应角色的班级管理员
        SysUserClass targetSysUserClass = sysUserClassList.stream()
                .filter(sysUserClass -> nextJobRole.equalsIgnoreCase(sysUserClass.getJobRole()))
                .findFirst()
                .orElseThrow(() -> {
                    String roleName = nextJobRole.equals(Constants.JOB_ROLE_COUNSELOR) ? "辅导员" : "班主任";
                    logger.error("班级 {} 没有{}，请先添加{}", student.getClassName(), roleName, roleName);
                    return new BusinessException(ResultCode.VALIDATE_FAILED);
                });

        // 获取对应的班级管理员的详细信息
        SysUserQuery sysUserQuery = new SysUserQuery();
        sysUserQuery.setSysUserNo(targetSysUserClass.getSysUserNo());
        List<SysUser> sysUserList = sysUserService.selectByCondition(sysUserQuery);

        if (sysUserList.isEmpty()) {
            logger.error("班级 {} 的 {} 信息不存在，请先添加", student.getClassName(),
                    targetSysUserClass.getJobRole().equalsIgnoreCase(Constants.JOB_ROLE_COUNSELOR) ? "辅导员" : "班主任");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        SysUser nextAuditor = sysUserList.get(0);

        // 验证邮箱
        if (StringUtils.isBlank(nextAuditor.getEmail())) {
            String roleName = nextJobRole.equals(Constants.JOB_ROLE_COUNSELOR) ? "辅导员" : "班主任";
            logger.error("{} {} 没有邮箱，请先添加邮箱", roleName, targetSysUserClass.getSysUserNo());
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        return nextAuditor;

    }

    /**
     * 创建并保存通知记录
     * 
     * @param student     学生信息
     * @param nextAuditor 下一个审核人
     * @return 创建的通知记录
     */
    private Notification createAndSaveNotification(Student student, SysUser nextAuditor) {
        // 创建通知记录
        Notification notification = new Notification();
        notification.setNoticeId(NoticeIdGenerator.generate());
        notification.setTitle(Constants.LATE_RETURN_AUDIT_TITLE);
        notification.setNoticeType(Constants.NOTIFICATION_TYPE_AUDIT);
        notification.setContent(buildNotificationContent(student));
        notification.setTargetId(nextAuditor.getSysUserNo());
        notification.setStatus(Constants.UNREAD);
        notification.setCreateTime(new Date());
        notification.setUpdateTime(new Date());
        notification.setTargetType(nextAuditor.getJobRole());

        int affectedRow = notificationService.insert(notification);
        if (affectedRow <= 0) {
            logger.error("插入通知记录失败");
            throw new BusinessException(ResultCode.ERROR);
        }

        return notification;
    }

    /**
     * 构建通知内容
     * 
     * @param student 学生信息
     * @return 通知内容
     */
    private String buildNotificationContent(Student student) {
        return String.format("您有一条待审核的晚归情况说明,学生信息 [姓名-> %s, 学号-> %s]， 请在%d天之内完成审核",
                student.getName(), student.getStudentNo(), Constants.LATE_RETURN_AUDIT_DAYS);
    }

    /**
     * 验证审核权限
     * 
     * @param nextAuditor 下一个审核人
     * @param student     学生信息
     * @param auditPerson 当前审核人的工号
     */
    private void validateAuditPermission(SysUser nextAuditor, Student student, String auditPerson) {
        if (!sysUserClassService.hasClassPermission(
                nextAuditor.getSysUserNo(), student.getClassName())) {
            logger.error("审核人工号 {} 没有管理班级 {} 的权限，请先更新班级管理信息",
                    auditPerson, student.getClassName());
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    /**
     * 发送邮件通知
     * 
     * @param email        审核人的邮件
     * @param notification 通知
     * @param student      学生
     */
    private void sendEmailNotification(String email, Notification notification, Student student) {
        try {
            // 开发环境下使用该邮箱做测试
            email = Constants.EMAIL_DEV_RECEIVER;
            emailService.send(email, notification.getTitle(),
                    buildNotificationContent(student), false);
        } catch (Exception e) {
            // 尝试重新发送
            retryEmailSend(email, notification, student);
        }
    }

    /**
     * 尝试重发邮件
     * 
     * @param email        目标邮箱地址
     * @param notification 通知
     * @param student      学生
     */
    private void retryEmailSend(String email, Notification notification, Student student) {
        int maxRetries = Constants.EMAIL_MAX_RETRIES;
        int retryCount = 0;
        while (retryCount < maxRetries) {
            try {
                emailService.send(email, notification.getTitle(),
                        buildNotificationContent(student), false);
            } catch (Exception e) {
                retryCount++;
                if (retryCount == maxRetries) {
                    logger.error("邮件重试发送失败，已达到最大重试次数");
                }
            }
        }
    }
}
