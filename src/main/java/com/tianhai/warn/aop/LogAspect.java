package com.tianhai.warn.aop;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.UserRole;
import com.tianhai.warn.model.*;
import com.tianhai.warn.service.SystemLogService;
import com.tianhai.warn.utils.RoleObjectCaster;
import com.tianhai.warn.utils.SysLogIdGenerator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import static com.tianhai.warn.enums.UserRole.STUDENT;

@Aspect
@Component
public class LogAspect {
    private static final Logger logger = LoggerFactory.getLogger(LogAspect.class);

    private static final String SUCCESSFUL = "successful";
    private static final String FAILED = "failed";

    private static final String STUDENT = "student";
    private static final String DORMITORY_MANAGER = "dormitorymanager";
    private static final String SYSTEM_USER = "systemuser";
    private static final String SUPER_ADMIN = "superadmin";

    @Autowired
    private SystemLogService systemLogService;

    @Around("@annotation(logOperation)")
    public Object log(ProceedingJoinPoint joinPoint, LogOperation logOperation) throws Throwable {
        logger.debug("开始记录操作日志: {}", logOperation.value());

        HttpServletRequest request = ((ServletRequestAttributes) Objects
                .requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        // 使用@Builder构建SystemLog对象
        SystemLog.SystemLogBuilder logBuilder = SystemLog.builder()
                .logId(SysLogIdGenerator.generate())
                .operation(logOperation.value())
                .method(joinPoint.getSignature().toShortString())
                .params(Arrays.toString(joinPoint.getArgs()))
                .ip(request.getRemoteAddr());

        try {
            logger.debug("执行目标方法: {}", joinPoint.getSignature().toShortString());
            // 执行目标方法
            Object result = joinPoint.proceed();

            // 方法执行后获取session和用户信息
            HttpSession session = null;
            try {
                session = request.getSession(false);
                logger.debug("获取到session: {}", session != null ? "存在" : "不存在");
            } catch (Exception e) {
                logger.warn("获取session时发生错误: {}", e.getMessage());
            }

            Object user = null;
            String userRoleStr = null;

            if (session != null) {
                try {
                    user = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);
                    userRoleStr = (String) session.getAttribute(Constants.SESSION_ATTRIBUTE_ROLE);
                    logger.debug("从session中获取到用户信息 - 用户{}, 角色: {}",
                            user != null ? "存在" : "不存在",
                            userRoleStr != null ? userRoleStr : "不存在");
                } catch (Exception e) {
                    logger.warn("从session获取用户信息时发生错误: {}", e.getMessage());
                }
            }

            // 处理用户信息
            if (userRoleStr != null && user != null) {
                try {
                    switch (userRoleStr) {
                        case STUDENT:
                            Student student = RoleObjectCaster.cast(STUDENT, user);
                            logBuilder.userNo(student.getStudentNo())
                                    .username(student.getName())
                                    .userRole(userRoleStr);
                            break;

                        case DORMITORY_MANAGER:
                            DormitoryManager dormitoryManager = RoleObjectCaster.cast(DORMITORY_MANAGER, user);
                            logBuilder.userNo(dormitoryManager.getManagerId())
                                    .username(dormitoryManager.getName())
                                    .userRole(userRoleStr);
                            break;

                        case SYSTEM_USER:
                            SysUser systemUser = RoleObjectCaster.cast(SYSTEM_USER, user);
                            logBuilder.userNo(systemUser.getSysUserNo())
                                    .username(systemUser.getName())
                                    .userRole(userRoleStr);
                            break;

                        case SUPER_ADMIN:
                            SuperAdmin superAdmin = RoleObjectCaster.cast(SUPER_ADMIN, user);
                            logBuilder.userNo(String.valueOf(superAdmin.getId()))
                                    .username(superAdmin.getName())
                                    .userRole(userRoleStr);
                            break;

                        default:
                            logger.warn("未知的用户角色: {}", userRoleStr);
                            break;
                    }
                } catch (Exception e) {
                    logger.warn("处理用户信息时发生错误: {}", e.getMessage());
                }
            } else {
                logger.debug("未获取到用户信息，将记录匿名操作");
            }

            // 记录成功日志
            SystemLog log = logBuilder.status(SUCCESSFUL).build();
            try {
                int insertResult = systemLogService.insert(log);
                logger.debug("日志插入结果: {}", insertResult);
            } catch (Exception e) {
                logger.error("插入日志时发生错误: {}", e.getMessage());
            }
            return result;
        } catch (Exception e) {
            logger.error("记录日志时发生错误", e);
            // 记录失败日志
            SystemLog log = logBuilder.status(FAILED)
                    .errorMsg(e.getMessage())
                    .build();
            logger.debug("准备插入失败日志: {}", log);
            try {
                int insertResult = systemLogService.insert(log);
                logger.debug("失败日志插入结果: {}", insertResult);
            } catch (Exception ex) {
                logger.error("插入失败日志时发生错误: {}", ex.getMessage());
            }
            throw e;
        }
    }
}
