package com.tianhai.warn.enums;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// 不同角色业务id的特征枚举
public enum RoleMatcher {
//    STUDENT("STUDENT", Pattern.compile("^S\\d{6}$")),             // 学号：S 开头 + 6 位数字
//    DORMITORY_MANAGER("DORMITORYMANAGER", Pattern.compile("^DM\\d+$")), // DM 开头 + 任意数字
//    SYS_USER("SYSUSER", Pattern.compile("^CNS\\d{4,6}$"));     // CNS 开头 + 4~6 位数字

    STUDENT("STUDENT", Pattern.compile(".*XS.*")),                  // 包含 XS
    DORMITORY_MANAGER("DORMITORYMANAGER", Pattern.compile(".*SG.*")), // 包含 SG
    SYS_USER("SYSUSER", Pattern.compile(".*(FDY|zsh|BZR).*")),
    ILLEGAL("ILLEGAL", Pattern.compile("^a.*")); // 此处的ILLEGAL是用于开发测试


    public final String role;
    public final Predicate<String> matcher;

    RoleMatcher(String role, Pattern pattern) {
        this.role = role;
        this.matcher = pattern.asMatchPredicate(); // Java 11+，将 Pattern 转为 Predicate
    }

    public String getRole() {
        return role;
    }

    public Set<String> filter(Set<String> idSet) {
        return idSet.stream().filter(matcher).collect(Collectors.toSet());
    }
}
