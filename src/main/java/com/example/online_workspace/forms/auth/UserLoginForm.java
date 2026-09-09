package com.example.online_workspace.forms.auth;

import com.example.online_workspace.services.auth.EmailNormalizer;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * ログインAPIの入力値。
 *
 * @param email ログインに使用するメールアドレス
 * @param password パスワード
 */
public record UserLoginForm(
	@NotBlank
	@Email
	@Size(max = 255)
	String email,
	@NotBlank
	@Size(max = 72)
	@Pattern(
		regexp = "^[\\x21-\\x7E]+$",
		message = "パスワードは半角の英字・数字・記号（空白を除く）で入力してください。"
	)
	String password
) {
	public UserLoginForm {
		if (email != null) {
			email = EmailNormalizer.normalize(email);
		}
	}
}
