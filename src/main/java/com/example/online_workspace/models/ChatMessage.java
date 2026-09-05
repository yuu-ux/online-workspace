package com.example.online_workspace.models;

import java.time.Instant;

public record ChatMessage(
	long id,
	long roomId,
	UserSummary sender,
	String content,
	Instant sentAt
) {
	public record UserSummary(long id, String name, String iconUrl) {
	}
}
