package com.example.online_workspace.controllers.auth;

import com.example.online_workspace.forms.auth.UserRegistrationForm;
import com.example.online_workspace.services.auth.UserRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * ユーザー登録を提供するREST API。
 */
@RestController
@RequestMapping("/api/v1/auth")
public class UserRegistrationController {

	private final UserRegistrationService userRegistrationService;

	/**
	 * ユーザー登録Controllerを生成する。
	 *
	 * @param userRegistrationService ユーザー登録サービス
	 */
	public UserRegistrationController(UserRegistrationService userRegistrationService) {
		this.userRegistrationService = userRegistrationService;
	}

	/**
	 * ユーザーを登録する。
	 *
	 * @param form ユーザー登録の入力値
	 */
	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public void register(@Valid @RequestBody UserRegistrationForm form) {
		userRegistrationService.register(form);
	}
}
