package com.tianhai.warn.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UniqueIdGenerator {
    public static String generate(String prefix) {
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        int random = (int) ((Math.random() * 9 + 1) * 100000); // 6位随机数
        return prefix + date + random;
    }
}