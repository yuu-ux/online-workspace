package com.example.online_workspace.models.users;

import org.springframework.security.core.AuthenticatedPrincipal;

/**
 * セッションへ保存する認証主体。
 *
 * @param user 認証済みユーザー情報
 */
public record AuthenticatedUserPrincipal(AuthenticatedUser user) implements AuthenticatedPrincipal {

	@Override
	public boolean equals(Object other) {
		return other instanceof AuthenticatedUserPrincipal principal
			&& user.id() == principal.user.id();
	}

	@Override
	public int hashCode() {
		return Long.hashCode(user.id());
	}

	@Override
	public String getName() {
		return user.email();
	}
}
