package com.example.online_workspace.models;

import java.time.Instant;

public record WorkSession(
	Long id,
	Long userId,
	Long roomId,
	Long categoryId,
	Instant startedAt,
	Instant endedAt,
	Instant createdAt,
	Instant updatedAt
) {
}
