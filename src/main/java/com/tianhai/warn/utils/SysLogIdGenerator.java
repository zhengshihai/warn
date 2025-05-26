package com.tianhai.warn.utils;

public class SysLogIdGenerator {
    public static String generate() {
        String prefix = "LOG";
        String date = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date());
        int random = (int)((Math.random() * 9 + 1) * 100000); // 6位随机数
        return prefix + date + random;
    }
}