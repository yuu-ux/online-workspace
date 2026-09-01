package com.example.online_workspace.exceptions;

import org.springframework.http.HttpStatus;

/**
 * メールアドレスまたはパスワードが一致しない場合の例外。
 */
public class InvalidLoginCredentialsException extends ApiException {

	public InvalidLoginCredentialsException() {
		super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "メールアドレスまたはパスワードが正しくありません。");
	}
}
