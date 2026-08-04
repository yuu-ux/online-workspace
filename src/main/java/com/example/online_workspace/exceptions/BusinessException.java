package com.example.online_workspace.exceptions;

import org.springframework.http.HttpStatus;

public class BusinessException extends ApiException {

	public BusinessException(String code, String message) {
		super(HttpStatus.CONFLICT, code, message);
	}
}
