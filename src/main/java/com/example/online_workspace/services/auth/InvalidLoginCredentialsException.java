package com.example.online_workspace.services.auth;

import com.example.online_workspace.exceptions.ApiException;
import org.springframework.http.HttpStatus;

/**
 * メールアドレスまたはパスワードが一致しない場合の例外。
 */
public class InvalidLoginCredentialsException extends ApiException {

	/**
	 * 認証情報不正の例外を生成する。
	 */
	public InvalidLoginCredentialsException() {
		super(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "メールアドレスまたはパスワードが正しくありません。");
	}
}
