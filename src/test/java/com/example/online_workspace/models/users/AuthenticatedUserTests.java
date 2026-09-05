package com.example.online_workspace.models.users;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AuthenticatedUserTests {

	@Test
	void authenticationNameIsTheNormalizedEmailAddress() {
		AuthenticatedUser user = new AuthenticatedUser(
			1L,
			"テストユーザー",
			"user@example.com",
			"ACTIVE",
			null
		);
		AuthenticatedUserPrincipal principal = new AuthenticatedUserPrincipal(user);

		assertEquals("user@example.com", principal.getName());
	}
}
