package com.tianhai.warn.service;

import java.util.List;
import java.util.Map;

public interface SmsService {
    /**
     * 发送短信验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @return 是否发送成功
     */
    boolean sendVerificationCode(String phone, String code);

    /**
     * 验证短信验证码是否正确
     *
     * @param phone 手机号
     * @param code  用户提交的验证码
     * @return 是否验证成功
     */
    boolean verifyCode(String phone, String code);

    /**
     * 发送普通文本短信
     *
     * @param phone 手机号
     * @param content 短信内容（纯文本）
     * @return 是否发送成功
     */
    boolean sendMessage(String phone, String content);

    /**
     * 发送模板短信（用于通知、营销等）
     *
     * @param phone 手机号
     * @param templateId 短信模板ID
     * @param params 模板中的变量参数
     * @return 是否发送成功
     */
    boolean sendTemplateMessage(String phone, String templateId, Map<String, String> params);

    /**
     * 批量发送短信（通常用于通知或营销）
     *
     * @param phones 多个手机号
     * @param content 短信内容
     * @return 每个手机号的发送结果
     */
    Map<String, Boolean> sendBatchMessage(List<String> phones, String content);

    /**
     * 查询短信发送状态（部分短信平台支持）
     *
     * @param messageId 短信平台返回的消息ID
     * @return 当前发送状态（成功、失败、待发送等）
     */
    String querySendStatus(String messageId);
}
