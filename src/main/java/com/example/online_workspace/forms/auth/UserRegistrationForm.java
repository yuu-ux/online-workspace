package com.example.online_workspace.forms.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ユーザー登録APIで受け取る入力値。
 *
 * @param name 表示名
 * @param email ログインに使用するメールアドレス
 * @param password 登録するパスワード
 */
public record UserRegistrationForm(
	@NotBlank
	@Size(max = 100)
	String name,
	@NotBlank
	@Email
	@Size(max = 255)
	String email,
	@NotBlank
	@Size(min = 8, max = 72)
	String password
) {
}
