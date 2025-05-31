package com.tianhai.warn.utils;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.LateReturnReportChartDTO;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import lombok.Builder;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 日期工具类
 */
public class DateUtils {
    private static final Logger logger = LoggerFactory.getLogger(DateUtils.class);

    public static final String START_TIME = "startTime";
    public static final String END_TIME = "endTime";
    private static final DateTimeFormatter YMD_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 获取指定日期的开始时间（00:00:00）
     * 
     * @param date 指定日期
     * @return 开始时间
     */
    public static Date getStartOfDay(Date date) {
        LocalDateTime localDateTime = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        LocalDateTime startOfDay = localDateTime.toLocalDate().atStartOfDay();

        return Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取指定日期的结束时间（23:59:59）
     * 
     * @param date 指定日期
     * @return 结束时间
     */
    public static Date getEndOfDay(Date date) {
        LocalDateTime localDateTime = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        LocalDateTime endOfDay = localDateTime.toLocalDate().atTime(23, 59, 59);
        return Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取指定日期的开始和结束时间
     * 
     * @param date 指定日期
     * @return 包含开始和结束时间的数组，[0]为开始时间，[1]为结束时间
     */
    public static Date[] getDayRange(Date date) {
        return new Date[] {
                getStartOfDay(date),
                getEndOfDay(date)
        };
    }

    /**
     * 获取最近7天的起始和截止时间（包含今天），以Map形式返回， 形式是年月日时分秒
     * key: "startDate", "endDate"，value为 java.util.Date 类型
     */
    public static Map<String, Date> getLast7DaysRange() {
        Map<String, Date> result = new HashMap<>();

        // 当前时间
        LocalDateTime now = LocalDateTime.now();

        // 起始时间：6天前的 00:00:00（共7天：含今天）
        LocalDateTime startDateTime = now.minusDays(6).with(LocalTime.MIN);

        // 截止时间：今天的 23:59:59.999999999
        LocalDateTime endDateTime = now.with(LocalTime.MAX);

        // 转换为 java.util.Date
        Date startDate = Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant());

        result.put(Constants.START_TIME, startDate);
        result.put(Constants.END_TIME, endDate);

        return result;
    }

    /**
     * 获取最近 N 天（包含今天）的起始和截止时间
     */
    public static Map<String, Date> getLastNDaysRangeIncludeToday(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("天数必须大于0");
        }

        Map<String, Date> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime startDateTime = now.minusDays(days - 1).with(LocalTime.MIN);
        LocalDateTime endDateTime = now.with(LocalTime.MAX);

        result.put(Constants.START_TIME, toDate(startDateTime));
        result.put(Constants.END_TIME, toDate(endDateTime));
        return result;
    }

    /**
     * 获取最近 N 天（不包含今天）的起始和截止时间  年月日时分秒
     */
    public static Map<String, Date> getLastNDaysExcludeToday(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("天数必须大于0");
        }

        Map<String, Date> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // 起始时间：从（今天 - days）天的 00:00:00
        LocalDateTime startDateTime = now.minusDays(days).with(LocalTime.MIN);

        // 截止时间：昨天的 23:59:59
        LocalDateTime endDateTime = now.minusDays(1).with(LocalTime.MAX);

        result.put(Constants.START_TIME, toDate(startDateTime));
        result.put(Constants.END_TIME, toDate(endDateTime));
        return result;
    }

    /**
     * LocalDateTime 转 java.util.Date
     */
    private static Date toDate(LocalDateTime dateTime) {
        try {
            return Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        } catch (ArithmeticException e) {
            // 如果发生溢出，使用一个安全的日期范围
            if (dateTime.equals(LocalDateTime.MAX)) {
                return new Date(Long.MAX_VALUE);
            } else if (dateTime.equals(LocalDateTime.MIN)) {
                return new Date(Long.MIN_VALUE);
            }
            throw e;
        }
    }

    /**
     * 将指定的 Date 补全为当天的开始时间（00:00:00）
     */
    public static Date toStartOfDay(Date date) {
        if (date == null) {
            return null;
        }

        LocalDate localDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        LocalDateTime startOfDay = localDate.atStartOfDay();

        return Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 将指定的 Date 补全为当天的结束时间（23:59:59）
     */
    public static Date toEndOfDay(Date date) {
        if (date == null) {
            return null;
        }
        LocalDate localDate = date.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        // 当天的 23:59:59（精确到秒）
        LocalDateTime endOfDay = localDate.atTime(23, 59, 59);

        return Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     *  // 返回某天的年月日 以及补全起始时间和结束时间 00:00:00和23:59:59
     * @param startDate     起始日期
     * @param endDate       终止日期
     * @return              时间范围
     */
    public static Map<String, Date> resolveSingleDayRange(Date startDate, Date endDate) {
        Map<String, Date> timeRangeMap = new HashMap<>();
        if (startDate == null || endDate == null) {
            logger.error("时间参数不合法");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        if (startDate.after(endDate)) {
            logger.error("时间参数不合法，startDate: {}, endDate:{} ", startDate, endDate);
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        timeRangeMap.put(START_TIME, DateUtils.toStartOfDay(startDate));
        timeRangeMap.put(END_TIME, DateUtils.toEndOfDay(endDate));

        return timeRangeMap;
    }

    /**
     * 将 java.util.Date 转换为 "yyyy-MM-dd" 格式的字符串
     *
     * @param date java.util.Date 对象
     * @return 格式化后的字符串，例如 "2025-05-30"
     */
    public static String formatDateToYMD(Date date) {
        if (date == null) {
            return null;
        }
        LocalDate localDate = Instant.ofEpochMilli(date.getTime())
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        return localDate.format(YMD_FORMATTER);
    }
}
