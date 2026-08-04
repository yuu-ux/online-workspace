package com.example.online_workspace.exceptions;

import java.time.Instant;
import java.util.List;

public record ValidationApiErrorResponse(
	int status,
	String code,
	String message,
	String path,
	Instant timestamp,
	String traceId,
	List<FieldErrorResponse> fieldErrors
) {
}
