package com.tianhai.warn.annotation;

import com.tianhai.warn.enums.EsFieldType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Zheng
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EsField {
    EsFieldType type() default EsFieldType.KEYWORD;

    String analyzer() default "";

    String searchAnalyzer() default "";

    String format() default "";
}
