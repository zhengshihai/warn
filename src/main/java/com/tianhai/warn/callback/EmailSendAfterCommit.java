package com.tianhai.warn.callback;

import com.tianhai.warn.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.support.TransactionSynchronization;

/**
 * 回调：邮件发送
 */
@RequiredArgsConstructor
public class EmailSendAfterCommit implements TransactionSynchronization {

    private final EmailService emailService;

    private final String targetEmail;

    private final String emailSubject;

    private final String emailContent;

    @Override
    public void afterCommit() {
        emailService.send(targetEmail, emailSubject, emailContent, false);
    }
}
