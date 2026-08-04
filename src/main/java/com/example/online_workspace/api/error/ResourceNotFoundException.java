package com.example.online_workspace.api.error;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

	public ResourceNotFoundException(String code, String message) {
		super(HttpStatus.NOT_FOUND, code, message);
	}
}
