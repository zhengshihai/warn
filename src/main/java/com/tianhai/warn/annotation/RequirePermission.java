package com.tianhai.warn.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String[] roles() default{}; // 允许的角色列表 此处的角色只校验四大类 student, sysUser, dormitoryManager, superAdmin

    boolean checkDataScope() default false; // 是否需要数据范围校验
}
