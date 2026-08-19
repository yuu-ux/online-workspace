package com.example.online_workspace.validations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * UTF-8換算のバイト数に上限を設ける。
 */
@Documented
@Constraint(validatedBy = MaxUtf8ByteLengthValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxUtf8ByteLength {

	String message() default "入力値が長すぎます。";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

	int max();
}
