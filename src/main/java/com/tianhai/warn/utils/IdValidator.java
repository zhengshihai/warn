package com.tianhai.warn.utils;

import org.eclipse.tags.shaded.org.apache.bcel.generic.IF_ACMPEQ;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * 校验实体类的xxxId形式是否符合要求
 */
public class IdValidator {

    // 支持的Id前缀及其长度
    private static final PrefixInfo[] SUPPORTED_PREFIXES = {
            new PrefixInfo("EX", 2), // 晚归说明
            new PrefixInfo("AP", 2), // 晚归申请
            new PrefixInfo("NT", 2), // 通知
            new PrefixInfo("LR", 2), // 晚归记录
            new PrefixInfo("LOG", 3) // 系统日志
    };

    // 允许将来日期的前缀
    private static final String[] FUTURE_DATE_ALLOWED_PREFIXES = {"AP", "NT"};

    // 日期格式
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");


    /**
     * 前缀信息类
     */
    private static class PrefixInfo {
        private final String prefix;
        private final int length;
        private final Pattern pattern;

        public PrefixInfo(String prefix, int length) {
            this.prefix = prefix;
            this.length = length;
            // 固定格式： 前缀 + 8位日期（年月日） + 6位随机数
            this.pattern = Pattern.compile("^" + prefix + "\\d{8}\\d{6}$");
        }

        public boolean matches(String objectId) {
            return pattern.matcher(objectId).matches();
        }
    }

    /**
     * 验证ID格式是否合法
     * @param objectId 要验证的objectId
     * @return 是否合法
     */
    public static boolean isValidFormat(String objectId) {
        if (objectId == null) {
            return false;
        }

        for (PrefixInfo prefixInfo : SUPPORTED_PREFIXES) {
            if (prefixInfo.matches(objectId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证objectId前缀是否合法
     * @param objectId 要验证的objectId
     * @return 是否合法
     */
    public static boolean isValidPrefix(String objectId) {
        if (objectId == null) {
            return false;
        }

        for (PrefixInfo prefixInfo : SUPPORTED_PREFIXES) {
            if (objectId.startsWith(prefixInfo.prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查前缀是否允许将来日期
     * @param prefix objectId前缀
     * @return 是否允许将来日期
     */
    private static boolean isFutureDateAllowed(String prefix) {
        for (String allowedPrefix : FUTURE_DATE_ALLOWED_PREFIXES) {
            if (allowedPrefix.equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取objectId的前缀
     * @param objectId objectId字符串
     * @return 前缀，如果objectId格式不合法则返回null
     */
    private static String getPrefix(String objectId) {
        for (PrefixInfo prefixInfo : SUPPORTED_PREFIXES) {
            if (objectId.startsWith(prefixInfo.prefix)) {
                return prefixInfo.prefix;
            }
        }
        return null;
    }

    /**
     * 验证objectId中的日期是否合法
     * @param objectId 要验证的objectId
     * @return 是否合法
     */
    public static boolean isValidDate(String objectId) {
        if (!isValidFormat(objectId)) {
            return false;
        }
        try {
            String prefix = getPrefix(objectId);
            if (prefix == null) {
                return false;
            }

            // 获取前缀长度
            int prefixLength = prefix.length();
            String dateStr = objectId.substring(prefixLength, prefixLength + 8);
            Date date = DATE_FORMAT.parse(dateStr);

            // 解析年月日
            int year = Integer.parseInt(dateStr.substring(0, 4));
            int month = Integer.parseInt(dateStr.substring(4, 6));
            int day = Integer.parseInt(dateStr.substring(6, 8));

            // 验证月份是否在1-12之间
            if (month < 1 || month > 12) {
                return false;
            }

            // 验证日期是否在1-31之间
            if (day < 1 || day > 31) {
                return false;
            }

            // 验证具体月份的天数
            int[] daysInMonth = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

            // 处理闰年
            if (month == 2 && isLeapYear(year)) {
                daysInMonth[2] = 29;
            }

            // 验证日期是否超过该月的最大天数
            if (day > daysInMonth[month]) {
                return false;
            }


            // 如果前缀允许将来日期，则只检查日期格式是否合法
            if (isFutureDateAllowed(prefix)) {
                return true;
            }

            // 对于其他类型，检查日期是否不超过当前日期
            return !date.after(new Date());
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * 判断是否为闰年
     * @param year 年份
     * @return 是否为闰年
     */
    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    /**
     * 验证objectId是否完全合法（格式、前缀、日期都合法）
     * @param objectId 要验证的objectId
     * @return 是否合法
     */
    public static boolean isValid(String objectId) {
        return isValidFormat(objectId) && isValidPrefix(objectId) && isValidDate(objectId);
    }


    /**
     * 验证特定类型的objectId
     * @param objectId 要验证的objectId
     * @param expectedPrefix 期望的前缀
     * @return 是否合法
     */
    public static boolean isValidType(String objectId, String expectedPrefix) {
        if (!isValidFormat(objectId)) {
            return false;
        }
        String prefix = getPrefix(objectId);
        return expectedPrefix.equals(prefix) && isValidDate(objectId);
    }





}
