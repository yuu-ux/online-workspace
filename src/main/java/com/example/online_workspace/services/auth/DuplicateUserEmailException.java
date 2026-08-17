package com.example.online_workspace.services.auth;

import com.example.online_workspace.exceptions.ApiException;
import org.springframework.http.HttpStatus;

/**
 * 登録済みのメールアドレスでユーザー登録を試みた場合の例外。
 */
public class DuplicateUserEmailException extends ApiException {

	/**
	 * 重複メールアドレス用の例外を生成する。
	 */
	public DuplicateUserEmailException() {
		super(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "メールアドレスは既に登録されています。");
	}
}
