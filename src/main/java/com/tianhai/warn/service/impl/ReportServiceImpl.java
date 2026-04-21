package com.tianhai.warn.service.impl;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.model.CalculationResult;
import com.tianhai.warn.model.LateReturn;
import com.tianhai.warn.model.SystemRule;
import com.tianhai.warn.query.LateReturnQuery;
import com.tianhai.warn.service.LateReturnService;
import com.tianhai.warn.service.ReportService;
import com.tianhai.warn.service.SystemRuleService;
import com.tianhai.warn.service.WarningRuleService;
import com.tianhai.warn.utils.DateUtils;
import com.tianhai.warn.utils.ReportExcelExporter;
import com.tianhai.warn.utils.TimeRange;
import com.tianhai.warn.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportServiceImpl.class);

    @Autowired
    private LateReturnService lateReturnService;

    @Autowired
    private SystemRuleService systemRuleService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private WarningRuleService warningRuleService;

    private static final String unjustifiedLateReturnReportKey = "late_return:unjustified:list:report:";

    /**
     * 生成缓存键
     * 根据查询参数生成唯一的缓存键，格式为：prefix_startDate_endDate_college_dormitoryBuilding
     *
     * @param prefix            缓存键前缀，用于区分不同类型的统计数据
     * @param startTime         开始时间
     * @param endTime           结束时间
     * @param college           学院
     * @param dormitoryBuilding 宿舍楼
     * @return 格式化的缓存键
     */
    private String generateCacheKey(String prefix, Date startTime, Date endTime, String college,
            String dormitoryBuilding) {
        return String.format("%s%s_%s_%s_%s",
                prefix,
                DateUtils.formatDateToYMD(startTime),
                DateUtils.formatDateToYMD(endTime),
                college,
                dormitoryBuilding);
    }

    /**
     * 从缓存获取数据，如果不存在则计算并缓存
     * 使用泛型方法处理不同类型的统计数据，确保类型安全
     *
     * @param <T>        统计数据的类型
     * @param cacheKey   缓存键
     * @param calculator 数据计算函数，当缓存未命中时调用
     * @return 统计数据，优先从缓存获取，缓存未命中时计算并缓存
     */
    private <T> T getOrCalculate(String cacheKey, Supplier<T> calculator) {
        // 尝试从缓存获取
        T cachedValue = (T) redisTemplate.opsForValue().get(cacheKey);
        if (cachedValue != null) {
            logger.debug("从缓存获取数据: {}", cacheKey);
            return cachedValue;
        }

        // 缓存不存在，计算并缓存
        T value = calculator.get();
        redisTemplate.opsForValue().set(cacheKey, value, Constants.CACHE_REPORT_EXPIRE_TIME, TimeUnit.SECONDS);
        logger.debug("计算并缓存数据: {}", cacheKey);
        return value;
    }

    /**
     * 获取统计卡片数据（不包括高危预警）
     * 统计指定时间范围内的晚归总次数、晚归学生人数和完成率
     * 结果会被缓存2小时
     *
     * @param startDate         开始日期
     * @param endDate           结束日期
     * @param college           学院，ALL表示所有学院
     * @param dormitoryBuilding 宿舍楼，ALL表示所有宿舍楼
     * @return 统计卡片数据
     */
    @Override
    public ReportCardStatVO statsReportCardDataExcludeHighRisk(Date startDate,
            Date endDate,
            String college,
            String dormitoryBuilding) {
        String cacheKey = generateCacheKey(Constants.CACHE_REPORT_CARD, startDate, endDate, college, dormitoryBuilding);
        return getOrCalculate(cacheKey, () -> calculateReportCardData(startDate, endDate, college, dormitoryBuilding));
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
     * 将特定时间的违规晚归记录缓存到 redis 时效性为今晚23:59:59
     * 
     * @param unjustifiedLateReturns 违规的晚归记录
     */
    public void cachedUnjustifiedLateReturnUntilEndOfDay(String cacheKey, List<LateReturn> unjustifiedLateReturns) {
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime endOfToday = LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59, 59));

        Duration duration = Duration.between(now, endOfToday);
        long seconds = duration.getSeconds();

        redisTemplate.opsForValue().set(
                cacheKey, unjustifiedLateReturns, seconds, TimeUnit.SECONDS);

    }

    /**
     * 并发安全获取未处理的晚归记录 //todo 需要校验封装的方法是否有效
     *
     * @param query 查询条件
     * @return 未处理的晚归记录
     */
    public List<LateReturn> getUnjustifiedLateReturns(LateReturnQuery query) {
        String cacheKey = "lateReturn:unjustified:" + query.hashCode();
        String lockKey = "lock:" + cacheKey;

        // 优先查缓存
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (List<LateReturn>) cached;
        }

        // 尝试加锁
        Boolean lockAcquired = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", 10, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(lockAcquired)) {
            try {
                // 查询数据库并过滤
                List<LateReturn> unjustifiedLateReturns = queryAndFilterLateReturns(query);

                // 写入缓存 时效性截止到今晚23:59:59
                cachedUnjustifiedLateReturnUntilEndOfDay(cacheKey, unjustifiedLateReturns);

                return unjustifiedLateReturns;
            } finally {
                redisTemplate.delete(lockKey); // 释放锁
            }
        } else {
            // 未获得锁 等待一会再尝试读取缓存
            try {
                Thread.sleep(100); // 等待 100 毫秒
            } catch (InterruptedException ignored) {
            }
            Object retry = redisTemplate.opsForValue().get(cacheKey);
            if (retry != null) {
                return (List<LateReturn>) retry;
            } else {
                return queryAndFilterLateReturns(query);
            }
        }
    }

    /**
     * 获得违规的晚归记录
     * 
     * @param query 查询条件
     * @return 晚归记录列表
     */
    private List<LateReturn> queryAndFilterLateReturns(LateReturnQuery query) {
        List<LateReturn> lateReturns = lateReturnService.selectByCondition(query);

        return lateReturns.stream()
                .filter(lr -> Constants.LATE_RETURN_PROCESS_STATUS_FINISHED.equals(lr.getProcessStatus())
                        && Constants.AUDIT_ACTION_REJECT.equals(lr.getProcessResult())
                        || StringUtils.isBlank(lr.getProcessResult()))
                .toList();

    }

    /**
     * 计算周晚归统计数据
     * 按照周一到周日统计晚归次数
     * 结果会被缓存2小时
     *
     * @param startTime         开始时间
     * @param endTime           结束时间
     * @param college           学院，ALL表示所有学院
     * @param dormitoryBuilding 宿舍楼，ALL表示所有宿舍楼
     * @return 按周统计的晚归数据列表
     */
    @Override
    public List<WeekLateReturnStatVO> calWeekLateReturnStat(Date startTime,
            Date endTime,
            String college,
            String dormitoryBuilding) {

        String cacheKey = generateCacheKey(Constants.CACHE_REPORT_WEEK, startTime, endTime, college, dormitoryBuilding);

        return getOrCalculate(cacheKey,
                () -> calculateWeekLateReturnStat(startTime, endTime, college, dormitoryBuilding));
    }

    /**
     * 按照周一到周日的英文缩写（前三个字母，首字母大写） 进行分天统计 (使用Map封装结果）
     * 
     * @param lateReturns 违规的晚归记录
     * @return 按日时间统计的晚归记录
     */
    public List<WeekLateReturnStatVO> countByWeek(List<LateReturn> lateReturns) {
        // 初步统计已有的星期缩写 -> 计数
        Map<String, Integer> statsMapByWeek = lateReturns.stream()
                .filter(lr -> lr.getLateTime() != null)
                .map(lr -> {
                    DayOfWeek dayOfWeek = lr.getLateTime()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .getDayOfWeek();

                    return dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                })
                .collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(x -> 1)));

        // 构造完整的星期缩写列表，顺序固定（Mon -> Sun)
        return Arrays.stream(DayOfWeek.values())
                .map(day -> {
                    String shortName = day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                    int count = statsMapByWeek.getOrDefault(shortName, 0);
                    return new WeekLateReturnStatVO(shortName, count);
                })
                .toList();
    }

    /**
     * 计算学院晚归统计数据
     * 统计每个学院的晚归次数和占比
     * 结果会被缓存2小时
     *
     * @param startTime         开始时间
     * @param endTime           结束时间
     * @param college           学院，ALL表示所有学院
     * @param dormitoryBuilding 宿舍楼，ALL表示所有宿舍楼
     * @return 按学院统计的晚归数据列表
     */
    @Override
    public List<CollegeLateReturnStatVO> calCollegeLateReturnStat(Date startTime, Date endTime, String college,
            String dormitoryBuilding) {
        String cacheKey = generateCacheKey(Constants.CACHE_REPORT_COLLEGE, startTime, endTime, college,
                dormitoryBuilding);
        return getOrCalculate(cacheKey,
                () -> calculateCollegeLateReturnStat(startTime, endTime, college, dormitoryBuilding));
    }

    /**
     * 按照学院进行分类
     * 
     * @param lateReturns 晚归列表
     * @return 学院晚归统计列表
     */
    public List<CollegeLateReturnStatVO> countByCollege(List<LateReturn> lateReturns) {
        // 初步统计已有的学院 -> 计数
        Map<String, Integer> statsMapByCollege = lateReturns.stream()
                .filter(lr -> lr.getCollege() != null)
                .collect(Collectors.groupingBy(
                        LateReturn::getCollege, // 按照college分组
                        Collectors.summingInt(e -> 1) // 每个元素记作1， 统计计数
                ));

        // 构造完整的学院列表，顺序固定
        int total = statsMapByCollege.values().stream().mapToInt(Integer::intValue).sum();

        // 防止除以0
        if (total == 0) {
            return statsMapByCollege.entrySet().stream()
                    .map(entry -> new CollegeLateReturnStatVO(entry.getKey(), entry.getValue(), 0.0))
                    .toList();
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(statsMapByCollege.entrySet());
        List<CollegeLateReturnStatVO> result = new ArrayList<>();
        double sumPercentage = 0.0;

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<String, Integer> entry = entries.get(i);
            String college = entry.getKey();
            int count = entry.getValue();
            double percentage;

            if (i == entries.size() - 1) {
                // 最后一个元素，用1减去前面所有百分比的和
                percentage = Math.round((100.0 - sumPercentage) * 100.0) / 100.0;
            } else {
                percentage = Math.round((count * 100.0 / total) * 100.0) / 100.0;
                sumPercentage += percentage;
            }

            result.add(new CollegeLateReturnStatVO(college, count, percentage));
        }

        return result;
    }

    @Override
    public List<TimeRangeLateReturnStatVO> calTimeLateReturnStat(Date startTime, Date endTime, String college,
            String dormitoryBuilding) {

        LateReturnQuery query = buildLateReturnQuery(startTime, endTime, college, dormitoryBuilding);

        List<LateReturn> unjustifiedLateReturns = getUnjustifiedLateReturns(query);

        // 获取system_rule表中定义的晚归时间段
        SystemRule lateReturnStartTime = systemRuleService.selectByRuleKey(Constants.LATE_RETURN_TIME_WEEKDAYS);
        SystemRule lateReturnEndTime = systemRuleService.selectByRuleKey(Constants.LATE_RETURN_TIME_DECLINE);

        if (lateReturnEndTime == null || lateReturnStartTime == null) {
            logger.error("晚归时间定义不存在");
            throw new SystemException(ResultCode.ERROR);
        }
        if (StringUtils.isBlank(lateReturnStartTime.getRuleValue())
                || StringUtils.isBlank(lateReturnEndTime.getRuleValue())) {
            logger.error("晚归时间定义不完整：{} - {}", lateReturnStartTime, lateReturnEndTime);
            throw new SystemException(ResultCode.ERROR);
        }

        String startTimeStr = lateReturnStartTime.getRuleValue();
        String endTimeStr = lateReturnEndTime.getRuleValue();
        if (StringUtils.isBlank(startTimeStr) || StringUtils.isBlank(endTimeStr)) {
            logger.error("晚归时间定义不完整：{} - {}", startTimeStr, endTimeStr);
            throw new SystemException(ResultCode.ERROR);
        }

        // 按小时整点划分晚归时间定义
        List<String> hourlyRangeList = getHourlyRanges(startTimeStr, endTimeStr);

        // 按照一小时为单位段（目前前端页面暂时设为1小时）进行统计
        // 如果需要统计其他类型的时间段，可直接使用mybatis查询
        return countLateReturnByCustomRanges(hourlyRangeList, unjustifiedLateReturns);
    }

    /**
     * 切割晚归时间
     * 按照一小时为单位切割，对晚归时间范围进行分段
     * 
     * @param startTimeStr 晚归规则起始时间字符串
     * @param endTimeStr   晚归规则结束时间字符串
     * @return 按小时分段的晚归时间范围列表
     */
    public List<String> getHourlyRanges(String startTimeStr, String endTimeStr) {
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("HH:mm");

        LocalTime startTime = LocalTime.parse(startTimeStr, timeFormatter);
        LocalTime endTime = LocalTime.parse(endTimeStr, timeFormatter);

        // 向下取整（起始小时的整点）
        LocalTime startHour = startTime.withMinute(0).withSecond(0);
        // 向上取整（结束小时的整点 + 1小时）
        LocalTime endHour = endTime.withMinute(0).plusHours(1);

        List<String> hourlyRangeList = new ArrayList<>();
        LocalTime current = startHour;

        int maxHours = 48; // 最多48小时，防止死循环
        int count = 0;

        while (!current.equals(endHour) && count < maxHours) {
            LocalTime next = current.plusHours(1);
            String range = current.format(displayFormatter) + "-" + next.format(displayFormatter);
            hourlyRangeList.add(range);
            current = next;
            count++;
        }

        return hourlyRangeList;
    }

    /**
     * 按照自定义时间段进行晚归统计
     * 
     * @param customRangeStrList 自定义时间段字符串列表
     * @param lateReturns        晚归记录列表
     * @return 自定义时间段晚归统计结果列表
     */
    public List<TimeRangeLateReturnStatVO> countLateReturnByCustomRanges(List<String> customRangeStrList,
            List<LateReturn> lateReturns) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        // 将字符串转为 TimeRange 对象列表
        List<TimeRange> timeRanges = customRangeStrList.stream()
                .map(str -> {
                    String[] parts = str.split("-");
                    return new TimeRange(str,
                            LocalTime.parse(parts[0], formatter),
                            LocalTime.parse(parts[1], formatter));
                })
                .toList();

        // 初始化统计 Map
        Map<String, Integer> countMap = new LinkedHashMap<>();
        for (TimeRange range : timeRanges) {
            countMap.put(range.getLabel(), 0);
        }

        for (LateReturn lr : lateReturns) {
            if (lr.getLateTime() == null) {
                continue;
            }

            LocalTime lateTime = lr.getLateTime().toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalTime();

            for (TimeRange range : timeRanges) {
                if (range.contains(lateTime)) {
                    countMap.put(range.getLabel(), countMap.get(range.getLabel()) + 1);
                    break; // 此处通过break去报每条记录只会命中一个时间段
                }
            }
        }

        // 转为 TimeRangeLateReturnStatVO 列表
        return countMap.entrySet().stream()
                .map(e -> new TimeRangeLateReturnStatVO(e.getKey(), e.getValue()))
                .toList();
    }

    /**
     * 计算宿舍楼晚归统计数据
     * 分别统计宿舍门牌号和宿舍楼栋的晚归次数
     * 结果会被缓存2小时
     *
     * @param startTime         开始时间
     * @param endTime           结束时间
     * @param college           学院，ALL表示所有学院
     * @param dormitoryBuilding 宿舍楼，ALL表示所有宿舍楼
     * @return 包含宿舍和楼栋统计数据的Map
     */
    @Override
    public Map<String, List<DormitoryLateReturnStatVO>> calDormitoryLateReturnStat(Date startTime, Date endTime,
            String college, String dormitoryBuilding) {
        String cacheKey = generateCacheKey(Constants.CACHE_REPORT_DORMITORY, startTime, endTime, college,
                dormitoryBuilding);
        return getOrCalculate(cacheKey,
                () -> calculateDormitoryLateReturnStat(startTime, endTime, college, dormitoryBuilding));
    }

    /**
     * 按照宿舍门牌号进行统计
     * 
     * @param unjustifiedLateReturns 晚归记录列表
     * @return 宿舍晚归统计结果列表
     */
    public List<DormitoryLateReturnStatVO> statByDormitory(List<LateReturn> unjustifiedLateReturns) {
        // 按照 dormitory 分组统计
        Map<String, Long> countByDormitory = unjustifiedLateReturns.stream()
                .filter(lr -> lr.getDormitory() != null && !lr.getDormitory().isBlank())
                .collect(Collectors.groupingBy(LateReturn::getDormitory, Collectors.counting()));

        // 构建 DormitoryLateReturnStatVO 列表
        return countByDormitory.entrySet().stream()
                .map(entry -> DormitoryLateReturnStatVO.builder()
                        .dormitory(entry.getKey())
                        .totalCountByDormitory(entry.getValue().intValue())
                        .build())
                .toList();
    }

    /**
     * 按照宿舍楼栋进行统计
     * 
     * @param unjustifiedLateReturns 晚归记录列表
     * @return 宿舍楼栋晚归统计结果列表
     */
    public List<DormitoryLateReturnStatVO> statByDormitoryBuilding(List<LateReturn> unjustifiedLateReturns) {
        // 按照 dormitoryBuilding 分组统计
        Map<String, Long> countByBuilding = unjustifiedLateReturns.stream()
                .filter(lr -> lr.getDormitory() != null && !lr.getDormitory().isBlank())
                .map(lr -> this.extractBuilding(lr.getDormitory()))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // 构建 DormitoryLateReturnStatVO 列表
        return countByBuilding.entrySet().stream()
                .map(entry -> DormitoryLateReturnStatVO.builder()
                        .dormitoryBuilding(entry.getKey())
                        .totalCountByBuilding(entry.getValue().intValue())
                        .build())
                .toList();
    }

    /**
     * 提取宿舍楼栋名 例如"A-101" -> "A栋"
     * 
     * @param dormitory 宿舍门牌号
     * @return 宿舍楼栋
     */
    public String extractBuilding(String dormitory) {
        if (StringUtils.isBlank(dormitory)) {
            logger.warn("提取宿舍楼栋编号时，存在未知的宿舍楼");
            return "未知";
        }
        // 项目规则是：宿舍编号首字符就是楼栋标识
        char buildingChar = dormitory.charAt(0);
        return buildingChar + "栋";

    }

    @Override
    public Workbook exportReportToExcel(Date startTime, Date endTime, String college, String dormitoryBuilding)
            throws IOException {
        logger.info("开始导出Excel报表，筛选条件: startTime={}, endTime={}, college={}, dormitoryBuilding={}",
                startTime, endTime, college, dormitoryBuilding);

        // 优先从缓存获取统计卡片数据
        ReportCardStatVO reportCardStatVO = statsReportCardDataExcludeHighRisk(
                startTime, endTime, college, dormitoryBuilding);
        logger.debug("获取到统计卡片数据: {}", reportCardStatVO);

        // 优先从缓存获取高危预警人数统计
        CalculationResult calculationResult = warningRuleService.calHighRiskStudents(
                startTime, endTime, college, dormitoryBuilding);
        logger.debug("获取高危预警人数统计数据: {}", calculationResult);

        // 优先从缓存获取晚归趋势数据 (按周统计)
        List<WeekLateReturnStatVO> weekLateReturnStatVOList = calWeekLateReturnStat(
                startTime, endTime, college, dormitoryBuilding);
        logger.debug("获取到晚归趋势数据: {}", weekLateReturnStatVOList);

        // 优先从缓存获取学院分布数据
        List<CollegeLateReturnStatVO> collegeLateReturnStatVOList = calCollegeLateReturnStat(
                startTime, endTime, college, dormitoryBuilding);
        logger.debug("获取到学院分布数据: {}", collegeLateReturnStatVOList);

        // 优先从缓存获取宿舍楼统计数据
        Map<String, List<DormitoryLateReturnStatVO>> dormitoryLateReturnStatMap = calDormitoryLateReturnStat(
                startTime, endTime, college, dormitoryBuilding);
        logger.debug("获取到宿舍楼统计数据: {}", dormitoryLateReturnStatMap);

        // 生成Excel工作簿
        return ReportExcelExporter.createReportWorkbook(
                reportCardStatVO, calculationResult,
                weekLateReturnStatVOList, collegeLateReturnStatVOList, dormitoryLateReturnStatMap,
                DateUtils.formatDateToYMD(startTime),
                DateUtils.formatDateToYMD(endTime),
                college,
                dormitoryBuilding);
    }

    // 将原有的计算方法重命名为 calculate 前缀
    private ReportCardStatVO calculateReportCardData(Date startDate, Date endDate, String college,
            String dormitoryBuilding) {
        Map<String, Date> timeRangeMap = DateUtils.resolveSingleDayRange(startDate, endDate);
        Date startTime = timeRangeMap.get(Constants.START_TIME);
        Date endTime = timeRangeMap.get(Constants.END_TIME);

        LateReturnQuery query = buildLateReturnQuery(startTime, endTime, college, dormitoryBuilding);

        // 把违规的晚归记录缓存到 redis 时效性为当前时间到今天的晚上23:59:59
        List<LateReturn> unjustifiedLateReturns = getUnjustifiedLateReturns(query);

        // 获取卡片数据
        int totalLateReturns, lateStudentCount;
        totalLateReturns = unjustifiedLateReturns.isEmpty() ? 0 : unjustifiedLateReturns.size();

        lateStudentCount = unjustifiedLateReturns.isEmpty()
                ? 0
                : (int) unjustifiedLateReturns.stream()
                        .map(LateReturn::getStudentNo)
                        .filter(Objects::nonNull)
                        .distinct()
                        .count();

        int finishedCount = (int) unjustifiedLateReturns.stream()
                .filter(lr -> Constants.LATE_RETURN_PROCESS_STATUS_FINISHED.equals(lr.getProcessStatus()))
                .count();
        int unjustifiedCount = unjustifiedLateReturns.size();

        String completionRateStr;
        if (unjustifiedCount == 0) {
            completionRateStr = "100.00%"; // 没有数据默认设置为100.00%
        } else {
            BigDecimal completionRate = BigDecimal.valueOf(finishedCount * 100.0)
                    .divide(BigDecimal.valueOf(unjustifiedCount), 2, RoundingMode.HALF_UP);
            completionRateStr = completionRate.toPlainString() + "%";
        }

        return ReportCardStatVO.builder()
                .totalLateReturns(totalLateReturns)
                .lateStudentCount(lateStudentCount)
                .completionRate(completionRateStr)
                .build();
    }

    private List<WeekLateReturnStatVO> calculateWeekLateReturnStat(Date startTime, Date endTime, String college,
            String dormitoryBuilding) {
        LateReturnQuery query = buildLateReturnQuery(startTime, endTime, college, dormitoryBuilding);

        List<LateReturn> unjustifiedLateReturns = getUnjustifiedLateReturns(query);

        return countByWeek(unjustifiedLateReturns);
    }

    private List<CollegeLateReturnStatVO> calculateCollegeLateReturnStat(Date startTime, Date endTime, String college,
            String dormitoryBuilding) {
        LateReturnQuery query = buildLateReturnQuery(startTime, endTime, college, dormitoryBuilding);

        List<LateReturn> unjustifiedLateReturns = getUnjustifiedLateReturns(query);

        return countByCollege(unjustifiedLateReturns);
    }

    private Map<String, List<DormitoryLateReturnStatVO>> calculateDormitoryLateReturnStat(Date startTime, Date endTime,
            String college, String dormitoryBuilding) {
        LateReturnQuery query = buildLateReturnQuery(startTime, endTime, college, dormitoryBuilding);

        List<LateReturn> unjustifiedLateReturns = getUnjustifiedLateReturns(query);

        // 按照宿舍门牌号进行筛选
        List<DormitoryLateReturnStatVO> statByDormitoryList = statByDormitory(unjustifiedLateReturns);

        // 按照宿舍楼栋进行筛选
        List<DormitoryLateReturnStatVO> statByDormitoryBuildingList = statByDormitoryBuilding(unjustifiedLateReturns);

        // 构建结果 Map
        Map<String, List<DormitoryLateReturnStatVO>> resultMap = new HashMap<>();
        resultMap.put("dormitory", statByDormitoryList);
        resultMap.put("building", statByDormitoryBuildingList);

        return resultMap;
    }
}
