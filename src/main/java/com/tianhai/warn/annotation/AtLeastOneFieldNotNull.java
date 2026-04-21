package com.tianhai.warn.annotation;

import com.tianhai.warn.service.impl.VerificationServiceImpl;
import jakarta.validation.Constraint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 要求查询类至少有一个属性不为空 避免恶意攻击
 *
 * @author Zheng
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = VerificationServiceImpl.class)
public @interface AtLeastOneFieldNotNull {
    String message() default "至少有一个字段不能为空";

    Class<?>[] groups() default {};

    Class<? extends jakarta.validation.Payload>[] payload() default {};
}
