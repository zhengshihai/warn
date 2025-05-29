package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.aop.LogAspect;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.service.AuthService;
import com.tianhai.warn.utils.Result;
import com.tianhai.warn.utils.SessionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.tags.shaded.org.apache.regexp.RE;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private LogAspect logAspect;


    @RequestMapping("/login")
    public String login(HttpServletRequest request) {
        return "login";
    }

    /**
     * 处理使用邮箱的登录请求
     */
    @LogOperation("用户登录")
    @PostMapping("/do-login") // zsh774538399@gmail.com ZSH774538399@gmail.com
    @ResponseBody
    public Result<Map<String, Object>> doLogin(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String role) {

        boolean validateFailed = StringUtils.isBlank(email)
                || StringUtils.isBlank(password)
                || StringUtils.isBlank(role);
        if (validateFailed) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        Map<String, Object> infoMap = authService.login(name, email, password, role);
        HttpSession session = SessionUtils.getSession(true);
        assert session != null;
        session.setAttribute(Constants.SESSION_ATTRIBUTE_USER, infoMap.get("user"));
        session.setAttribute(Constants.SESSION_ATTRIBUTE_ROLE, infoMap.get("role"));
        session.setAttribute(Constants.SESSION_ATTRIBUTE_JOB_ROLE, infoMap.get("jobRole"));

        return Result.success(infoMap);
    }

    /**
     * 处理退出登录请求
     */
    @LogOperation("用户退出")
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login";
    }
}

