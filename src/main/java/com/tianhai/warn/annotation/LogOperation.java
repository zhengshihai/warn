package com.tianhai.warn.annotation;

import java.lang.annotation.*;

/**
 * 日志注解
 *
 * @author Zheng
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LogOperation {
    String value(); // 操作描述
}
