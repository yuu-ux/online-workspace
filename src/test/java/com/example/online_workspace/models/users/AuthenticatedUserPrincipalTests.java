package com.example.online_workspace.models.users;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuthenticatedUserPrincipalTests {

	@Test
	void principalsWithSameUserIdHaveSameIdentity() {
		AuthenticatedUserPrincipal first = new AuthenticatedUserPrincipal(
			new AuthenticatedUser(1L, "旧名", "user@example.com", "ACTIVE", null)
		);
		AuthenticatedUserPrincipal second = new AuthenticatedUserPrincipal(
			new AuthenticatedUser(
				1L,
				"新しい名前",
				"user@example.com",
				"ACTIVE",
				Instant.parse("2026-09-08T00:00:00Z")
			)
		);

		assertThat(first).isEqualTo(second);
		assertThat(first.hashCode()).isEqualTo(second.hashCode());
	}
}
