package com.tianhai.warn.service;

import com.tianhai.warn.enums.BusinessType;
import com.tianhai.warn.utils.Result;

public interface VerificationService {
    /**
     * 生成图形验证码
     * 
     * @param sessionId    会话ID
     * @param businessType 业务类型
     * @return 验证码
     */
    Result<String> generateSessionCaptcha(String sessionId, BusinessType businessType);

    /**
     * 生成邮箱验证码
     * 
     * @param email        邮箱地址
     * @param businessType 业务类型
     * @return 验证码
     */
    Result<String> generateEmailCaptcha(String email, BusinessType businessType);

    /**
     * 验证图形验证码
     * 
     * @param sessionId    会话ID
     * @param captcha      用户输入的验证码
     * @param businessType 业务类型
     * @return 验证结果
     */
    Result<Boolean> validateImageCaptcha(String sessionId, String captcha, BusinessType businessType);

    /**
     * 验证邮箱验证码
     * 
     * @param email        邮箱地址
     * @param captcha      用户输入的验证码
     * @param businessType 业务类型
     * @return 验证结果
     */
    Result<Boolean> validateEmailCaptcha(String email, String captcha, BusinessType businessType);

    Result<Boolean> checkRegisterLimit(String sessionId, BusinessType businessType);

    void cleanupRegistrationCodes(String sessionId, String email, BusinessType businessType);
}
