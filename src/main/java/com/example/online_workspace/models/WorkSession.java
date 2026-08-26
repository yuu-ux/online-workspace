package com.example.online_workspace.models;

import java.time.Instant;

public record WorkSession(
	long id,
	long userId,
	long roomId,
	long categoryId,
	Instant startedAt,
	Instant endedAt,
	Instant createdAt,
	Instant updatedAt
) {
}
