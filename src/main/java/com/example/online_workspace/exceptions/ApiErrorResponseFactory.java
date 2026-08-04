package com.example.online_workspace.exceptions;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;

final class ApiErrorResponseFactory {

	private static final String TRACE_ID_ATTRIBUTE = ApiErrorResponseFactory.class.getName() + ".traceId";

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
			traceId(request)
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
			traceId(request),
			List.copyOf(fieldErrors)
		);
	}

	private static String traceId(HttpServletRequest request) {
		Object existingTraceId = request.getAttribute(TRACE_ID_ATTRIBUTE);
		if (existingTraceId instanceof String traceId) {
			return traceId;
		}

		String traceId = UUID.randomUUID().toString();
		request.setAttribute(TRACE_ID_ATTRIBUTE, traceId);
		return traceId;
	}
}
