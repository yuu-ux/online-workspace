package com.example.online_workspace.api.error;

public record FieldErrorResponse(
	String field,
	String code,
	String message
) {
}
