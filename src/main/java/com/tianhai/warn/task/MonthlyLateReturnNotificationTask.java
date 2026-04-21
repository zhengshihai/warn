package com.tianhai.warn.task;

import com.tianhai.warn.model.SystemRule;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.service.EmailService;
import com.tianhai.warn.service.LateReturnService;
import com.tianhai.warn.service.StudentService;
import com.tianhai.warn.service.SystemRuleService;
import com.tianhai.warn.utils.RedisLockUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * 每月晚归次数预警通知定时任务
 * 每月1号凌晨1点执行，统计上个月每个学生的晚归次数
 * 如果超过阈值，则发送邮件通知
 */
@Component
public class MonthlyLateReturnNotificationTask {

    private static final Logger logger = LoggerFactory.getLogger(MonthlyLateReturnNotificationTask.class);

    private static final String RULE_KEY = "LARGE_TIMES_LATERETURN_MONTH";
    private static final String LOCK_KEY = "monthlyLateReturnNotification:lock";

    @Autowired
    private SystemRuleService systemRuleService;

    @Autowired
    private LateReturnService lateReturnService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RedisLockUtils redisLockUtils;

    /**
     * 每月1号凌晨1点执行
     * 统计上个月的晚归数据
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkAndNotifyExcessiveLateReturns() {
        String taskId = java.util.UUID.randomUUID().toString();
        boolean locked = false;

        try {
            logger.info("开始执行定时任务：每月晚归次数预警通知，taskId: {}", taskId);

            // 获取分布式锁，锁定5分钟
            locked = redisLockUtils.tryLock(LOCK_KEY, taskId, 300);
            if (!locked) {
                logger.warn("每月晚归次数预警通知任务正在执行中，跳过本次执行");
                return;
            }

            // 获取上个月的时间范围
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            Date startTime = calendar.getTime();

            // 上个月最后一天 23:59:59
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
            calendar.set(Calendar.HOUR_OF_DAY, 23);
            calendar.set(Calendar.MINUTE, 59);
            calendar.set(Calendar.SECOND, 59);
            calendar.set(Calendar.MILLISECOND, 999);
            Date endTime = calendar.getTime();

            logger.info("统计时间范围：{} 至 {}", startTime, endTime);

            // 获取阈值
            SystemRule rule = systemRuleService.selectByRuleKey(RULE_KEY);
            if (rule == null || StringUtils.isBlank(rule.getRuleValue())) {
                logger.warn("未找到规则 {} 或规则值为空，跳过本次执行", RULE_KEY);
                return;
            }

            int threshold;
            try {
                threshold = Integer.parseInt(rule.getRuleValue().trim());
            } catch (NumberFormatException e) {
                logger.error("规则值格式错误，无法转换为整数：{}", rule.getRuleValue(), e);
                return;
            }

            if (threshold <= 0) {
                logger.warn("阈值必须大于0，当前值：{}，跳过本次执行", threshold);
                return;
            }

            logger.info("晚归次数阈值：{}", threshold);

            // 统计上个月每个学生的晚归次数
            Map<String, Integer> lateReturnCountMap =
                    lateReturnService.countLateReturnsByStudentNoInPeriod(startTime, endTime);

            if (lateReturnCountMap == null || lateReturnCountMap.isEmpty()) {
                logger.info("上个月没有晚归记录，任务结束");
                return;
            }

            logger.info("共统计到 {} 个学生有晚归记录", lateReturnCountMap.size());

            // 遍历统计结果，发送邮件通知
            int notifiedCount = 0;
            int failedCount = 0;

            for (Map.Entry<String, Integer> entry : lateReturnCountMap.entrySet()) {
                String studentNo = entry.getKey();
                Integer lateCount = entry.getValue();

                // 如果晚归次数超过阈值，发送邮件
                if (lateCount > threshold) {
                    try {
                        // 查询学生信息
                        Student student = studentService.selectByStudentNo(studentNo);
                        if (student == null) {
                            logger.warn("未找到学号为 {} 的学生信息，跳过邮件发送", studentNo);
                            failedCount++;
                            continue;
                        }

                        String email = student.getEmail();
                        if (StringUtils.isBlank(email)) {
                            logger.warn("学号为 {} 的学生邮箱为空，跳过邮件发送", studentNo);
                            failedCount++;
                            continue;
                        }

                        // 构建邮件内容
                        String subject = "晚归次数预警通知";
                        String content = buildEmailContent(
                                student.getName(), studentNo, lateCount,
                                threshold, startTime, endTime);

                        // 发送邮件
                        boolean success = emailService.send(email, subject, content, false);
                        if (success) {
                            logger.info("成功发送晚归预警邮件给学号：{}，邮箱：{}，晚归次数：{}", studentNo, email, lateCount);
                            notifiedCount++;
                        } else {
                            logger.error("发送晚归预警邮件失败，学号：{}，邮箱：{}", studentNo, email);
                            failedCount++;
                        }
                    } catch (Exception e) {
                        logger.error("处理学号 {} 的晚归预警通知时发生异常", studentNo, e);
                        failedCount++;
                    }
                }
            }

            logger.info("定时任务执行完成，taskId: {}，成功通知：{} 人，失败：{} 人", taskId, notifiedCount, failedCount);

        } catch (Exception e) {
            logger.error("定时任务执行失败，taskId: {}", taskId, e);
        } finally {
            // 释放分布式锁
            if (locked) {
                redisLockUtils.releaseLock(LOCK_KEY, taskId);
                logger.info("释放分布式锁，taskId: {}", taskId);
            }
        }
    }

    /**
     * 构建邮件内容
     * 
     * @param studentName 学生姓名
     * @param studentNo   学号
     * @param lateCount   晚归次数
     * @param threshold   阈值
     * @param startTime   统计开始时间
     * @param endTime     统计结束时间
     * @return 邮件内容
     */
    private String buildEmailContent(String studentName, String studentNo, int lateCount, int threshold, Date startTime,
            Date endTime) {
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("yyyy年MM月");
        String monthStr = dateFormat.format(startTime);

        StringBuilder content = new StringBuilder();
        content.append("尊敬的学生 ").append(studentName).append("（学号：").append(studentNo).append("），您好！\n\n");
        content.append("根据系统统计，您在 ").append(monthStr).append(" 的晚归次数为 ").append(lateCount).append(" 次，");
        content.append("超过了系统设定的阈值（").append(threshold).append(" 次）。\n\n");
        content.append("请您注意合理安排时间，遵守学校作息规定。如有特殊情况，请及时向宿管或辅导员说明。\n\n");
        content.append("感谢您的配合！\n\n");
        content.append("学生晚归预警系统");

        return content.toString();
    }
}
