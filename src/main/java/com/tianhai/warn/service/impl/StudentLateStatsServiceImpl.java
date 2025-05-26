package com.tianhai.warn.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.StudentLateQueryDTO;
import com.tianhai.warn.dto.StudentLateResultDTO;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.mapper.StudentLateStatsMapper;
import com.tianhai.warn.model.StudentLateStats;
import com.tianhai.warn.query.StudentLateStatsQuery;
import com.tianhai.warn.service.LateReturnService;
import com.tianhai.warn.service.StudentLateStatsService;
import com.tianhai.warn.utils.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StudentLateStatsServiceImpl implements StudentLateStatsService {

    private static final Logger logger = LoggerFactory.getLogger(StudentLateStatsServiceImpl.class);

    @Autowired
    private LateReturnService lateReturnService;

    @Autowired
    private StudentLateStatsMapper studentLateStatsMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Redis中HMGET方式适用的学生数量 todo 此处需要进行压测才能确定取值
    private static final Integer MAX_STUDENT_COUNT_FOR_HMGET = 100;

    // 创建本地缓存 最大缓存100个键 30分钟有效期
    private final Cache<String, List<StudentLateResultDTO>> localCache = Caffeine.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build();

    private static final String STATS_KEY_PATTERN = "late_stats:%s:%s:%s";
    // 月度统计设置为45天 确保数据在下一个统计周期开始前仍可用
    private static final long MONTHLY_STATS_EXPIRE_DAYS = 45;
    // 学期统计设置为180天 覆盖整个学期，方便学期末的统计和评估
    private static final long SEMESTER_STATS_EXPIRE_DAYS = 180;
    // 周统计过期时间设置为7天
    private static final long WEEKLY_STATS_EXPIRE_DAYS = 7;
    // 动态统计（最近30天和最近7天）过期时间设置为2天
    private static final long DYNAMIC_STATS_EXPIRE_DAYS = 2;

    private static final String lockKey = "stats:lock:fixed_monthly";
    @Autowired
    private RedisLockUtils redisLockUtils;

    @Override
    public StudentLateStats getById(Integer id) {
        if (id == null)
            return null;

        return studentLateStatsMapper.selectById(id);
    }

    @Override
    public StudentLateStats getByStatsId(String statsId) {
        if (statsId == null || statsId.isEmpty())
            return null;
        return studentLateStatsMapper.selectByStatsId(statsId);
    }

    @Override
    public List<StudentLateStats> selectList(StudentLateStatsQuery query) {
        return studentLateStatsMapper.selectByCondition(query);
    }

    @Override
    public PageResult<StudentLateStats> selectByPageQuery(StudentLateStatsQuery query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());

        List<StudentLateStats> list = studentLateStatsMapper.selectByCondition(query);

        PageInfo<StudentLateStats> pageInfo = new PageInfo<>(list);

        PageResult<StudentLateStats> result = new PageResult<>();
        result.setData(pageInfo.getList());
        result.setTotal((int) pageInfo.getTotal());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());

        return result;
    }

    @Override
    @Transactional
    public StudentLateStats createStats(StudentLateStats stats) {
        if (stats == null) {
            logger.error("传入的StudentLateStats不合法");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        if (stats.getStatsId() == null) {
            stats.setStatsId(StatsIdGenerator.generate());
        }
        stats.setLastUpdatedTime(new Date());

        studentLateStatsMapper.insert(stats);

        return stats;
    }

    @Override
    @Transactional
    public int batchCreateStats(List<StudentLateStats> statsList) {
        if (CollectionUtils.isEmpty(statsList)) {
            return 0;
        }
        for (StudentLateStats stats : statsList) {
            if (stats.getStatsId() == null) {
                stats.setStatsId(StatsIdGenerator.generate());
            }
            stats.setLastUpdatedTime(new Date());
        }
        return studentLateStatsMapper.batchInsert(statsList);
    }

    @Override
    public StudentLateStats updateStats(StudentLateStats stats) {
        if (stats == null || stats.getId() == null) {
            logger.error("被更新的stats记录不存在");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }
        stats.setLastUpdatedTime(new Date());
        studentLateStatsMapper.updateById(stats);

        return getById(stats.getId()); // 返回更新后的完整记录
    }

    /**
     * 固定月定时统计晚归记录
     */
    @Override
    @Transactional
    @Scheduled(cron = "0 0 1 * * ?") // 每月1号凌晨1点执行
    // todo 如果任务执行失败需要重试 （直接重试 或 实现重试接口）
    public void generateAndStorePeriodicLateReturnStats() {
        // 统计周期 上一个固定月份
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        calendar.set(Calendar.DAY_OF_MONTH, 1); // 上个月
        Date startDate = calendar.getTime();

        // 上个月最后一天
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date endDate = calendar.getTime();

        Map<String, String> timeInfoMap = SchoolYearSemesterUtils.getSchoolYearAndSemester(calendar);
        String schoolYear = timeInfoMap.get(Constants.SCHOOL_YEAR); // 学年
        String semester = timeInfoMap.get(Constants.SEMESTER); // 学期
        String statsPeriodType = Constants.FIXED_MONTHLY; // 固定月份
        String taskId = UUID.randomUUID().toString();

        List<StudentLateResultDTO> statsCountList = new ArrayList<>();

        boolean needFollowupAction = false; // 判断是否要执行后续业务
        boolean locked = false;

        try {
            logger.info("开始执行定时任务： 按固定月份统计学生晚归情况");

            locked = redisLockUtils.tryLock(lockKey, taskId, 300); // 锁定5分钟
            if (!locked) {
                logger.warn("每月定时统计任务正在执行中，跳过本次执行");
                return;
            }

            long startTime = System.currentTimeMillis();

            logger.info("生成晚归统计报告，时间范围：{}-{}, 统计周期类型：{}, 学年：{}, 学期：{}",
                    startDate, endDate, statsPeriodType, schoolYear, semester);

            StudentLateQueryDTO studentLateQueryDTO = new StudentLateQueryDTO();
            studentLateQueryDTO.setStartDate(startDate);
            studentLateQueryDTO.setEndDate(endDate);
            statsCountList = lateReturnService.selectByStuLateQueryInPeriod(studentLateQueryDTO);

            // todo 日志记录
            // recordTaskExecution(taskId, true, System.currentTimeMillis() - startTime);

            // todo 发送警报
            // sendAlert(taskId, e);

            needFollowupAction = true;
        } catch (Exception e) {
            logger.error("定时统计周期性晚归记录任务失败，taskId:{}", taskId, e);
        } finally {
            if (locked) {
                redisLockUtils.releaseLock(lockKey, taskId);
            }
        }

        if (needFollowupAction) {
            // 处理并保存统计数据
            processAndSaveStatsData(statsCountList, startDate, endDate, schoolYear, semester, statsPeriodType);

            // 将统计结果存入redis
            saveStatsToRedis(statsCountList, schoolYear, semester, statsPeriodType);

            // todo 发送统计结果已出炉的邮件 以及站内通知 给院级领导
        }
    }

    /**
     * 处理并保存统计数据
     * 
     * @param statsCountList  统计结果列表
     * @param startDate       统计开始时间
     * @param endDate         统计结束时间
     * @param schoolYear      学年
     * @param semester        学期
     * @param statsPeriodType 统计周期类型
     */
    @Transactional(rollbackFor = Exception.class)
    protected void processAndSaveStatsData(List<StudentLateResultDTO> statsCountList,
            Date startDate, Date endDate, String schoolYear, String semester, String statsPeriodType) {
        List<StudentLateStats> newStatsList = new ArrayList<>();

        // 创建新的统计记录
        for (StudentLateResultDTO rawCount : statsCountList) {
            StudentLateStats newStat = new StudentLateStats();
            newStat.setStatsId(StatsIdGenerator.generate());
            newStat.setStudentNo(rawCount.getStudentNo());
            newStat.setLateReturnCount(rawCount.getLateReturnCount());

            newStat.setStatsPeriodStartDate(startDate);
            newStat.setStatsPeriodEndDate(endDate);
            newStat.setStatsPeriodType(statsPeriodType);
            newStat.setSchoolYear(schoolYear);
            newStat.setSemester(semester);
            newStat.setActiveStatus(Constants.STATS_ACTIVE); // 新生成的记录表示活跃
            newStat.setLastUpdatedTime(new Date());
            newStatsList.add(newStat);
        }

        if (!newStatsList.isEmpty()) {
            // 将旧的同类型、同学年、同学期、同学生的统计记录标记为不活跃 (isActive= 0 )
            List<String> studentNosForUpdate = newStatsList.stream()
                    .map(StudentLateStats::getStudentNo)
                    .distinct()
                    .toList();
            if (!studentNosForUpdate.isEmpty()) {
                studentLateStatsMapper.batchUpdateIsActive(
                        studentNosForUpdate, schoolYear, semester, statsPeriodType,
                        Constants.STATS_INACTIVE);
            }

            // 批量创建新的统计记录
            batchCreateStats(newStatsList);

            logger.info("成功生成并存储新的 {} 条晚归统计记录", newStatsList.size());
        } else {
            logger.info("没有找到该周期内的晚归记录，时间范围：{}-{}", startDate, endDate);
        }
    }

    /**
     * 保存特定统计周期的统计数据到Redis
     * 
     * @param statsList       统计数据
     * @param schoolYear      学年
     * @param semester        学期
     * @param statsPeriodType 统计周期的类型
     */
    // todo Redis操作失败后直接降级到本地缓存，但没有考虑本地缓存容量限制

    private void saveStatsToRedis(List<StudentLateResultDTO> statsList,
            String schoolYear,
            String semester,
            String statsPeriodType) {
        // 统计数据为空 跳过保存
        if (CollectionUtils.isEmpty(statsList)) {
            logger.warn("统计数据为空，跳过保存");
            return;
        }

        String redisKey = String.format(STATS_KEY_PATTERN,
                schoolYear, semester, statsPeriodType);

        // 使用Pipeline 批量写入
        try {
            redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                for (StudentLateResultDTO stat : statsList) {
                    String field = stat.getStudentNo();
                    redisTemplate.opsForHash().put(redisKey, field, stat);
                }
                return null;
            });

            // 根据不同的统计周期类型设置不同的过期时间
            long expireSeconds = switch (statsPeriodType) {
                case Constants.FIXED_WEEKLY -> WEEKLY_STATS_EXPIRE_DAYS * 24 * 60 * 60;

                case Constants.FIXED_MONTHLY -> MONTHLY_STATS_EXPIRE_DAYS * 24 * 60 * 60;

                case Constants.DYNAMIC_LAST_30_DAYS, Constants.DYNAMIC_LAST_7_DAYS ->
                    DYNAMIC_STATS_EXPIRE_DAYS * 24 * 60 * 60;

                default -> MONTHLY_STATS_EXPIRE_DAYS * 24 * 60 * 60; // 默认使用月度统计的过期时间
            };

            redisTemplate.expire(redisKey, expireSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("无法保存统计数据到redis:{}", e.getMessage(), e);
            // 运行降级策略 将统计数据保存到本地缓存 使用Caffeine实现
            localCache.put(redisKey, statsList);
        }
    }

    /**
     * 获取特定统计周期的统计数据
     * 优先从本地缓存获取 其次Redis 最后Mysql数据库
     * 
     * @param studentNos      学号列表
     * @param schoolYear      学年
     * @param semester        学期
     * @param statsPeriodType 统计周期的类型
     * @return 统计数据
     */
    // todo 没有对输入参数进行有效性验证
    // todo 本地缓存和Redis缓存的数据一致性没有保证
    // todo 数据库查询失败时没有合适的降级策略
    public List<StudentLateResultDTO> getStats(List<String> studentNos,
            String schoolYear,
            String semester,
            String statsPeriodType) {
        // 参数验证
        if (CollectionUtils.isEmpty(studentNos)) {
            return Collections.emptyList();
        }
        if (StringUtils.isAnyBlank(schoolYear, semester, statsPeriodType)) {
            logger.error("以下参数不完整, schoolYear:{}, semester:{}, statsPeriodType:{} ",
                    schoolYear, semester, statsPeriodType);
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        // 参数去重
        studentNos = studentNos.stream().distinct().collect(Collectors.toList());

        String redisKey = String.format(STATS_KEY_PATTERN,
                schoolYear, semester, statsPeriodType);

        // 先尝试从本地缓存中获取
        List<StudentLateResultDTO> statsDataList = localCache.getIfPresent(redisKey);

        // 额外处理没有晚归记录的学生
        if (statsDataList != null) {
            return processStatsData(statsDataList, studentNos);
        }

        // 再尝试从Redis中获取
        try {
            // 如果查询的学生数量超过阈值 则使用Redis的HGETALL方式
            if (studentNos.size() > MAX_STUDENT_COUNT_FOR_HMGET) {
                List<Object> values = redisTemplate.opsForHash().values(redisKey);
                if (!values.isEmpty()) {
                    // 使用新的processStatsData方法处理数据，同时进行类型转换
                    statsDataList = processStatsData(values, studentNos, obj -> (StudentLateResultDTO) obj);
                }
            } else {
                // 如果查询到额学生数量小于阈值 则使用Redis的HMGET批量获取指定学生的数据
                List<Object> values = redisTemplate.opsForHash().multiGet(redisKey, new ArrayList<>(studentNos));
                if (!values.isEmpty()) {
                    // 使用新的processStatsData方法处理数据，同时进行类型转换和过滤null值
                    statsDataList = processStatsData(
                            values.stream().filter(Objects::nonNull).collect(Collectors.toList()),
                            studentNos,
                            obj -> (StudentLateResultDTO) obj);
                }
            }

            // 更新本地缓存
            if (statsDataList != null && !statsDataList.isEmpty()) {
                localCache.put(redisKey, statsDataList);
                return statsDataList;
            }
        } catch (Exception e) {
            logger.error("无法从Redis中获取学生的晚归统计数据", e);
        }

        // 如果Redis中没有数据，从数据库获取
        // 这里根据statsPeriodType进行判断，分成固定时间窗口统计和滑动时间窗口统计
        // 统计完成后筛选出对应的studentNos并额外处理lateReturnCount为0的记录
        // 统计完后还需要保存到缓存中
        // 目前实现的统计周期类型： "FIXED_WEEKLY" "FIXED_MONTHLY" "LAST_30_DAYS" "LAST_7_DAYS";
        StudentLateQueryDTO queryDTO = new StudentLateQueryDTO();

        // 设置统计时间范围
        Map<String, Date> dateRange = calculateDateRange(statsPeriodType);
        queryDTO.setStartDate(dateRange.get("startDate"));
        queryDTO.setEndDate(dateRange.get("endDate"));
        statsDataList = lateReturnService.selectByStuLateQueryInPeriod(queryDTO);

        if (statsDataList == null || statsDataList.isEmpty()) {
            logger.warn("没有晚归记录，建议检查晚归记录业务是否正常或者查询参数是否正常");
        } else {
            // 额外处理没有晚归记录的学生
            statsDataList = processStatsData(statsDataList, studentNos);

            // 保存到缓存中
            if (!statsDataList.isEmpty()) {
                saveStatsToRedis(statsDataList, schoolYear, semester, statsPeriodType);
            }
        }

        return statsDataList;
    }

    /**
     * 根据统计周期类型计算对应的开始和结束时间
     * 
     * @param statsPeriodType 统计周期类型
     * @return 包含开始时间和结束时间的Map
     */
    // todo 没有考虑时区问题
    // todo 没有处理日期计算可能出现的异常情况
    // todo 没有对statsPeriodType进行有效性验证
    private Map<String, Date> calculateDateRange(String statsPeriodType) {
        // 参数校验
        if (StringUtils.isBlank(statsPeriodType)) {
            logger.error("统计周期类型不能为空");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        Map<String, Date> result = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Date endDate = calendar.getTime(); // 结束时间默认为今天0点
        Date startDate;

        switch (statsPeriodType) {
            case Constants.FIXED_WEEKLY:
                // 获取上周的周一和周日
                calendar.add(Calendar.DAY_OF_WEEK, -7); // 先回到上周
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); // 设置为周一
                startDate = calendar.getTime();
                calendar.add(Calendar.DAY_OF_WEEK, 6); // 加6天到周日
                endDate = calendar.getTime();
                break;

            case Constants.FIXED_MONTHLY:
                // 获取上个月的第一天和最后一天
                calendar.add(Calendar.MONTH, -1); // 先回到上个月
                calendar.set(Calendar.DAY_OF_MONTH, 1); // 设置为1号
                startDate = calendar.getTime();
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH)); // 设置为月末
                endDate = calendar.getTime();
                break;

            case Constants.DYNAMIC_LAST_30_DAYS:
                // 最近30天（不包括今天）
                calendar.add(Calendar.DAY_OF_MONTH, -30);
                startDate = calendar.getTime();
                break;

            case Constants.DYNAMIC_LAST_7_DAYS:
                // 最近7天（不包括今天）
                calendar.add(Calendar.DAY_OF_MONTH, -7);
                startDate = calendar.getTime();
                break;

            default:
                logger.error("暂时不支持该统计周期类型");
                throw new BusinessException(ResultCode.ERROR);
        }

        result.put("startDate", startDate);
        result.put("endDate", endDate);
        return result;
    }

    /**
     * 创建一个晚归次数为0的统计记录
     *
     * @param studentNo 学号
     * @return 统计记录
     */
    private StudentLateResultDTO createNoLateReturnStats(String studentNo) {
        StudentLateResultDTO noLateReturnStats = new StudentLateResultDTO();
        noLateReturnStats.setStudentNo(studentNo);
        noLateReturnStats.setLateReturnCount(0);

        return noLateReturnStats;
    }

    /**
     * 处理统计数据，确保返回所有请求学号的数据
     * 对于没有晚归记录的学生，创建lateReturnCount为0的记录
     *
     * @param statsDataList 从缓存或数据库获取的统计数据
     * @param studentNos    请求的学号列表
     * @return 处理后的统计数据列表，包含所有请求学号的数据
     */
    // todo 没有对输入参数进行有效性验证
    // todo 本地缓存和Redis缓存的数据一致性没有保证
    // todo 数据库查询失败时没有合适的降级策略
    private List<StudentLateResultDTO> processStatsData(List<StudentLateResultDTO> statsDataList,
            List<String> studentNos) {
        // 去重
        studentNos = studentNos.stream().distinct().collect(Collectors.toList());

        if (statsDataList == null || statsDataList.isEmpty()) {
            return studentNos.stream()
                    .map(this::createNoLateReturnStats)
                    .collect(Collectors.toList());
        }

        // 创建学号到统计数据的映射
        Map<String, StudentLateResultDTO> statsMap = statsDataList.stream()
                .collect(Collectors.toMap(StudentLateResultDTO::getStudentNo, stat -> stat));

        // 为所有请求的学号创建结果，如果学号不在缓存中，则创建lateReturnCount为0的记录
        return studentNos.stream()
                .map(studentNo -> statsMap.getOrDefault(studentNo, createNoLateReturnStats(studentNo)))
                .collect(Collectors.toList());
    }

    /**
     * 处理统计数据，确保返回所有请求学号的数据
     * 对于没有晚归记录的学生，创建lateReturnCount为0的记录
     * 此方法用于处理需要类型转换的情况，如从Redis获取的Object类型数据
     *
     * @param rawDataList 原始数据列表（如从Redis获取的Object类型数据）
     * @param studentNos  请求的学号列表
     * @param converter   数据转换函数，将原始数据转换为StudentLateResultDTO
     * @return 处理后的统计数据列表，包含所有请求学号的数据
     */
    private List<StudentLateResultDTO> processStatsData(List<Object> rawDataList,
            List<String> studentNos,
            Function<Object, StudentLateResultDTO> converter) {
        // 参数验证
        if (converter == null) {
            throw new IllegalArgumentException("转换函数不能为空");
        }

        // 去重
        studentNos = studentNos.stream().distinct().collect(Collectors.toList());

        if (rawDataList == null || rawDataList.isEmpty()) {
            return studentNos.stream()
                    .map(this::createNoLateReturnStats)
                    .collect(Collectors.toList());
        }

        // 创建学号到统计数据的映射，同时进行类型转换
        Map<String, StudentLateResultDTO> statsMap = rawDataList.stream()
                .map(converter)
                .collect(Collectors.toMap(StudentLateResultDTO::getStudentNo, stat -> stat));

        // 为所有请求的学号创建结果，如果学号不在缓存中，则创建lateReturnCount为0的记录
        return studentNos.stream()
                .map(studentNo -> statsMap.getOrDefault(studentNo, createNoLateReturnStats(studentNo)))
                .collect(Collectors.toList());
    }


    /**
     * 获取特定学生特定天数的晚归次数
     * @param studentNo             学号
     * @param timeRangeDaysList     时间天数
     * @return                      晚归次数
     */
    @Override
    public Map<Integer, Integer> getLateReturnCountsInDaysRange(String studentNo, List<Integer> timeRangeDaysList) {
        if (StringUtils.isBlank(studentNo) || CollectionUtils.isEmpty(timeRangeDaysList)) {
            logger.error("studentNo为空");
            return Collections.emptyMap();
        }

        Map<Integer, Integer> lateStatsMap = new HashMap<>();

        for (Integer days : timeRangeDaysList) {
            if (days != null && days > 0) {
                Map<String, Date> timeRange = DateUtils.getLastNDaysRangeIncludeToday(days);
                Date startDate = timeRange.get(Constants.START_TIME);
                Date endDate = timeRange.get(Constants.END_TIME);

                // lateCount为没有正当理由的晚归记录次数
                Integer lateCount = lateReturnService.countLateReturnsInPeriod(studentNo, startDate, endDate);
                lateStatsMap.put(days, lateCount);
            }
        }

        return lateStatsMap;
    }

    @Override
    public boolean deleteById(Integer id) {
        if (id == null)
            return false;
        return studentLateStatsMapper.deleteById(id) > 0;
    }

    @Override
    public boolean deleteByStatsId(String statsId) {
        if (StringUtils.isBlank(statsId)) {
            return false;
        }

        return studentLateStatsMapper.deleteByStatsId(statsId) > 0;
    }

    // todo
    @Override
    public Integer deleteConditional(StudentLateStatsQuery query) {
        return 0;
    }

}
