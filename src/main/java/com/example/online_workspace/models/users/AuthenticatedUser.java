package com.example.online_workspace.models.users;

import java.time.OffsetDateTime;

public record AuthenticatedUser(
	long id,
	String name,
	String email,
	String accountStatus,
	OffsetDateTime suspendedUntil
) {
}
