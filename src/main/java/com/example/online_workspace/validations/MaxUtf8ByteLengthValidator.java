package com.example.online_workspace.validations;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * UTF-8換算のバイト数を検証する。
 */
public class MaxUtf8ByteLengthValidator implements ConstraintValidator<MaxUtf8ByteLength, CharSequence> {

	private int max;

	@Override
	public void initialize(MaxUtf8ByteLength annotation) {
		max = annotation.max();
	}

	@Override
	public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		return value.toString().getBytes(StandardCharsets.UTF_8).length <= max;
	}
}
