package com.example.online_workspace.exceptions;

import java.util.List;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final List<FieldErrorResponse> fieldErrors;

	public ApiException(HttpStatus status, String code, String message) {
		this(status, code, message, List.of());
	}

	public ApiException(
		HttpStatus status,
		String code,
		String message,
		List<FieldErrorResponse> fieldErrors
	) {
		super(message);
		if (!status.is4xxClientError()) {
			throw new IllegalArgumentException("ApiException status must be a 4xx status");
		}
		this.status = status;
		this.code = code;
		this.fieldErrors = List.copyOf(fieldErrors);
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public List<FieldErrorResponse> getFieldErrors() {
		return fieldErrors;
	}
}
