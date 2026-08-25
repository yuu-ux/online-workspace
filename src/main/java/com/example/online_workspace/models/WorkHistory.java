package com.example.online_workspace.models;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class WorkHistory {

	private WorkHistory() {
	}

	public record RoomCategory(
		long id,
		String name,
		String description,
		int sortOrder
	) {
	}

	public record UserSummary(
		long id,
		String name,
		String iconUrl
	) {
	}

	public record RoomSummary(
		long id,
		String name,
		RoomCategory category,
		String workStyle,
		int maxMembers,
		int currentMembers,
		String status,
		UserSummary createdBy,
		boolean joinable,
		String joinRestriction,
		Instant createdAt
	) {
	}

	public record WorkSession(
		long id,
		RoomSummary room,
		RoomCategory category,
		List<UserSummary> participants,
		Instant startedAt,
		Instant endedAt,
		long durationSeconds
	) {
	}

	public record PageMeta(
		int page,
		int size,
		long totalElements,
		int totalPages,
		boolean first,
		boolean last
	) {
	}

	public record WorkSessionPage(
		List<WorkSession> items,
		PageMeta page
	) {
	}

	public record CategoryWorkSummary(
		RoomCategory category,
		long durationSeconds
	) {
	}

	public record DailyWorkSummary(
		LocalDate date,
		long durationSeconds
	) {
	}

	public record WorkSessionSummary(
		long totalDurationSeconds,
		List<CategoryWorkSummary> byCategory,
		List<DailyWorkSummary> byDate
	) {
	}
}
