package com.tianhai.warn.service.impl;

import com.tianhai.warn.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class SmsServiceImpl implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsServiceImpl.class);

    @Override
    public boolean sendVerificationCode(String phone, String code) {
        return false;
    }

    @Override
    public boolean verifyCode(String phone, String code) {
        return false;
    }

    @Override
    public boolean sendMessage(String phone, String content) {
        logger.info("模拟发送手机短信, phone: {}, content {}", phone, content);

        return true;
    }

    @Override
    public boolean sendTemplateMessage(String phone, String templateId, Map<String, String> params) {
        return false;
    }

    @Override
    public Map<String, Boolean> sendBatchMessage(List<String> phones, String content) {
        return Map.of();
    }

    @Override
    public String querySendStatus(String messageId) {
        return "";
    }
}
