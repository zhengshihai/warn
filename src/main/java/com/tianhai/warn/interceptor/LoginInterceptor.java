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

    // 通过loginUUID 拦截校验
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