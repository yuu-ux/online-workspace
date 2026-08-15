package com.example.online_workspace.models;

import java.time.Instant;

public record RoomMember(
	long membershipId,
	long userId,
	String userName,
	String iconUrl,
	Instant joinedAt
) {
}
