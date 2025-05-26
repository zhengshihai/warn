package com.tianhai.warn.service.impl;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EmailServiceImpl implements EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private static final String sender = Constants.EMAIL_DEV_SENDER;

    @Autowired
    private JavaMailSender mailSender;


    @Override
    public boolean sendCaptcha(String targetEmail, String captcha, boolean isHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(sender);
            helper.setTo(targetEmail);
            helper.setSubject("注册验证码");
            String content = "您的验证码是：" + captcha + "，15分钟内有效";
            helper.setText(content, isHtml);

            mailSender.send(message);
            logger.info("验证码邮件发送成功，接收邮箱：{}", targetEmail);

            return true;
        } catch (Exception e) {
            logger.error("验证码邮件发送失败，接收邮箱：{}，错误信息：{}",
                    targetEmail, e.getMessage(), e);
            throw new SystemException(ResultCode.EMAIL_SEND_FAIL);
        }
    }

    @Override
    public boolean send(String targetEmail, String subject, String content, boolean isHtml) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(sender);
            helper.setTo(targetEmail);
            helper.setSubject(subject);
            helper.setText(content, isHtml);

            mailSender.send(message);
            logger.info("邮件发送成功，接收邮箱：{}", targetEmail);

            return true;
        } catch (Exception e) {
            logger.error("邮件发送失败，接收邮箱：{}，错误信息：{}", targetEmail, e.getMessage(), e);
            throw new SystemException(ResultCode.EMAIL_SEND_FAIL);
        }
    }

    @Override
    public Map<String, String> sendBatchDifferentContent(Map<String, String> emailContentMap, boolean isHtml) {
        Map<String, String> failedMap = new HashMap<>();

        for (Map.Entry<String, String> entry : emailContentMap.entrySet()) {
            String targetEmail = entry.getKey();
            String content = entry.getValue();

            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(sender);
                helper.setTo(targetEmail);
                helper.setSubject("批量邮件");
                helper.setText(content, isHtml);

                mailSender.send(message);

            } catch (Exception e) {
                logger.error("批量邮件发送失败，接收邮箱：{}，错误信息：{}", targetEmail, e.getMessage(), e);
                failedMap.put(targetEmail, content);
            }
        }

        return failedMap;
    }

    @Override
    public List<String> sendBatchSameContent(List<String> targetEmails,
            String subject,
            String content,
            boolean isHtml) {
        List<String> failedEmails = new ArrayList<>();
        for (String targetEmail : targetEmails) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setFrom(sender);
                helper.setTo(targetEmail);
                helper.setSubject(subject);
                helper.setText(content, isHtml);

                mailSender.send(message);

            } catch (Exception e) {
                logger.error("批量邮件发送失败，接收邮箱：{}，错误信息：{}", targetEmail, e.getMessage(), e);
                failedEmails.add(targetEmail);
            }
        }

        return failedEmails;
    }
}
