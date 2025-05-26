package com.tianhai.warn.interceptor;

import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 登录拦截器
 */
public class LoginInterceptor implements HandlerInterceptor {

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

        // 获取Session中的用户信息
        Object user = request.getSession().getAttribute("user");

        // 如果用户已登录，放行
        if (user != null) {
            return true;
        }

        // 未登录，重定向到登录页面
        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}