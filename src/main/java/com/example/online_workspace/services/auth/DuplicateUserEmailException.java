package com.example.online_workspace.services.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 登録済みのメールアドレスでユーザー登録を試みた場合の例外。
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateUserEmailException extends RuntimeException {

	/**
	 * 重複メールアドレス用の例外を生成する。
	 */
	public DuplicateUserEmailException() {
		super("メールアドレスは既に登録されています。");
	}
}
