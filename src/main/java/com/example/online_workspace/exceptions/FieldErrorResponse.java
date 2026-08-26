package com.example.online_workspace.exceptions;

public record FieldErrorResponse(
	String field,
	String code,
	String message
) {
}
