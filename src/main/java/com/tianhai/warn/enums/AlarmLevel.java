package com.tianhai.warn.enums;

import lombok.Getter;

// 报警级别枚举
@Getter
public enum AlarmLevel {
    NORMAL(1, "普通"),
    CRITICAL(2, "紧急");

    private final int code;
    private final String desc;

    AlarmLevel(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
