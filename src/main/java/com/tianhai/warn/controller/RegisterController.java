package com.tianhai.warn.controller;

import com.google.code.kaptcha.Producer;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.RegisterDTO;
import com.tianhai.warn.enums.BusinessType;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.factory.UserFactory;
import com.tianhai.warn.service.RegisterService;
import com.tianhai.warn.service.VerificationService;
import com.tianhai.warn.utils.Result;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/register")
@Validated
public class RegisterController {
    private static final Logger logger = LoggerFactory.getLogger(RegisterController.class);

    @Autowired
    private RegisterService registerService;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private Producer captchaProducer;

    @Autowired
    private UserFactory userFactory;

    // 返回注册页面的请求
    @GetMapping
    public String registerPage() {
        return "register"; // 返回JSP视图
    }

    // 获取图形验证码
    @GetMapping("/captcha")
    @ResponseBody
    public void getCaptcha(HttpServletResponse response, HttpSession session) {
        try {
            // 设置响应类型
            response.setContentType("image/jpeg");
            // 禁止图像缓存
            response.setHeader("Cache-Control", "no-store, no-cache");
            response.setHeader("Pragma", "no-cache");
            response.setDateHeader("Expires", 0);

            // 生成验证码文本
            Result<String> captchaResult = verificationService.generateSessionCaptcha(session.getId(),
                    BusinessType.REGISTER);
            if (!captchaResult.isSuccess()) {
                throw new SystemException("验证码生成失败");
            }
            String capText = captchaResult.getData();

            // 创建验证码图片
            BufferedImage bi = captchaProducer.createImage(capText);

            // 将图片写入响应流
            ImageIO.write(bi, "jpg", response.getOutputStream());

            // 强制刷新缓冲区
            response.getOutputStream().flush();

        } catch (IOException e) {
            logger.error("验证码生成失败", e);
            throw new SystemException(ResultCode.CAPTCHA_GENERATE_ERROR);
        } finally {
            try {
                response.getOutputStream().close();
            } catch (IOException e) {
                logger.error("关闭响应流失败", e);
            }
        }
    }



    // 发送邮箱验证码
    @PostMapping("/send-email-code")
    @ResponseBody
    public Result<?> sendEmailCaptcha(@RequestParam String email,
            @RequestParam String antispamCaptcha,
            HttpSession session) {
        // 1. 验证图形验证码
        Result<Boolean> validateResult = verificationService.validateImageCaptcha(
                session.getId(),
                antispamCaptcha,
                BusinessType.REGISTER);

        if (!validateResult.isSuccess() || !validateResult.getData()) {
            throw new BusinessException(ResultCode.CAPTCHA_VALIDATE_ERROR);
        }

        // 2. 调用服务层发送邮箱验证码
        return registerService.sendEmailCaptcha(email);
    }

    // 注册接口 - 原来的实现（注释掉）
    /*
     * @PostMapping("/do-register")
     * 
     * @ResponseBody
     * public Result<?> register(@Valid @ModelAttribute RegisterDTO registerDTO,
     * HttpSession session) {
     * // 1. 检查注册频率
     * Result<Boolean> limitResult =
     * verificationService.checkRegisterLimit(session.getId(),
     * BusinessType.REGISTER);
     * if (!limitResult.isSuccess()) {
     * return limitResult;
     * }
     * 
     * // 2. 验证邮箱验证码
     * Result<Boolean> emailResult = verificationService.validateEmailCaptcha(
     * registerDTO.getEmail(),
     * registerDTO.getEmailCaptcha(),
     * BusinessType.REGISTER);
     * 
     * if (!emailResult.isSuccess() || !emailResult.getData()) {
     * return Result.error("邮箱验证码错误");
     * }
     * 
     * // 3. 调用服务层处理注册
     * Result<?> registerResult = registerService.register(registerDTO);
     * 
     * // 4. 如果注册成功，清理验证码和限制
     * if (registerResult.isSuccess()) {
     * verificationService.cleanupRegistrationCodes(session.getId(),
     * registerDTO.getEmail(),
     * BusinessType.REGISTER);
     * }
     * 
     * return registerResult;
     * }
     */

    // 注册接口 - 新的实现
    @PostMapping("/do-register")
    public String register(@Valid @ModelAttribute RegisterDTO registerDTO,
            HttpSession session,
            Model model) {
        // 1. 检查注册频率
        Result<Boolean> limitResult = verificationService.checkRegisterLimit(session.getId(), BusinessType.REGISTER);
        if (!limitResult.isSuccess()) {
            model.addAttribute("error", limitResult.getMessage());
            return "register";
        }

        // 2. 验证邮箱验证码
        Result<Boolean> emailResult = verificationService.validateEmailCaptcha(
                registerDTO.getEmail(),
                registerDTO.getEmailCaptcha(),
                BusinessType.REGISTER);

        if (!emailResult.isSuccess() || !emailResult.getData()) {
            model.addAttribute("error", "邮箱验证码错误");
            return "register";
        }

        // 3. 调用服务层处理注册
        Map<String, Object> registerInfoMap;
        try {
             registerInfoMap = registerService.register(registerDTO);
        } catch (Exception e) {
            logger.error("注册失败，跳转回注册页面");
            return "register";
        }

        // 4. 如果注册成功，清理验证码和限制
        verificationService.cleanupRegistrationCodes(session.getId(), registerDTO.getEmail(),
                BusinessType.REGISTER);

        // 5. 根据角色跳转到不同的页面
        Object user = userFactory.createUser(registerDTO);
        //这里的role 是student , systemuser, dormitorymanager, superadmin
        String roleCode = userFactory.getRoleCode(registerDTO.getRole());
        String pagePath = userFactory.getPagePath(registerDTO.getRole());

        session.setAttribute(Constants.SESSION_ATTRIBUTE_USER, user);
        session.setAttribute(Constants.SESSION_ATTRIBUTE_ROLE, roleCode);

        // 这里进一步根据班级管理员的角色进行页面跳转
        String jobRole = (String) registerInfoMap.get("jobRole");
        if (jobRole != null) {
            pagePath = jobRole;
            session.setAttribute(Constants.SESSION_ATTRIBUTE_JOB_ROLE, jobRole);
        }

        return "redirect:/" + pagePath;

    }
}
