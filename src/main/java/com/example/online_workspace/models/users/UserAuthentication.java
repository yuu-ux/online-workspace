package com.example.online_workspace.models.users;

import java.time.Instant;

/**
 * ログインに必要なユーザー情報。
 *
 * @param id ユーザーID
 * @param name 表示名
 * @param email 正規化済みメールアドレス
 * @param passwordHash BCryptでハッシュ化されたパスワード
 * @param accountStatus アカウント状態コード
 * @param suspendedUntil 停止解除日時
 */
public record UserAuthentication(
	long id,
	String name,
	String email,
	String passwordHash,
	String accountStatus,
	Instant suspendedUntil
) {

	/**
	 * APIレスポンスやセッションへ保存する公開ユーザー情報へ変換する。
	 *
	 * @return パスワードハッシュを含まないユーザー情報
	 */
	public AuthenticatedUser toAuthenticatedUser() {
		return new AuthenticatedUser(id, name, email, accountStatus, suspendedUntil);
	}
}
