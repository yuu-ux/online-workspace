package com.example.online_workspace.models.users;

import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * セッションへ保存する認証主体。
 *
 * @param user 認証済みユーザー情報
 */
public record AuthenticatedUserPrincipal(AuthenticatedUser user) implements AuthenticatedPrincipal {

	@Override
	public String getName() {
		return user.email();
	}
}
