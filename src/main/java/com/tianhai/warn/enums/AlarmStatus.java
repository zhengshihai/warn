package com.tianhai.warn.enums;

import lombok.Getter;

// 报警状态枚举
@Getter
public enum AlarmStatus {
    PENDING(0, "未处理"),
    PROCESSING(1, "处理中"),
    PROCESSED(2, "已处理"),
    CLOSED(3, "已关闭");

    private final int code;
    private final String desc;

    AlarmStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}