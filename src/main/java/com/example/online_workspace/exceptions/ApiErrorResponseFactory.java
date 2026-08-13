package com.example.online_workspace.exceptions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

final class ApiErrorResponseFactory {

	private ApiErrorResponseFactory() {
	}

	static ApiErrorResponse create(
		HttpServletRequest request,
		HttpStatus status,
		String code,
		String message
	) {
		return new ApiErrorResponse(
			status.value(),
			code,
			message,
			request.getRequestURI(),
			Instant.now(),
			UUID.randomUUID().toString()
		);
	}

	static ValidationApiErrorResponse createValidation(
		HttpServletRequest request,
		List<FieldErrorResponse> fieldErrors
	) {
		return new ValidationApiErrorResponse(
			HttpStatus.UNPROCESSABLE_CONTENT.value(),
			"VALIDATION_FAILED",
			"入力内容を確認してください。",
			request.getRequestURI(),
			Instant.now(),
			UUID.randomUUID().toString(),
			List.copyOf(fieldErrors)
		);
	}
}
