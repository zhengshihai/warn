package com.tianhai.warn.service;

import com.tianhai.warn.utils.Result;
import reactor.util.annotation.Nullable;

import java.util.List;
import java.util.Map;

public interface EmailService {
    /**
     * 发送验证码邮件
     * 
     * @param to      接收邮箱
     * @param captcha 验证码
     * @param isHtml  是否为HTML格式
     * @return 发送结果
     */
     boolean sendCaptcha(String to, String captcha, boolean isHtml);

    /**
     * 发送邮件
     *
     * @param to      接收邮箱
     * @param object  邮件主题
     * @param content 邮件内容
     * @param isHtml  是否为HTML格式
     * @return 发送结果
     */
     boolean send(String to, String object, String content, boolean isHtml);

    /**
     * 批量相同内容的邮件
     * @param targetEmails
     * @param subject
     * @param content
     * @param isHtml
     * @return 没有发送成功的邮箱
     */
     List<String> sendBatchSameContent(List<String> targetEmails,
                                       String subject,
                                       String content,
                                       boolean isHtml);

    /**
     * 批量不同内容的邮件
     * @param emailContentMap
     * @param isHtml
     * @return 没有发送成功的邮箱以及邮件
     */
     Map<String, String> sendBatchDifferentContent(Map<String, String> emailContentMap,
                                                   boolean isHtml);
}
