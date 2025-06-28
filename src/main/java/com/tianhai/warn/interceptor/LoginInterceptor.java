package com.tianhai.warn.interceptor;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.service.DormitoryManagerService;
import com.tianhai.warn.service.StudentService;
import com.tianhai.warn.service.SuperAdminService;
import com.tianhai.warn.service.SysUserService;
import com.tianhai.warn.utils.RoleObjectCaster;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Calendar;
import java.util.Date;

/**
 * 登录拦截器
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoginInterceptor.class);

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private DormitoryManagerService dormitoryManagerService;

    @Autowired
    private SuperAdminService superAdminService;


    // 方案一 通过查询数据库进行拦截校验
//    @Override
//    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
//            jakarta.servlet.http.HttpServletResponse response,
//            Object handler) throws Exception {
//        // 获取请求的URL
//        String uri = request.getRequestURI();
//
//        // 登录页面和登录请求放行
//        if (uri.contains("/login") || uri.contains("/logout") ||
//                uri.contains("/css/") || uri.contains("/js/") ||
//                uri.contains("/images/")) {
//            return true;
//        }
//
//        // 获取Session中的用户信息
//        Object sessionUser = request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_USER);
//        if (sessionUser == null) {
//            response.sendRedirect(request.getContextPath() + "/login");
//            return false;
//        }
//
//        String sessionUserRole = (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_ROLE);
//
//
//        if (sessionUserRole.equalsIgnoreCase(Constants.SYSTEM_USER)) {
//            SysUser sessionSysUser = RoleObjectCaster.cast(Constants.SYSTEM_USER, sessionUser);
//            if (sessionSysUser.getId() == null) {
//                logger.info("session中的SysUser的id为空，可能是未登录或会话已过期");
//                response.sendRedirect(request.getContextPath() + "/login");
//                return false;
//            }
//
//            SysUser sysUserExisting = sysUserService.getSysUserById(sessionSysUser.getId());
//            if (sysUserExisting == null) {
//                logger.info("找不到该班级管理员");
//                response.sendRedirect(request.getContextPath() + "/login");
//                return false;
//            }
//            if (!sysUserExisting.getStatus().equalsIgnoreCase(Constants.ENABLE_STR)) {
//                logger.info("该班级管理员已被禁用");
//                response.sendRedirect(request.getContextPath() + "/login");
//                return false;
//            }
//
//            if (!sysUserExisting.getSysUserNo().equals(sessionSysUser.getSysUserNo())
//               || !sysUserExisting.getEmail().equals(sessionSysUser.getEmail())
//               || !sysUserExisting.getName().equals(sessionSysUser.getName())) {
//                logger.info("请求中的班级管理员信息无效");
//                response.sendRedirect(request.getContextPath() + "/login");
//                return false;
//            }
//
//
//            Date sysUserExistingDate = DateUtils.truncate(sysUserExisting.getCreateTime(), Calendar.SECOND);
//            Date sessionSysUserDate = DateUtils.truncate(sessionSysUser.getCreateTime(), Calendar.SECOND);
//            if (!sysUserExistingDate.equals(sessionSysUserDate)) {
//                logger.info("请求中的班级管理员信息无效");
//                response.sendRedirect(request.getContextPath() + "/login");
//                return false;
//            }
//        }
//
//        return true;
//
//    }

    // 方案二 通过loginUUID 拦截校验
    @Override
    public boolean preHandle(jakarta.servlet.http.HttpServletRequest request,
                             jakarta.servlet.http.HttpServletResponse response,
                             Object handler) throws Exception {
        // 获取请求的URL
        String uri = request.getRequestURI();
        // 登录页面和登录请求放行
        if (uri.contains("/login") || uri.contains("/logout") ||
                uri.contains("/css/") || uri.contains("/js/") ||
                uri.contains("/images/")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            logger.info("用户没登录或者session已过期，跳转到登录页");
            response.sendRedirect("/login");
            return false;
        }

        String sessionLoginUUID =
                (String) request.getSession().getAttribute(Constants.SESSION_ATTRIBUTE_LOGIN_UUID);
        String requestLoginUUID = request.getHeader("X-Login-UUID");
        if (sessionLoginUUID == null || !sessionLoginUUID.equals(requestLoginUUID)) {
            response.sendRedirect("/login");
            return false;
        }

        return true;

    }
}