package com.tianhai.warn.filter;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.service.*;
import com.tianhai.warn.utils.SessionUtils;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tianhai.warn.constants.Constants.*;

@Component //todo 无法生效
public class JwtRememberMeFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRememberMeFilter.class);

    @Value("${jwt.rememberMe.secret}") // 从配置文件读取密钥，提供默认值
    private String jwtSecret;

    // 不需要Filter处理的路径模式列表
    private final List<String> excludeUrlPatterns;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    private AuthService authService;

    public JwtRememberMeFilter() {
        excludeUrlPatterns = new ArrayList<>();
        excludeUrlPatterns.add("/login");
        excludeUrlPatterns.add("/logout");
        excludeUrlPatterns.add("/css/**");
        excludeUrlPatterns.add("/js/**");
        excludeUrlPatterns.add("/images/**");
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }

    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestURI = httpRequest.getRequestURI();

        //检查当前路径是否在排除列表中
        for (String pattern : excludeUrlPatterns) {
            if (pathMatcher.match(pattern, requestURI)) {
                // 如果匹配到排除的路径，直接放行
                filterChain.doFilter(request, response);
                return;
            }
        }

        // 检查当前 session 是否有用户信息
        HttpSession session = httpRequest.getSession(false);
        boolean isLoggedIn = session != null
                && session.getAttribute(Constants.SESSION_ATTRIBUTE_USER) != null;

        // 用户已登录 直接放行
        if (isLoggedIn) {
            filterChain.doFilter(request, response);
            return;
        }

        // session 无用户信息 检查 Cookie 中的 rememberMe
        String rememberMeJwt = getRememberMeCookie(httpRequest);

        if (StringUtils.hasText(rememberMeJwt)) {
            // --- 调用 AuthService 来查找用户 ---
            Map<String, Object> userInfo = authService.findUserByRememberMeToken(rememberMeJwt);

            if (userInfo != null) {
                // --- 成功找到用户，建立新的Session ---
                HttpSession newSession = httpRequest.getSession(true);
                newSession.setAttribute(SESSION_ATTRIBUTE_USER,
                        userInfo.get(Constants.SESSION_ATTRIBUTE_USER));
                newSession.setAttribute(SESSION_ATTRIBUTE_ROLE,
                        userInfo.get(Constants.SESSION_ATTRIBUTE_ROLE));
                newSession.setAttribute(SESSION_ATTRIBUTE_JOB_ROLE,
                        userInfo.get(Constants.SESSION_ATTRIBUTE_JOB_ROLE)); // 设置jobRole

                logger.debug("JWT 记住我自动登录成功 for user: {}", userInfo.get(Constants.SESSION_ATTRIBUTE_USER));

                filterChain.doFilter(request, response);
                return; // 处理完毕，直接返回

            } else {
                // Token无效或用户未找到，删除Cookie
                logger.debug("记住我Token无效或用户未找到，删除Cookie");
                removeRememberMeCookie(httpRequest, httpResponse);
            }
        }

        // 没有Session，没有有效的 rememberMe Cookie，继续Filter Chain
        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中获取 rememberMe Cookie
     * @param request   HttpServletRequest 请求对象
     * @return          rememberMe Cookie 的值
     */
    private String getRememberMeCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("rememberMe".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    /**
     * 删除 rememberMe Cookie
     * @param request   HttpServletRequest 请求对象
     * @param response  HttpServletResponse 响应对象
     */
    private void removeRememberMeCookie(HttpServletRequest request, HttpServletResponse response) {
        Cookie cookie = new Cookie("rememberMe", null);
        cookie.setMaxAge(0); // 设置过期时间为0
        cookie.setPath("/"); // 确保删除所有路径下的 Cookie
//        cookie.setSecure(request.isSecure()); // 如果是 HTTPS 请求，则设置 Secure 属性
        response.addCookie(cookie);
        logger.debug("已删除 rememberMe Cookie");
    }

}
