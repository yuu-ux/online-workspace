package com.example.online_workspace.exceptions;

import org.springframework.http.HttpStatus;

/**
 * ログイン失敗回数が上限に達した場合の例外。
 */
public class TooManyLoginAttemptsException extends ApiException {

	private final long retryAfterSeconds;

	/**
	 * レート制限エラーを生成する。
	 *
	 * @param retryAfterSeconds 再試行可能になるまでの秒数
	 */
	public TooManyLoginAttemptsException(long retryAfterSeconds) {
		super(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "ログイン試行回数が上限に達しました。時間をおいて再度お試しください。");
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public long retryAfterSeconds() {
		return retryAfterSeconds;
	}
}
