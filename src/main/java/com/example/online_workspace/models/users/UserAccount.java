package com.example.online_workspace.models.users;

/**
 * usersテーブルへ登録する認証情報。
 *
 * @param name 表示名
 * @param email 正規化済みのメールアドレス
 * @param passwordHash BCryptでハッシュ化済みのパスワード
 */
public record UserAccount(String name, String email, String passwordHash) {
}
