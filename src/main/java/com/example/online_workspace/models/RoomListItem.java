package com.example.online_workspace.models;

import java.time.Instant;

public record RoomListItem(
	long id,
	String name,
	long categoryId,
	String categoryName,
	String categoryDescription,
	int categorySortOrder,
	String workStyle,
	int maxMembers,
	int currentMembers,
	String visibility,
	String status,
	long creatorId,
	String creatorName,
	String creatorIconUrl,
	boolean blocked,
	Instant createdAt
) {
}
