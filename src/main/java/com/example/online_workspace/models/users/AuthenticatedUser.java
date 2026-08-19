package com.example.online_workspace.models.users;

import java.time.Instant;

/**
 * 認証済みユーザーとしてクライアントへ返す情報。
 *
 * @param id ユーザーID
 * @param name 表示名
 * @param email メールアドレス
 * @param role ロールコード
 * @param accountStatus アカウント状態コード
 * @param suspendedUntil 停止解除日時
 */
public record AuthenticatedUser(
	long id,
	String name,
	String email,
	String role,
	String accountStatus,
	Instant suspendedUntil
) {
}
