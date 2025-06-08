package com.tianhai.warn.enums;

import lombok.Getter;

// 报警类型枚举
@Getter
public enum AlarmType {
    ONE_CLICK(1, "一键报警");
//    TIMED(2, "定时报警"),
//    AREA(3, "区域报警");

    private final int code;
    private final String desc;

    AlarmType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}