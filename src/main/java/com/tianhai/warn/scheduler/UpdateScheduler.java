package com.tianhai.warn.scheduler;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.model.NotificationReceiver;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SuperAdmin;
import com.tianhai.warn.query.*;
import com.tianhai.warn.service.*;
import com.tianhai.warn.utils.ProfileUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class UpdateScheduler {
    private static final Logger logger = LoggerFactory.getLogger(UpdateScheduler.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StudentService studentService;

    @Autowired
    private AlarmRecordService alarmRecordService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ExplanationService explanationService;

    @Autowired
    private LateReturnService lateReturnService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StudentLateStatsService studentLateStatsService;

    @Autowired
    private NotificationReceiverService notificationReceiverService;

    @Autowired
    private SystemLogService systemLogService;

    @Autowired
    private SuperAdminService superAdminService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TaskScheduler taskScheduler;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private UpdateScheduler selfProxy; // 这里为了避免Spring事务自我调用失败，故采用自调用模式


    private static final String STUDENT_NO_UPDATE_QUEUE = "student:no:update:queue";

    /**
     * 安排更新学号在下一个凌晨3点执行

     * 使用这种方式更新学生学号，是因为超级管理员更新了学生学号，
     * 为了避免学生的日常使用，便将学号推迟到晚上更新，考虑到项目可能采用集群部署，
     * 所以就使用TaskScheduler + Redisson分布式锁来实现只有一个实例会执行该更新任务
     */
    public void scheduleStudentNoUpdate(String oldStudentNo, String newStudentNo) {
        String taskKey = "studentNoUpdate:scheduled:" + oldStudentNo + "->" + newStudentNo;
        Boolean alreadyScheduled = redisTemplate.opsForValue().setIfAbsent(taskKey, "1", 2, TimeUnit.DAYS);
        if (Boolean.FALSE.equals(alreadyScheduled)) {
            logger.info("该学号变更任务已调度过，跳过: {}", taskKey);
            return;
        }

        Runnable task = () -> doStudentNoUpdateWithLock(oldStudentNo, newStudentNo);

        boolean devOrTestProfile = Objects.requireNonNull(ProfileUtils.getFirstActiveProfile()).equalsIgnoreCase("dev")
                 || Objects.requireNonNull(ProfileUtils.getFirstActiveProfile()).equalsIgnoreCase("test");

        Trigger trigger = triggerContext -> {

            if (devOrTestProfile){
                // 开发或测试环境： 指定首次执行时间（5秒后）
                return new Date(System.currentTimeMillis() + 5000L).toInstant();
            } else {
                // 生产环境： 指定首次执行时间（下一个凌晨3点）
                return new Date(System.currentTimeMillis() + getDelayUntilNext3AM()).toInstant();
            }
        };

        taskScheduler.schedule(task, trigger);
    }

    /**
     * 计算距离下一个凌晨3点的毫秒数
     */
    private long getDelayUntilNext3AM() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next3AM = now.withHour(3).withMinute(0).withSecond(0).withNano(0);
        if (!now.isBefore(next3AM)) {
            next3AM = next3AM.plusDays(1);
            logger.warn("当前时间已经在当天凌晨3点之后，学号更新任务将推迟到下一个凌晨3点执行");
        }

        return Duration.between(now, next3AM).toMillis();
    }


    /**
     * 带分布式锁的学号变更逻辑
     */
    private void doStudentNoUpdateWithLock(String oldStudentNo, String newStudentNo) {
        logger.info("进入学号更新任务，oldNo={}, newNo={}, 线程={}, 时间={}",
                oldStudentNo,
                newStudentNo,
                Thread.currentThread().getName(),
                System.currentTimeMillis());

        String studentNoLockKey = "studentNoUpdate:" + oldStudentNo + "->" + newStudentNo;
        RLock studentNoLock = redissonClient.getLock(studentNoLockKey);
        boolean locked = false;
        int waitTimeSeconds = 5;  // 获取锁的过程最多等待5秒
        int lockTimeMinutes = 2; // 锁定学号的时间最多维持2分钟

        try {
            locked = studentNoLock.tryLock(waitTimeSeconds, lockTimeMinutes, TimeUnit.MINUTES);
            if (locked) {
                selfProxy.executeStudentNoUpdate(oldStudentNo, newStudentNo);
            } else {
                logger.info("有其他服务节点实例正在更新学号变更任务，当前实例跳过该任务，studentNoLockKey：{}", studentNoLockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("更新学号失败,", e);
            sendFailedEmailToSuperAdmins(oldStudentNo, newStudentNo);
        } finally {
            if (locked) studentNoLock.unlock();
        }
    }

    /** 发送更新结果短信给超级管理员
     *
     * @param oldStudentNo    旧学号
     * @param newStudentNo    新学号
     */
    private void sendFailedEmailToSuperAdmins(String oldStudentNo, String newStudentNo) {
        String content = String.format(
                "系统更新学生学号出错，请及时人工更新！ oldStudentNo: %s, newStudentNo: %s",
                oldStudentNo, newStudentNo);

        SuperAdmin superAdmin = new SuperAdmin();
        superAdmin.setEnabled(Constants.ENABLE_INT);
        List<SuperAdmin> superAdminList = superAdminService.selectByCondition(superAdmin);
        if (superAdminList.isEmpty()) {
            logger.warn("找不到已启用的超级管理员");
        } else {
            List<String> distinctEmails = superAdminList.stream()
                    .map(SuperAdmin::getEmail)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            for (String email : distinctEmails) {
                boolean sendSuccess = emailService.send(email, "学号更新", content, false);
                if (!sendSuccess) logger.error("发送学号更新邮件失败");
            }
        }
    }


    /**
     * 执行单个学号更新任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeStudentNoUpdate(String oldStudentNo, String newStudentNo) {
        logger.info("开始更新学号: {} -> {}", oldStudentNo, newStudentNo);

        // 检查学生是否仍存在
        Student student = studentService.selectByStudentNo(oldStudentNo);
        if (student == null) {
            logger.warn("学生不存在，跳过学号更新: {}", oldStudentNo);
            return;
        }

        // 检查新学号是否已被占用
        Student existingStudent = studentService.selectByStudentNo(newStudentNo);
        if (existingStudent != null) {
            logger.error("新学号已被占用，学号更新失败: {} -> {}", oldStudentNo, newStudentNo);
            return;
        }

        // 执行批量更新
        updateStudentInfoInOtherTables(oldStudentNo, newStudentNo);

        // 更新学生表学号
        student.setStudentNo(newStudentNo);
        student.setUpdateTime(new Date());
        studentService.update(student);

        logger.info("学号更新成功: {} -> {}", oldStudentNo, newStudentNo);
    }

    private void updateStudentInfoInOtherTables(String oldStudentNo, String newStudentNo) {
        // 更新alarm_record表
        AlarmRecordQuery alarmRecordQuery = AlarmRecordQuery.builder().studentNo(oldStudentNo).build();
        updateTableStudentNo("alarm_record", oldStudentNo, newStudentNo,
                () -> alarmRecordService.selectList(alarmRecordQuery),
                (list) -> list.forEach(ar -> ar.setStudentNo(newStudentNo)),
                (list) -> alarmRecordService.updateBatch(list));

        // 更新application表
        ApplicationQuery applicationQuery = ApplicationQuery.builder().studentNo(oldStudentNo).build();
        updateTableStudentNo("application", oldStudentNo, newStudentNo,
                () -> applicationService.selectByCondition(applicationQuery),
                (list) -> list.forEach(app -> app.setStudentNo(newStudentNo)),
                (list) -> applicationService.updateBatch(list));

        // 更新explanation表
        ExplanationQuery explanationQuery = ExplanationQuery.builder().studentNo(oldStudentNo).build();
        updateTableStudentNo("explanation", oldStudentNo, newStudentNo,
                () -> explanationService.selectByCondition(explanationQuery),
                (list) -> list.forEach(exp -> exp.setStudentNo(newStudentNo)),
                (list) -> explanationService.updateBatch(list));

        // 更新late_return表
        LateReturnQuery lateReturnQuery = LateReturnQuery.builder().studentNo(oldStudentNo).build();
        updateTableStudentNo("late_return", oldStudentNo, newStudentNo,
                () -> lateReturnService.selectByCondition(lateReturnQuery),
                (list) -> list.forEach(lr -> lr.setStudentNo(newStudentNo)),
                (list) -> lateReturnService.updateBatch(list));

        // 不更新notification表

        // 不更新notification_receiver表

        // 更新student_late_stats表
        StudentLateStatsQuery studentLateStatsQuery = StudentLateStatsQuery.builder().studentNo(oldStudentNo).build();
        updateTableStudentNo("student_late_stats", oldStudentNo, newStudentNo,
                () -> studentLateStatsService.selectList(studentLateStatsQuery),
                (list) -> list.forEach(sta -> sta.setStudentNo(newStudentNo)),
                (list) -> studentLateStatsService.updateBatch(list));

        // 更新system_log表
        int updateRows = systemLogService.updateStudentNo(oldStudentNo, newStudentNo);
        logger.info("成功更新系统日志表，一共更新 {} 条数据, \n 更新内容，oldStudentNo:{}->newStudentNo:{}",
                updateRows, oldStudentNo, newStudentNo);
    }

    private <T> void updateTableStudentNo(String tableName,
                                          String oldStudentNo,
                                          String newStudentNo,
                                          Supplier<List<T>> querySupplier,
                                          Consumer<List<T>> updateConsumer,
                                          Function<List<T>, Integer> batchUpdateFunction) {
        List<T> list = querySupplier.get();
        if (list != null && !list.isEmpty()) {
            updateConsumer.accept(list);
            try {
                int updateRows = batchUpdateFunction.apply(list);
                logger.info("成功更新{}表，一共更新 {} 条数据, \n 更新内容，oldStudentNo:{}->newStudentNo:{}",
                        tableName, updateRows, oldStudentNo, newStudentNo);
            } catch (Exception e) {
                logger.error("更新 {} 表的studentNo 出现异常", tableName, e);
                throw new SystemException(ResultCode.ERROR);
            }
        }
    }


}
