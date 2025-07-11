package com.tianhai.warn.enums;

import java.util.Objects;

/**
 * SYSTEM_USER用户角色下的职位角色
 */
public enum JobRole {
    COUNSELOR("COUNSELOR", "辅导员"),
    CLASS_TEACHER("CLASSTEACHER", "班主任"),
    DEAN("DEAN", "院系领导"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String description;

    JobRole(String code, String description) {
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
    public static JobRole getByCode(String code) {
        for (JobRole role : values()) {
            if (role.getCode().equalsIgnoreCase(code)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 判断是否是有效的职位角色代码
     */
    public static boolean isValidRole(String code) {
        return getByCode(code) != null;
    }

}
