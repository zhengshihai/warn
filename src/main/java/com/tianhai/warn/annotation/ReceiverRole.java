package com.tianhai.warn.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 通知对象的用户角色类型注解
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReceiverRole {
    String value(); // 用户角色编码，例如学生角色就是Constants.STUDENT
}
