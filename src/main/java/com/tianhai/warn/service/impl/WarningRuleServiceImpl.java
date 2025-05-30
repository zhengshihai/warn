package com.tianhai.warn.service.impl;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.CalculationStatus;
import com.tianhai.warn.mapper.WarningRuleMapper;
import com.tianhai.warn.model.CalculationResult;
import com.tianhai.warn.model.LateReturn;
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
import java.util.stream.Collectors;

/**
 * 预警规则服务实现类
 */
@Service
public class WarningRuleServiceImpl implements WarningRuleService {

    /**
     * 使用 Hash结构存储 违规的晚归记录
     * key: late_return:unjustified:{studentNo}
     * field: lateReturnId
     * value: lateTime (时间戳)
     */
    private static final String UNJUSTIFIED_LATE_RETURN_KEY = "late_return:unjustified:";

    /**
     * Set 存储特定时间段内 有违规晚归记录的学生学号
     * key: late_return:unjustified:students
     * value： studentNos 学生学号
     */
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

    /**
     * 计算高危学生人数
     * 高危学生定义：在指定时间范围内，违反预警规则达到系统规则阈值的学生
     * 预警规则示例：
     * 1. 7天内晚归3次
     * 2. 15天内晚归5次
     * 3. 30天内晚归8次
     * 
     * 实现策略：
     * 1. 优先从缓存获取结果
     * 2. 如果缓存未命中，根据预估计算时间决定同步或异步计算
     * 3. 计算完成后缓存结果
     * 
     * @param startTime 统计开始时间
     * @param endTime   统计结束时间
     * @param college  学院
     * @param dormitoryBuilding 宿舍楼
     * @return 计算结果，包含状态和人数
     */
    @Override
    public CalculationResult calHighRiskStudents(Date startTime,
                                                 Date endTime,
                                                 String college,
                                                 String dormitoryBuilding) {
        logger.info("开始计算高危学生数量，时间范围: {} 到 {}", startTime, endTime);

        String cacheKey = HIGH_RISK_COUNT_CACHE_KEY + ":" + startTime.getTime() + ":" + endTime.getTime();
        logger.info("缓存key: {}", cacheKey);

        // 检查缓存
        String cachedCount = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedCount != null) {
            logger.info("从缓存获取到结果: {}", cachedCount);
            return CalculationResult.builder()
                    .status(CalculationStatus.COMPLETED)
                    .count(Integer.parseInt(cachedCount))
                    .build();
        }
        logger.info("缓存未命中");

        // 检查是否有正在进行的计算任务
        String taskKey = CALCULATION_TASK_KEY + cacheKey;
        String existingTask = stringRedisTemplate.opsForValue().get(taskKey);
        if (existingTask != null) {
            logger.info("存在正在进行的计算任务: {}", existingTask);
            return CalculationResult.builder()
                    .status(CalculationStatus.CALCULATING)
                    .taskId(existingTask)
                    .build();
        }
        logger.info("没有正在进行的计算任务");

        // 估算计算时间
        long estimatedTime = estimateCalculationTime(startTime, endTime);
        logger.info("估算计算时间: {}ms", estimatedTime);

        String taskId = UUID.randomUUID().toString();

