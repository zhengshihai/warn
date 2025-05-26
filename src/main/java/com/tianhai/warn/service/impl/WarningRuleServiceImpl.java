package com.tianhai.warn.service.impl;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.CalculationStatus;
import com.tianhai.warn.mapper.WarningRuleMapper;
import com.tianhai.warn.model.CalculationResult;
import com.tianhai.warn.model.WarningRule;
import com.tianhai.warn.query.LateReturnQuery;
import com.tianhai.warn.service.LateReturnService;
import com.tianhai.warn.service.WarningRuleService;
import org.checkerframework.framework.qual.CFComment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 预警规则服务实现类
 */
@Service
public class WarningRuleServiceImpl implements WarningRuleService {

    /** 使用 Hash结构存储 违规的晚归记录
     key: late_return:unjustified:{studentNo}
     field: lateReturnId
     value: lateTime (时间戳) */
    private static final String UNJUSTIFIED_LATE_RETURN_KEY = "late_return:unjustified:";

    /** Set 存储所有 有违规晚归记录的学生学号
     key: late_return:unjustified:students
     value： 所有 有违规晚归记录的学生学号*/
    private static final String UNJUSTIFIED_STUDENTS_SET = "late_return:unjustified:students";

    private static final String HIGH_RISK_COUNT_CACHE_KEY = "high_risk_count";

    private static final String CALCULATION_TASK_KEY = "calculation_task:";

    private static final long THRESHOLD_TIME = 4000L; // 设定阈值时间为4秒

    @Autowired
    private WarningRuleMapper warningRuleMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    @Autowired
    private LateReturnService lateReturnService;

    private static final Logger logger = LoggerFactory.getLogger(WarningRuleServiceImpl.class);


    @Override
    public WarningRule selectById(Integer id) {
        return warningRuleMapper.selectById(id);
    }

    @Override
    public List<WarningRule> selectAll() {
        return warningRuleMapper.selectAll();
    }

    @Override
    public List<WarningRule> selectByCondition(WarningRule rule) {
        return warningRuleMapper.selectByCondition(rule);
    }

    @Override
    public List<WarningRule> selectByStatus(String status) {
        return warningRuleMapper.selectByStatus(status);
    }

    @Override
    public int insert(WarningRule rule) {
        return warningRuleMapper.insert(rule);
    }

    @Override
    public int update(WarningRule rule) {
        return warningRuleMapper.update(rule);
    }

    @Override
    public int deleteById(Integer id) {
        return warningRuleMapper.deleteById(id);
    }

    @Override
    public int updateStatus(Integer id, String status) {
        return warningRuleMapper.updateStatus(id, status);
    }

    @Override
    public CalculationResult calculateHighRiskStudents(Date startTime, Date endTime) {
        String cacheKey = HIGH_RISK_COUNT_CACHE_KEY + startTime.getTime() + ":" + endTime.getTime();

        // 检查缓存
        String cachedCount = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedCount != null) {
            return CalculationResult.builder()
                    .status(CalculationStatus.COMPLETED)
                    .count(Integer.parseInt(cachedCount))
                    .build();
        }

        // 检查是否有正在进行的计算任务
        String taskKey = CALCULATION_TASK_KEY + cacheKey;
        String existingTask = stringRedisTemplate.opsForValue().get(taskKey);
        if (existingTask != null) {
            return CalculationResult.builder()
                    .status(CalculationStatus.CALCULATING)
                    .taskId(existingTask)
                    .build();
        }

        // 估算计算时间
        long estimatedTime = estimateCalculationTime(startTime, endTime);
        String taskId = UUID.randomUUID().toString();

