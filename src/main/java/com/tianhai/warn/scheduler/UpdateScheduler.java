package com.tianhai.warn.scheduler;

import cn.hutool.json.JSONUtil;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.query.*;
import com.tianhai.warn.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
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


    private static final String STUDENT_NO_UPDATE_QUEUE = "student:no:update:queue";

    /**
     * 将学号更新任务加入队列
     */
    public void scheduleStudentNoUpdate(String oldStudentNo, String newStudentNo) {
        StudentNoUpdateTask task = StudentNoUpdateTask.builder()
                .oldStudentNo(oldStudentNo)
                .newStudentNo(newStudentNo)
                .createTime(new Date())
                .status("PENDING")
                .build();

        // 加入Redis队列
        redisTemplate.opsForList().rightPush(STUDENT_NO_UPDATE_QUEUE, task);

        logger.info("学号更新任务已加入队列: {} -> {}", oldStudentNo, newStudentNo);
    }

    /**
     * 定时执行学号更新任务（每天凌晨2点执行）
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void executeStudentNoUpdateTasks() {
        logger.info("开始执行学号更新任务...");

        while (true) {
            // 从队列中取出任务
            String taskJson = (String) redisTemplate.opsForList().leftPop(STUDENT_NO_UPDATE_QUEUE, 5, TimeUnit.SECONDS);
            if (taskJson == null) {
                break; // 队列为空，退出
            }

            try {
                StudentNoUpdateTask task = JSONUtil.toBean(taskJson, StudentNoUpdateTask.class);
                executeStudentNoUpdate(task);
            } catch (Exception e) {
                logger.error("执行学号更新任务失败: {}", taskJson, e);
                // 可以选择重试或记录失败日志
            }
        }

        logger.info("学号更新任务执行完成");
    }

    /**
     * 执行单个学号更新任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeStudentNoUpdate(StudentNoUpdateTask task) {
        String oldStudentNo = task.getOldStudentNo();
        String newStudentNo = task.getNewStudentNo();

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
        updateTableStudentNo("alarm_record", oldStudentNo, newStudentNo,
                () -> alarmRecordService.selectList(AlarmRecordQuery.builder().studentNo(oldStudentNo).build()),
                (list) -> list.forEach(ar -> ar.setStudentNo(newStudentNo)),
                (list) -> alarmRecordService.updateBatch(list));

        // 更新application表
        updateTableStudentNo("application", oldStudentNo, newStudentNo,
                () -> applicationService.selectByCondition(ApplicationQuery.builder().studentNo(oldStudentNo).build()),
                (list) -> list.forEach(app -> app.setStudentNo(newStudentNo)),
                (list) -> applicationService.updateBatch(list));

        // 更新explanation表
        updateTableStudentNo("explanation", oldStudentNo, newStudentNo,
                () -> explanationService.selectByCondition(ExplanationQuery.builder().studentNo(oldStudentNo).build()),
                (list) -> list.forEach(exp -> exp.setStudentNo(newStudentNo)),
                (list) -> explanationService.updateBatch(list));

        // 更新late_return表
        updateTableStudentNo("late_return", oldStudentNo, newStudentNo,
                () -> lateReturnService.selectByCondition(LateReturnQuery.builder().studentNo(oldStudentNo).build()),
                (list) -> list.forEach(lr -> lr.setStudentNo(newStudentNo)),
                (list) -> lateReturnService.updateBatch(list));

        // 更新notification表
        updateTableStudentNo("notification", oldStudentNo, newStudentNo,
                () -> notificationService.selectByCondition(NotificationQuery.builder().targetId(oldStudentNo).build()),
                (list) -> list.forEach(notif -> notif.setTargetId(newStudentNo)),
                (list) -> notificationService.updateBatch(list));

        // 更新student_late_stats表
        updateTableStudentNo("student_late_stats", oldStudentNo, newStudentNo,
                () -> studentLateStatsService.selectList(StudentLateStatsQuery.builder().studentNo(oldStudentNo).build()),
                (list) -> list.forEach(stats -> stats.setStudentNo(newStudentNo)),
                (list) -> studentLateStatsService.updateBatch(list));

        // todo 更新系统日志该学生的操作
    }

    private <T> void updateTableStudentNo(String tableName, String oldStudentNo, String newStudentNo,
                                          Supplier<List<T>> querySupplier,
                                          Consumer<List<T>> updateConsumer,
                                          Function<List<T>, Integer> batchUpdateFunction) {
        List<T> list = querySupplier.get();
        if (list != null && !list.isEmpty()) {
            updateConsumer.accept(list);
            try {
                int updateRows = batchUpdateFunction.apply(list);
                logger.info("成功更新{}表，一共更新 {} 条数据", tableName, updateRows);
            } catch (Exception e) {
                logger.error("更新 {} 表的studentNo 出现异常", tableName, e);
                throw new SystemException(ResultCode.ERROR);
            }
        }
    }


}
