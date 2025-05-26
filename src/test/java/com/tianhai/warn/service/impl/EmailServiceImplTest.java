package com.tianhai.warn.service.impl;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.service.EmailService;
import jakarta.mail.Message;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.Test; // JUnit 4
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.Assert.assertTrue; // JUnit 4


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext.xml", "classpath:spring/spring-mvc.xml" }) // 根据你的Spring配置文件路径调整
@WebAppConfiguration // 如果测试需要WebApplicationContext，比如测试Controller时，对于Service测试可能不是必须的
public class EmailServiceImplTest {

    @Autowired
    private EmailService emailService;
    @Autowired
    private JavaMailSender mailSender;

    @Test
    public void testSendCaptcha() {
        String targetEmail = "zhengzsh001@2925.com";
        boolean isHtml = false;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(Constants.EMAIL_DEV_SENDER);
            helper.setTo(targetEmail);
            helper.setSubject("注册验证码");
            String content = "您的验证码是：" + 123456 + "，15分钟内有效";
            helper.setText(content, isHtml);

            mailSender.send(message);
        } catch (Exception e) {
            throw new SystemException(ResultCode.EMAIL_SEND_FAIL);
        }
    }
}