package com.example.online_workspace.services.auth;

import java.util.Locale;

/**
 * 登録・認証で共通利用するメールアドレスの正規化処理。
 */
public final class EmailNormalizer {

	private EmailNormalizer() {
	}

	/**
	 * 前後の空白を除去し、Localeに依存せず小文字化する。
	 *
	 * @param email 正規化するメールアドレス
	 * @return 正規化済みのメールアドレス
	 */
	public static String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
