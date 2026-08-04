package com.example.online_workspace.api.error;

import java.time.Instant;

public record ApiErrorResponse(
	int status,
	String code,
	String message,
	String path,
	Instant timestamp,
	String traceId
) {
}
