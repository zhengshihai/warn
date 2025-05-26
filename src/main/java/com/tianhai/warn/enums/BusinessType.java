package com.tianhai.warn.enums;

import lombok.Getter;

@Getter
public enum BusinessType {
    REGISTER("register", "注册"),
    LOGIN("login", "登录"),
    RESET_PASSWORD("reset", "重置密码");

    private final String code;
    private final String description;

    BusinessType(String code, String description) {
        this.code = code;
        this.description = description;
    }


}