        if (estimatedTime <= THRESHOLD_TIME) {
            logger.info("开始同步计算");
            try {
                Integer count = doCalculateHighRiskStudents(startTime, endTime, college, dormitoryBuilding);
                logger.info("同步计算完成，结果: {}", count);

                // 缓存数据
                stringRedisTemplate.opsForValue().set(cacheKey, count.toString(), 1, TimeUnit.DAYS);
                return CalculationResult.builder()
                        .status(CalculationStatus.COMPLETED)
                        .count(count)
                        .build();
            } catch (Exception e) {
                logger.error("同步统计高危预警人数失败，时间区间： {} - {} ", startTime, endTime, e);
                return CalculationResult.builder()
                        .status(CalculationStatus.FAILED)
                        .build();
            }
        } else {
            logger.info("开始异步计算");
            // 如果预计时间超过阈值，则异步计算
            Date now = new Date();
            Date estimatedEndTime = new Date(now.getTime() + estimatedTime);

            // 记录任务信息
            stringRedisTemplate.opsForValue().set(taskKey, taskId, 1, TimeUnit.HOURS);

            // 启动异步计算
            asyncTaskExecutor.execute(() -> {
                try {
                    // 执行计算
                    Integer count = doCalculateHighRiskStudents(startTime, endTime, college, dormitoryBuilding);

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
     * 根据时间范围和数据量估算计算所需时间
     * 当前估算：每天数据需要0.1秒
     * 注意：这是一个粗略估算，实际时间可能因数据量、系统负载等因素而变化
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 预估计算时间（毫秒）
     */
    private long estimateCalculationTime(Date startTime, Date endTime) {
        long timeRange = endTime.getTime() - startTime.getTime();
        // 此处每天数据需要0.1秒
        return timeRange / (24 * 60 * 60 * 1000) * 10000;
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
     * 获取任务状态
     */
    @Override
    public CalculationResult getTaskStatus(String taskId) {
        String statusKey = "task_status:" + taskId;
        Map<Object, Object> statusMap = stringRedisTemplate.opsForHash().entries(statusKey);

        if (statusMap.isEmpty()) {
            logger.info("任务：{} 不存在或已过期", taskId);
            return CalculationResult.builder()
                    .status(CalculationStatus.FAILED)
                    .build();
        }

        CalculationStatus status = CalculationStatus.valueOf(statusMap.get("status").toString());
        Integer count = statusMap.get("count") != null
                ? Integer.parseInt(statusMap.get("count").toString())
                : null;

        return CalculationResult.builder()
                .status(status)
                .count(count)
                .taskId(taskId)
                .build();
    }

    /**
     * 统计高危预警人数
     */
    public Integer doCalculateHighRiskStudents(Date startTime,
                                               Date endTime,
                                               String college,
                                               String dormitoryBuilding) {
        // 获取所有预警规则
        WarningRule query = WarningRule.builder()
                .status(Constants.ENABLE_STR)
                .build();
        List<WarningRule> rules = warningRuleMapper.selectByCondition(query);

        logger.info("启用的预警规则数量: {}", rules.size());
        for (WarningRule rule : rules) {
            logger.info("预警规则: 时间范围={}天, 最大晚归次数={}",
                    rule.getTimeRangeDays(), rule.getMaxLateTimes());
        }

        // highRiskStudents 存储高危学生学号的集合
        Set<String> highRiskStudents = new HashSet<>();

        // 获取 在特定时间段内 有出现违规晚归记录的学生学号列表
        Set<String> studentNos =
                getStudentNosWithUnjustifiedLateReturns(startTime, endTime, college, dormitoryBuilding);
        logger.info("查询时间范围: {} 到 {}", startTime, endTime);
        logger.info("违规晚归学生数量: {}", studentNos.size());

        for (WarningRule rule : rules) {
            int timeRangeDays = rule.getTimeRangeDays();
            int maxLateTimes = rule.getMaxLateTimes();
            logger.info("开始检查规则: {}天内{}次", timeRangeDays, maxLateTimes);

            for (String studentNo : studentNos) {
                String key = UNJUSTIFIED_LATE_RETURN_KEY + studentNo;
                Map<Object, Object> lateReturns = stringRedisTemplate.opsForHash().entries(key);
                logger.info("学生{}的晚归记录数量: {}", studentNo, lateReturns.size());

                // 使用滑动窗口时间统计
                int lateCount = countLateReturnsInTimeWindow(lateReturns, timeRangeDays, startTime, endTime);
                logger.info("学生{}在{}天内的最大晚归次数: {}", studentNo, timeRangeDays, lateCount);

                // 如果超过阈值 则加入高危学生（重点关注名单）集合
                if (lateCount >= maxLateTimes) {
                    highRiskStudents.add(studentNo);
                    logger.info("学生{}被标记为高危学生", studentNo);
                }
            }
        }

        logger.info("最终高危学生数量: {}", highRiskStudents.size());
        return highRiskStudents.size();
    }

    /**
     * 获取指定时间段内有未正当理由晚归记录的学生学号
     * 实现步骤：
     * 1. 查询指定时间范围内的所有违规晚归记录
     * 2. 按学生分组，统计每个学生的晚归记录
     * 3. 将学生学号和晚归记录缓存到Redis
     * 
     * Redis存储结构：
     * 1. Set结构：存储所有违规晚归的学生学号
     * key: late_return:unjustified:students
     * value: 学生学号集合
     * 
     * 2. Hash结构：存储每个学生的晚归记录
     * key: late_return:unjustified:{studentNo}
     * field: 晚归时间戳
     * value: "1"（表示一次晚归）
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param college 学院
     * @param dormitoryBuilding 宿舍楼
     * @return 违规晚归学生学号集合
     */
    private Set<String> getStudentNosWithUnjustifiedLateReturns(Date startTime,
                                                                Date endTime,
                                                                String college,
                                                                String dormitoryBuilding) {
        // 查询规定时间内所有违规晚归记录
        LateReturnQuery query = buildLateReturnQuery(startTime, endTime, college, dormitoryBuilding);

        List<LateReturn> lateReturns = lateReturnService.listPeriodLateReturns(query);

        logger.info("查询条件: startTime={}, endTime={}", startTime, endTime);
        logger.info("查询到的违规晚归记录数量: {}", lateReturns != null ? lateReturns.size() : 0);

        if (lateReturns != null && !lateReturns.isEmpty()) {
            // 按学生分组
            Map<String, List<LateReturn>> studentLateReturns = lateReturns.stream()
                    .collect(Collectors.groupingBy(LateReturn::getStudentNo));

            // 存储学生学号集合
            Set<String> studentNos = studentLateReturns.keySet();
            stringRedisTemplate.opsForSet().add(UNJUSTIFIED_STUDENTS_SET, studentNos.toArray(new String[0]));
            logger.info("已将{}个学生学号缓存到Redis", studentNos.size());

            // 存储每个学生的晚归时间
            for (Map.Entry<String, List<LateReturn>> entry : studentLateReturns.entrySet()) {
                String studentNo = entry.getKey();
                List<LateReturn> studentRecords = entry.getValue();

                String key = UNJUSTIFIED_LATE_RETURN_KEY + studentNo;
                Map<String, String> lateReturnMap = new HashMap<>();

                for (LateReturn lr : studentRecords) {
                    if (lr.getLateTime() != null) {
                        lateReturnMap.put(
                                String.valueOf(lr.getLateTime().getTime()),
                                "1" // 使用时间戳作为key，值设为1表示一次晚归
                        );
                    }
                }

                if (!lateReturnMap.isEmpty()) {
                    stringRedisTemplate.opsForHash().putAll(key, lateReturnMap);
                    logger.info("学生{}的{}条晚归记录已缓存到Redis", studentNo, lateReturnMap.size());
                }
            }

            return studentNos;
        }

        return new HashSet<>();
    }

    /**
     * 构建晚归查询条件
     *
     * @param startTime         起始时间
     * @param endTime           结束时间
     * @param college           学院
     * @param dormitoryBuilding 宿舍楼栋 "A栋"
     * @return 晚归查询条件
     */
    private LateReturnQuery buildLateReturnQuery(Date startTime, Date endTime,
                                                 String college, String dormitoryBuilding) {
        // 没有正当理由的总晚归次数
        LateReturnQuery query = new LateReturnQuery();
        query.setStartLateTime(startTime);
        query.setEndLateTime(endTime);

        // 处理宿舍楼的范围匹配
        // 1. 如果为ALL 则改为 null，让sql语句匹配所有宿舍楼
        if (dormitoryBuilding.equalsIgnoreCase(Constants.ALL)) {
            dormitoryBuilding = null;
        }
        // 2. 用宿舍楼的首字母进行模糊查询
        if (dormitoryBuilding != null && dormitoryBuilding.matches("^[A-Za-z]栋$")) {
            char firstChar = dormitoryBuilding.charAt(0);
            dormitoryBuilding = String.valueOf(Character.toUpperCase(firstChar));
        }
        // 3.设定条件
        query.setDormitoryLike(dormitoryBuilding);

        // 处理学院的范围匹配
        // 1. 改为 null 让sql语句匹配所有学院
        if (college.equalsIgnoreCase(Constants.ALL)) {
            college = null;
        }
        // 2. 设定条件
        query.setCollege(college);

        return query;
    }

    /**
     * 使用滑动时间窗口统计指定时间范围内的晚归次数
     * 滑动窗口算法：
     * 1. 窗口大小由预警规则的时间范围决定（如7天）
     * 2. 窗口每次滑动一天
     * 3. 统计每个窗口内的晚归次数
     * 4. 返回所有窗口中的最大晚归次数
     * 
     * 示例：
     * 假设统计1-15天的数据，窗口大小为5天
     * 窗口1：1-5天
     * 窗口2：2-6天
     * 窗口3：3-7天
     * ...以此类推
     * 
     * @param lateReturns   学生的晚归记录（Redis Hash结构）
     * @param timeRangeDays 预警规则的时间范围（天）
     * @param startTime     统计开始时间
     * @param endTime       统计结束时间
     * @return 指定时间范围内的最大晚归次数
     */
    private int countLateReturnsInTimeWindow(Map<Object, Object> lateReturns,
            int timeRangeDays,
            Date startTime,
            Date endTime) {
        long startTimestamp = startTime.getTime();
        long endTimeStamp = endTime.getTime();
        long windowSize = timeRangeDays * 24 * 60 * 60 * 1000L; // 转为毫秒

        int maxCount = 0;
        long currentWindowStart = startTimestamp;

        while (currentWindowStart + windowSize <= endTimeStamp) {
            int count = 0;
            long windowEnd = currentWindowStart + windowSize;

            // 统计当前窗口内的晚归次数
            for (Object key : lateReturns.keySet()) {
                long lateTime = Long.parseLong(key.toString());
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