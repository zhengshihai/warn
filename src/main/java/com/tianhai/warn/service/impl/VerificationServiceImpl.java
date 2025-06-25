package com.tianhai.warn.service.impl;

import com.tianhai.warn.enums.BusinessType;
import com.tianhai.warn.service.VerificationService;
import com.tianhai.warn.utils.CaptchaUtils;
import com.tianhai.warn.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class VerificationServiceImpl implements VerificationService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ITEM = "warn:";
    private static final String CAPTCHA_PREFIX = ITEM + "captcha:";
    private static final String EMAIL_PREFIX = ITEM + "email:";
    private static final long CAPTCHA_EXPIRE = 10; // 10分钟
    private static final long EMAIL_EXPIRE = 15; // 15分钟
    private static final long LIMIT_EXPIRE = 60; // 60秒

    /**
     * 生成图形验证码
     * 
     * @param sessionId    会话ID
     * @param businessType 业务类型
     * @return 验证码
     */
    @Override
    public Result<String> generateSessionCaptcha(String sessionId, BusinessType businessType) {
        if (sessionId == null || businessType == null) {
            return Result.error("参数不能为空");
        }

        String captcha = CaptchaUtils.generateCaptcha();
        String hashKey = String.format("%s%s:captcha", ITEM, businessType.getCode());

        // 使用Hash存储验证码
        redisTemplate.opsForHash().put(hashKey, sessionId, captcha);
        // 设置过期时间
        redisTemplate.expire(hashKey, CAPTCHA_EXPIRE, TimeUnit.MINUTES);

        return Result.success(captcha);
    }

    /**
     * 生成邮箱验证码
     * 
     * @param email        邮箱地址
     * @param businessType 业务类型
     * @return 验证码
     */
    @Override
    public Result<String> generateEmailCaptcha(String email, BusinessType businessType) {
        if (email == null || businessType == null) {
            return Result.error("参数不能为空");
        }

        String captcha = CaptchaUtils.generateCaptcha();
        String key = String.format("%s%s:email:%s", ITEM, businessType.getCode(), email);

        redisTemplate.opsForValue().set(key, captcha, EMAIL_EXPIRE, TimeUnit.MINUTES);
        return Result.success(captcha);
    }

    /**
     * 验证图形验证码
     * 
     * @param sessionId    会话ID
     * @param captcha      用户输入的验证码
     * @param businessType 业务类型
     * @return 验证结果
     */
    @Override
    public Result<Boolean> validateImageCaptcha(String sessionId, String captcha, BusinessType businessType) {
        if (sessionId == null || captcha == null || businessType == null) {
            return Result.error("参数不能为空");
        }

        String hashKey = String.format("%s%s:captcha", ITEM, businessType.getCode());
        String realCaptcha = (String) redisTemplate.opsForHash().get(hashKey, sessionId);

        if (realCaptcha == null) {
            return Result.error("验证码已过期");
        }

        boolean isValid = realCaptcha.equals(captcha);
        if (isValid) {
            // 验证成功后删除验证码
            redisTemplate.opsForHash().delete(hashKey, sessionId);
            return Result.success(true);
        }
        return Result.error("验证码错误");
    }

    /**
     * 验证邮箱验证码
     * 
     * @param email        邮箱地址
     * @param captcha      用户输入的验证码
     * @param businessType 业务类型
     * @return 验证结果
     */
    @Override
    public Result<Boolean> validateEmailCaptcha(String email, String captcha, BusinessType businessType) {
        if (email == null || captcha == null || businessType == null) {
            return Result.error("参数不能为空");
        }

        String key = String.format("%s%s:email:%s", ITEM, businessType.getCode(), email);
        String realCaptcha = (String) redisTemplate.opsForValue().get(key);

        if (realCaptcha == null) {
            return Result.error("验证码已过期");
        }

        boolean isValid = realCaptcha.equals(captcha);
        if (isValid) {
            // 验证成功后删除验证码
            redisTemplate.delete(key);
            return Result.success(true);
        }
        return Result.error("验证码错误");
    }

    /**
     * 检查注册频率限制
     * 限制同一会话在60秒内最多进行20次注册操作
     * @param sessionId 会话ID
     * @param businessType 业务类型
     * @return 检查结果
     */
    public Result<Boolean> checkRegisterLimit(String sessionId, BusinessType businessType) {
        // 获取频率限制的Redis hash key
        String hashKey = String.format("%s%s:limit", ITEM, businessType.getCode());
        String countStr = (String) redisTemplate.opsForHash().get(hashKey, sessionId);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        // 检查是否超过限制（20次）
        if (count >= 20) {
            return Result.error("操作过于频繁，请稍后再试");
        }

        // 更新访问计数
        if (count == 0) {
            // 第一次访问，设置计数器和过期时间
            redisTemplate.opsForHash().put(hashKey, sessionId, "1");
            redisTemplate.expire(hashKey, LIMIT_EXPIRE, TimeUnit.SECONDS);
        } else {
            // 增加计数
            redisTemplate.opsForHash().increment(hashKey, sessionId, 1);
        }

        return Result.success(true);
    }

    /**
     * 清理注册相关的所有数据
     * 包括图形验证码、邮箱验证码和频率限制记录
     * @param sessionId 会话ID
     * @param email 邮箱地址
     * @param businessType 业务类型
     */
    public void cleanupRegistrationCodes(String sessionId, String email, BusinessType businessType) {
        // 构建所有相关的Redis key
        String captchaHashKey = String.format("%s%s:captcha", ITEM, businessType.getCode());
        String limitHashKey = String.format("%s%s:limit", ITEM, businessType.getCode());
        String emailKey = String.format("%s%s:email:%s", ITEM, businessType.getCode(), email);

        // 清理所有数据
        redisTemplate.opsForHash().delete(captchaHashKey, sessionId);
        redisTemplate.opsForHash().delete(limitHashKey, sessionId);
        redisTemplate.delete(emailKey);
    }
}