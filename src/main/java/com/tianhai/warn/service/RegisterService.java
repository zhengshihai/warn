package com.tianhai.warn.service;

import com.tianhai.warn.dto.RegisterDTO;
import com.tianhai.warn.utils.Result;

import java.util.Map;

public interface RegisterService {
    /**
     * 处理用户注册
     * 
     * @param registerDTO 注册信息
     * @return 注册结果
     */
    Map<String, Object> register(RegisterDTO registerDTO);

    /**
     * 发送邮箱验证码
     * 
     * @param email 邮箱地址
     * @return 发送结果
     */
    Result<?> sendEmailCaptcha(String email);
}