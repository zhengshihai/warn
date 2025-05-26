package com.tianhai.warn.aop;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.UserRole;
import com.tianhai.warn.model.*;
import com.tianhai.warn.service.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;

import static com.tianhai.warn.enums.UserRole.STUDENT;

@Aspect
@Component
public class LogAspect {

    private static final String SUCCESSFUL = "successful";
    private static final String FAILED = "failed";

    private static final String STUDENT = "student";
    private static final String DORMITORY_MANAGER = "dormitorymanager";
    private static final String SYSTEM_USER = "systemuser";
    private static final String SUPER_ADMIN = "superadmin";

    @Autowired
    private SystemLogService systemLogService;

    @Around("@annotation(logOperation)")
    public Object log(ProceedingJoinPoint joinPoint, LogOperation logOperation) throws Throwable{
        HttpServletRequest request =
                ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        HttpSession session = request.getSession(false);
        Object user = session != null
                ? session.getAttribute(Constants.SESSION_ATTRIBUTE_USER)
                : null;

        // 这里的用户角色分成三大类，学生 宿管 系统用户
        String userRoleStr = session != null
                ? (String) session.getAttribute(Constants.SESSION_ATTRIBUTE_ROLE)
                : null;

        // 使用@Builder构建SystemLog对象
        SystemLog.SystemLogBuilder logBuilder = SystemLog.builder()
                .operation(logOperation.value())
                .method(joinPoint.getSignature().toLongString())
                .params(Arrays.toString(joinPoint.getArgs()))
                .ip(request.getRemoteAddr())
                .createTime(new Date());

        // 处理三种用户类型
        if (userRoleStr != null) {
            switch (userRoleStr) {
                case STUDENT:
                    Student student = (Student) user;
                    logBuilder.userNo(student.getStudentNo())
                            .username(student.getName())
                            .userRole(userRoleStr);
                    break;

                case DORMITORY_MANAGER:
                    DormitoryManager dormitoryManager = (DormitoryManager) user;
                    logBuilder.userNo(dormitoryManager.getManagerId())
                            .username(dormitoryManager.getName())
                            .userRole(userRoleStr);
                    break;

                case SYSTEM_USER:
                    SysUser systemUser = (SysUser) user;
                    logBuilder.userNo(systemUser.getSysUserNo())
                            .username(systemUser.getName())
                            .userRole(userRoleStr);
                    break;

                case SUPER_ADMIN:
                    SuperAdmin superAdmin = (SuperAdmin) user;
                    logBuilder.userNo(String.valueOf(superAdmin.getId()))
                            .username(superAdmin.getName())
                            .userRole(userRoleStr);
                    break;

                default:
                    break;
            }
        }

        SystemLog log = SystemLog.builder().build();
        log.setOperation(logOperation.value());
        log.setMethod(joinPoint.getSignature().toShortString());
        log.setParams(java.util.Arrays.toString(joinPoint.getArgs()));
        log.setIp(request.getRemoteAddr());
        log.setCreateTime(new Date());

        try {
            Object result = joinPoint.proceed();
            log = logBuilder.status(SUCCESSFUL).build();
            systemLogService.insert(log);

            return result;
        } catch (Exception e) {
            log.setStatus(FAILED);
            log.setErrorMsg(e.getMessage());
            systemLogService.insert(log);

            throw e;
        }


    }

}
