package com.example.online_workspace.models;

import java.time.OffsetDateTime;

public record DataRetentionCleanupResult(
	OffsetDateTime executionTime,
	OffsetDateTime chatHistoryCutoff,
	OffsetDateTime withdrawnWorkHistoryCutoff,
	int deletedMessages,
	int deletedWorkSessions
) {
}
