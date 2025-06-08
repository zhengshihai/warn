package com.tianhai.warn.enums;

// 处理方类型枚举
public enum HandlerType {
    CAMPUS_SECURITY(1, "学校安保"),
    POLICE(2, "警方"),
    MEDICAL(3, "医疗");

    private final int code;
    private final String desc;

    HandlerType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}