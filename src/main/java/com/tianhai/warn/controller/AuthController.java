package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.aop.LogAspect;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.service.AuthService;
import com.tianhai.warn.utils.Result;
import com.tianhai.warn.utils.SessionUtils;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.Cookie;
import lombok.extern.flogger.Flogger;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.tags.shaded.org.apache.regexp.RE;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private LogAspect logAspect;



    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(AuthController.class);


    @RequestMapping("/login")
    public String login(HttpServletRequest request) {
        return "login";
    }

    /**
     * 处理使用邮箱的登录请求 todo 根据用户是否在职 进行不同的登录处理
     */
    @LogOperation("用户登录")
    @PostMapping("/do-login") // zsh774538399@gmail.com ZSH774538399@gmail.com
    @ResponseBody
    public Result<Map<String, Object>> doLogin(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String role,
            @RequestParam(required = false) boolean remember,
            HttpServletResponse response) {

        boolean validateFailed = StringUtils.isBlank(email) || StringUtils.isBlank(password) || StringUtils.isBlank(role);
        if (validateFailed) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        Map<String, Object> infoMap = authService.login(name, email, password, role);
        if (infoMap == null || infoMap.isEmpty()) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }

        HttpSession session = SessionUtils.getSession(true);
        assert session != null;
        session.setAttribute(Constants.SESSION_ATTRIBUTE_USER, infoMap.get("user"));
        session.setAttribute(Constants.SESSION_ATTRIBUTE_ROLE, infoMap.get("role"));
        session.setAttribute(Constants.SESSION_ATTRIBUTE_JOB_ROLE, infoMap.get("jobRole"));

        // 生成并存储loginUUID
        String loginUUID = UUID.randomUUID().toString();
        session.setAttribute(Constants.SESSION_ATTRIBUTE_LOGIN_UUID, loginUUID);
        infoMap.put(Constants.SESSION_ATTRIBUTE_LOGIN_UUID, loginUUID);

        // 使用JWT 实现 “记住我”逻辑
        Cookie rememberMeCookie = authService.handleRememberMe(infoMap, remember);

        // 如果 Service 返回了 Cookie，添加到响应中
        if (rememberMeCookie != null) {
            response.addCookie(rememberMeCookie);
        }

        return Result.success(infoMap);
    }

    /**
     * 处理退出登录请求
     */
    @LogOperation("用户登出")
    @GetMapping("/logout")
    @RequirePermission
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}

