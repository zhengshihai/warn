package com.tianhai.warn.enums;

import java.util.Objects;

/**
 * 通知范围枚举
 */
public enum TargetScope {
    ALL_USERS("allusers", "全体用户"),
    SPECIAL_ROLE("specialrole", "特定角色"), // 如果是班级管理员 则会具体到职位角色
    SPECIAL_USER("specialuser", "特定用户");


    private final String code;
    private final String description;

    TargetScope(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据code获取枚举值
     */
    public static TargetScope getByCode(String code) {
        for (TargetScope role : values()) {
            if (Objects.equals(role.getCode(), code)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 判断是否是有效的通知范围代码
     */
    public static boolean isValidRole(String code) {
        return getByCode(code) != null;
    }
}
