package com.tianhai.warn.enums;

import lombok.Getter;

import java.util.Objects;

/**
 * 用户角色枚举
 */

public enum UserRole {
    STUDENT("student", "学生"),
    SYSTEM_USER("systemuser", "系统用户"), // 辅导员 班主任 院级领导等
    DORMITORY_MANAGER("dormitorymanager", "宿管"),
    SUPER_ADMIN("superadmin","超级管理员");

    private final String code;
    private final String description;

    UserRole(String code, String description) {
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
    public static UserRole getByCode(String code) {
        for (UserRole role : values()) {
            if (Objects.equals(role.getCode(), code)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 判断是否是有效的角色代码
     */
    public static boolean isValidRole(String code) {
        return getByCode(code) != null;
    }
}