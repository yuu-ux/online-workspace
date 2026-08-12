package com.example.online_workspace.models.users;

/**
 * usersテーブルへ登録する認証情報。
 */
public class UserAccount {

	private final String name;
	private final String email;
	private final String passwordHash;

	/**
	 * ユーザー登録用の認証情報を生成する。
	 *
	 * @param name 表示名
	 * @param email 正規化済みのメールアドレス
	 * @param passwordHash BCryptでハッシュ化済みのパスワード
	 */
	public UserAccount(String name, String email, String passwordHash) {
		this.name = name;
		this.email = email;
		this.passwordHash = passwordHash;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}
}
