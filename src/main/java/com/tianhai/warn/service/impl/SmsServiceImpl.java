package com.tianhai.warn.service.impl;

import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.enums.AlarmLevel;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.model.SysUserClass;
import com.tianhai.warn.service.SmsService;
import com.tianhai.warn.service.StudentService;
import com.tianhai.warn.service.SysUserClassService;
import com.tianhai.warn.service.SysUserService;
import com.tianhai.warn.utils.ProfileUtils;
import com.tianhai.warn.utils.RedisUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class SmsServiceImpl implements SmsService {

    private static final Logger logger = LoggerFactory.getLogger(SmsServiceImpl.class);

    @Autowired
    private StudentService studentService;

    @Autowired
    private SysUserClassService sysUserClassService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private SmsClient smsClient;

    @Value("${tencent.sms.sdk-app-id}")
    private String sdkAppId;

    @Value("${tencent.sms.sign-name}")
    private String signName;

    @Value("${tencent.sms.template-id}")
    private String templateId;

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
        if (ProfileUtils.isProfileActive("dev")) {
            // 开发环境模拟发送短信
            logger.info("开发环境模拟发送短信, phone: {}, content: {}", phone, content);
            return true;
        } else {
            return doSendMessage(phone, content);
        }
    }

    private boolean doSendMessage(String phone, String content) {
        try {
            SendSmsRequest req = new SendSmsRequest();
            req.setSmsSdkAppId(sdkAppId);
            req.setSignName(signName);
            req.setTemplateId(templateId);
            req.setPhoneNumberSet(new String[] { "+86" + phone });
            req.setTemplateParamSet(new String[] { content });

            SendSmsResponse resp = smsClient.SendSms(req);
            if (resp.getSendStatusSet()[0].getCode().equals("Ok")) {
                logger.info("短信发送成功: {}", phone);
                return true;
            } else {
                logger.error("短信发送失败: {}, 错误码: {}, 错误信息: {}",
                        phone,
                        resp.getSendStatusSet()[0].getCode(),
                        resp.getSendStatusSet()[0].getMessage());
                return false;
            }
        } catch (TencentCloudSDKException e) {
            logger.error("短信发送异常: {}, 错误: {}", phone, e.getMessage(), e);
            return false;
        }
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

    @Override
    public void sendTriggerOneClickAlarmSms(String studentNo, AlarmLevel alarmLevel, Date alarmTime) {
        // 获取学生信息
        Student student = studentService.selectByStudentNo(studentNo);
        if (student == null) {
            logger.error("学生不存在: {}", studentNo);
            return;
        }

        // 异步获取联系人信息
        CompletableFuture<List<String>> parentPhonesFuture = CompletableFuture
                .supplyAsync(() -> getParentPhones(student));
        CompletableFuture<List<String>> managerPhonesFuture = CompletableFuture
                .supplyAsync(() -> getManagerPhones(student));

        // 等待两个异步任务完成
        CompletableFuture.allOf(parentPhonesFuture, managerPhonesFuture).join();

        try {
            List<String> parentPhones = parentPhonesFuture.get();
            List<String> managerPhones = managerPhonesFuture.get();

            // 缓存联系人信息
            redisUtils.setStudentContacts(studentNo, parentPhones, managerPhones);

            // 根据报警级别发送短信
            if (alarmLevel == AlarmLevel.NORMAL) {
                sendNormalAlarmSms(student, parentPhones, managerPhones);
            } else if (alarmLevel == AlarmLevel.CRITICAL) {
                sendCriticalAlarmSms(student, parentPhones, managerPhones);
            }
        } catch (Exception e) {
            logger.error("发送报警短信失败: {}", e.getMessage(), e);
        }
    }

    @Async("asyncTaskExecutor")
    protected List<String> getParentPhones(Student student) {
        List<String> parentPhoneList = new ArrayList<>();
        if (StringUtils.isNotBlank(student.getMotherPhone())) {
            parentPhoneList.add(student.getMotherPhone());
        }
        if (StringUtils.isNotBlank(student.getFatherPhone())) {
            parentPhoneList.add(student.getFatherPhone());
        }

        return parentPhoneList;
    }

    @Async("asyncTaskExecutor")
    protected List<String> getManagerPhones(Student student) {
        List<String> managerPhoneList = new ArrayList<>();

        List<SysUserClass> sysUserClassList = sysUserClassService
                .getSysUserClassListByClassName(student.getClassName());
        List<String> sysUserNoList = new ArrayList<>();
        for (SysUserClass sysUserClass : sysUserClassList) {
            sysUserNoList.add(sysUserClass.getSysUserNo());
        }

        List<SysUser> classManagerList = sysUserService.selectBySysUserNos(sysUserNoList);
        for (SysUser sysUser : classManagerList) {
            if (StringUtils.isNotBlank(sysUser.getPhone())) {
                managerPhoneList.add(sysUser.getPhone());
            }
        }
        return managerPhoneList;
    }

    private void sendNormalAlarmSms(Student student, List<String> parentPhones, List<String> managerPhones) {
        String parentContent = String.format(AlarmConstants.ALARM_TRIGGER_SMS_PARENT_TEMPLATE,
                student.getName(), AlarmLevel.NORMAL.getDesc(), new Date());
        String managerContent = String.format(AlarmConstants.ALARM_TRIGGER_SMS_CLASS_MANAGER_TEMPLATE,
                student.getStudentNo(), AlarmLevel.NORMAL.getDesc(), new Date());

        // 发送第一条短信
        sendSmsAsync(parentPhones, parentContent);
        sendSmsAsync(managerPhones, managerContent);

        // 1分钟后发送第二条短信
        CompletableFuture.delayedExecutor(1, TimeUnit.MINUTES).execute(() -> {
            sendSmsAsync(parentPhones, parentContent);
            sendSmsAsync(managerPhones, managerContent);
        });

        // 2分钟后发送第三条短信
        CompletableFuture.delayedExecutor(2, TimeUnit.MINUTES).execute(() -> {
            sendSmsAsync(parentPhones, parentContent);
            sendSmsAsync(managerPhones, managerContent);
        });
    }

    private void sendCriticalAlarmSms(Student student, List<String> parentPhones, List<String> managerPhones) {
        String parentContent = String.format(AlarmConstants.ALARM_TRIGGER_SMS_PARENT_TEMPLATE,
                student.getName(), AlarmLevel.CRITICAL.getDesc(), new Date());
        String managerContent = String.format(AlarmConstants.ALARM_TRIGGER_SMS_CLASS_MANAGER_TEMPLATE,
                student.getStudentNo(), AlarmLevel.CRITICAL.getDesc(), new Date());

        // 发送前三条短信（每分钟一条）
        for (int i = 0; i < 3; i++) {
            final int delay = i;
            CompletableFuture.delayedExecutor(delay, TimeUnit.MINUTES).execute(() -> {
                sendSmsAsync(parentPhones, parentContent);
                sendSmsAsync(managerPhones, managerContent);
            });
        }

        // 发送后两条短信（每两分钟一条）
        for (int i = 3; i < 5; i++) {
            final int delay = i * 2;
            CompletableFuture.delayedExecutor(delay, TimeUnit.MINUTES).execute(() -> {
                sendSmsAsync(parentPhones, parentContent);
                sendSmsAsync(managerPhones, managerContent);
            });
        }
    }

    @Async("asyncTaskExecutor")
    protected void sendSmsAsync(List<String> phones, String content) {
        for (String phone : phones) {
            try {
                sendMessage(phone, content);
                logger.info("短信发送成功: {}", phone);
            } catch (Exception e) {
                logger.error("短信发送失败: {}, 错误: {}", phone, e.getMessage());
            }
        }
    }

    @Override
    public void sendCancelOneClickAlarmSms(String studentNo, String name, Date date) {
        RedisUtils.StudentContacts studentContacts = redisUtils.getStudentContacts(studentNo);
        boolean contactDisable = studentContacts == null
             || (studentContacts.getManagerPhones() == null && studentContacts.getParentPhones() == null);
        if (contactDisable) {
            logger.warn("找不到该该学生对应的班级管理员和家长的手机号码, studentNo: {}, name:{}", studentNo, name);
        } else {
            String parentSmsContent = String.format(
                    AlarmConstants.ALARM_CANCEL_SMS_PARENT_TEMPLATE,
                    name, date);
            sendSmsSync(studentContacts.getParentPhones(), parentSmsContent);

            String classManagerSmsContent = String.format(
                    AlarmConstants.ALARM_CANCEL_SMS_CLASS_MANAGER_TEMPLATE,
                    studentNo, name, date);
            sendSmsSync(studentContacts.getManagerPhones(), classManagerSmsContent);
        }
    }

    private void sendSmsSync(List<String> phones, String content) {
        for (String phone : phones) {
            try {
                sendMessage(phone, content);
                logger.info("短信发送成功: {}", phone);
            } catch (Exception e) {
                logger.error("短信发送失败: {}, 错误: {}", phone, e.getMessage());
            }
        }
    }

}
