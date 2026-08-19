package com.example.online_workspace.controllers.api.auth;

import com.example.online_workspace.models.users.AuthenticatedUser;

/**
 * Reactクライアントへ返す現在のブラウザセッション状態。
 *
 * @param authenticated 現在のリクエストが認証済みユーザーを持つかどうか
 * @param user 認証済みユーザー。取得できない場合は {@code null}
 */
public record SessionStatusResponse(boolean authenticated, AuthenticatedUser user) {
}
