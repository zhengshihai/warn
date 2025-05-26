package com.tianhai.warn.utils;

import com.tianhai.warn.constants.Constants;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SchoolYearSemesterUtils {
    /**
     * 根据指定日期，计算该日期所在的学年与学期。
     * <p>
     * 学期划分逻辑如下：
     * <ul>
     *     <li>9月-12月：秋季学期，学年为当年-次年（semester = "FALL"）</li>
     *     <li>1月-2月：秋季学期后半段，学年为上一年-当年（semester = "FALL"）</li>
     *     <li>3月-8月：春季学期，学年为上一年-当年（semester = "SPRING"）</li>
     * </ul>
     *
     * 示例：2025年1月 -> 学年为"2024-2025"，学期为"FALL"
     *
     * @param calendar 任意一个时间点（用于判断其所处的学年与学期）
     * @return 包含两个键值对的 Map：
     *         <ul>
     *             <li>schoolYear: 学年字符串（如 "2024-2025"）</li>
     *             <li>semester: 学期标识字符串（"FALL" 表示秋季，"SPRING" 表示春季）</li>
     *         </ul>
     */
    public static Map<String, String> getSchoolYearAndSemester(Calendar calendar) {

        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1; // java中月份是从0开始

        String schoolYear, semester;

        if (month >=9 && month <= 12) {
            // 秋季学期 9-12月， 学年为当前年-次年
            schoolYear = year + "-" + (year + 1);
            semester = Constants.FALL;
        } else if (month >= 1 && month <= 2) {
            // 秋季学期的后半段（1-2月）， 学年为上一年=当前年
            schoolYear = (year - 1) + "-" + year;
            semester = Constants.FALL;
        } else {
            // 春季学期（3-8月）， 学年为上一年-当前年
            schoolYear = (year - 1) + "-" + year;
            semester = Constants.SPRING;
        }

        Map<String, String> result = new HashMap<>();
        result.put(Constants.SCHOOL_YEAR, schoolYear);
        result.put(Constants.SEMESTER, semester);

        return result;
    }


}