        if (estimatedTime <= THRESHOLD_TIME) {
            // 如果预计时间小于阈值，则同步计算
            try {
                Integer count = doCalculateHighRiskStudents(startTime, endTime);

                // 缓存数据
                stringRedisTemplate.opsForValue().set(cacheKey, count.toString(), 1, TimeUnit.DAYS);
                return CalculationResult.builder()
                        .status(CalculationStatus.COMPLETED)
                        .count(count)
                        .build();
            } catch (Exception e) {
                logger.error("同步统计高危预警人数失败，时间区间： {} - {} ", startTime, endTime);

                return CalculationResult.builder()
                        .status(CalculationStatus.FAILED)
                        .build();
            }
        } else {
            // 如果预计时间超过阈值，则异步计算
            Date now = new Date();
            Date estimatedEndTime = new Date(now.getTime() + estimatedTime);

            // 记录任务信息
            stringRedisTemplate.opsForValue().set(taskKey, taskId, 1, TimeUnit.HOURS);

            // 启动异步计算
            asyncTaskExecutor.execute(() -> {
                try {
                    // 执行计算
                    Integer count = doCalculateHighRiskStudents(startTime, endTime);

                    // 缓存结果
                    stringRedisTemplate.opsForValue().set(cacheKey, count.toString(), 1, TimeUnit.DAYS);

                    // 更新任务状态
                    updateTaskStatus(taskId, CalculationStatus.COMPLETED, count);
                } catch (Exception e) {
                    logger.error("异步统计高危预警人数失败，时间区间： {} - {} ", startTime, endTime);
                    updateTaskStatus(taskId, CalculationStatus.FAILED, null);
                } finally {
                    stringRedisTemplate.delete(taskKey);
                }
            });

            return CalculationResult.builder()
                    .status(CalculationStatus.CALCULATING)
                    .taskId(taskId)
                    .startTime(now)
                    .estimatedEndTime(estimatedEndTime)
                    .build();
        }
    }

    /**
     * 估算计算时间
     */
    private long estimateCalculationTime(Date startTime, Date endTime) {
        // 根据时间范围和数据量估算计算时间
        long timeRange = endTime.getTime() - startTime.getTime();

        // todo 需要根据历史数据或模拟数据估算
        // 此处每天数据需要1秒
        return timeRange / (24 * 60 * 60 * 1000) * 1000;
    }

    /**
     * 更新任务状态
     */
    private void updateTaskStatus(String taskId, CalculationStatus status, Integer count) {
        String statusKey = "task_status:" + taskId;
        Map<String, String> statusMap = new HashMap<>();
        statusMap.put("status", status.name());
        if (count != null) {
            statusMap.put("count", count.toString());
        }

        stringRedisTemplate.opsForHash().putAll(statusKey, statusMap);
        stringRedisTemplate.expire(statusKey, 1, TimeUnit.HOURS);
    }

    /**
      获取任务状态
     */
    @Override
    public CalculationResult getTaskStatus(String taskId) {
        String statusKey = "task_status:" + taskId;
        Map<Object, Object> statusMap = stringRedisTemplate.opsForHash().entries(statusKey);

        if (statusMap.isEmpty()) {
            logger.info("任务：{} 不存在货已过期", taskId);
            return CalculationResult.builder()
                    .status(CalculationStatus.FAILED)
                    .build();
        }

        CalculationStatus status = CalculationStatus.valueOf(statusMap.get("status").toString());
        Integer count = statusMap.get("count") != null
                ? Integer.parseInt(statusMap.get("count").toString()) : null;

        return CalculationResult.builder()
                .status(status)
                .count(count)
                .taskId(taskId)
                .build();
    }


    /**
     * 统计高危预警人数
     */
    public Integer doCalculateHighRiskStudents(Date startTime, Date endTime) {
        // 获取所有预警规则
        WarningRule query = WarningRule.builder()
                .status(Constants.ENABLE_STR)
                .build();
        List<WarningRule> rules = warningRuleMapper.selectByCondition(query);

        // 获取所有学生的没有正当理由晚归记录
        Set<String> highRiskStudents = new HashSet<>();

        for (WarningRule rule : rules) {
            int timeRangeDays = rule.getTimeRangeDays();
            int maxLateTimes = rule.getMaxLateTimes();

            // 异步对每个学生进行统计
            Set<String> studentNos = getStudentNosWithUnjustifiedLateReturns();
            for (String studentNo : studentNos) {
                String key = UNJUSTIFIED_LATE_RETURN_KEY + studentNo;
                Map<Object, Object> lateReturns = stringRedisTemplate.opsForHash().entries(key);

                // 使用滑动窗口时间统计
                int lateCount = countLateReturnsInTimeWindow(lateReturns, timeRangeDays, startTime, endTime);

                // 如果超过阈值 则加入高危学生（重点关注名单）集合
                if (lateCount >= maxLateTimes) {
                    highRiskStudents.add(studentNo);
                }
            }
        }

        // todo 根据不同时间条件缓存结果到redis中

        return highRiskStudents.size();
    }

    /**
     * 获取所有有未正当理由晚归记录的学生学号 //todo 这里需要事先导入原本已经存在的没有正当理由晚归数据
     */
    private Set<String> getStudentNosWithUnjustifiedLateReturns() {
        Set<String> studentNoSet = lateReturnService.listPeriodLateReturnStudentNos(new LateReturnQuery());
        if (studentNoSet != null && studentNoSet.isEmpty()) {
            stringRedisTemplate.opsForSet().add(UNJUSTIFIED_STUDENTS_SET, studentNoSet.toArray(new String[0]));
        }

        return studentNoSet;
    }

    /**
     * 使用滑动时间窗口统计指定时间范围内的晚归次数
     */
    private int countLateReturnsInTimeWindow(Map<Object, Object> lateReturns,
                                             int timeRangeDays,
                                             Date startTime,
                                             Date endTime) {
        long startTimestamp = startTime.getTime();
        long endTimeStamp = endTime.getTime();
        long windowSize = timeRangeDays * 24 * 60 * 1000L; // 转为毫秒

        int maxCount = 0;
        long currentWindowStart = startTimestamp;

        while (currentWindowStart + windowSize <= endTimeStamp) {
            int count = 0;
            long windowEnd = currentWindowStart + windowSize;

            // 统计当前窗口内的晚归次数
            for (Object value : lateReturns.values()) {
                long lateTime = Long.parseLong(value.toString());
                if (lateTime >= currentWindowStart && lateTime < windowEnd) {
                    count++;
                }
            }

            maxCount = Math.max(maxCount, count);
            currentWindowStart += 24 * 60 * 60 * 1000L; // 滑动一天
        }

        return maxCount;
    }

}