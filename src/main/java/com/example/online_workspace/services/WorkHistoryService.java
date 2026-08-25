package com.example.online_workspace.services;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.example.online_workspace.models.WorkHistory.CategoryWorkSummary;
import com.example.online_workspace.models.WorkHistory.DailyWorkSummary;
import com.example.online_workspace.models.WorkHistory.PageMeta;
import com.example.online_workspace.models.WorkHistory.RoomCategory;
import com.example.online_workspace.models.WorkHistory.RoomSummary;
import com.example.online_workspace.models.WorkHistory.UserSummary;
import com.example.online_workspace.models.WorkHistory.WorkSession;
import com.example.online_workspace.models.WorkHistory.WorkSessionPage;
import com.example.online_workspace.models.WorkHistory.WorkSessionSummary;
import com.example.online_workspace.repositories.WorkHistoryRepository;
import com.example.online_workspace.repositories.WorkHistoryRepository.SessionRow;

@Service
public class WorkHistoryService {

	private final WorkHistoryRepository workHistoryRepository;

	public WorkHistoryService(WorkHistoryRepository workHistoryRepository) {
		this.workHistoryRepository = workHistoryRepository;
	}

	public WorkSessionPage findSessions(
		String email,
		LocalDate from,
		LocalDate to,
		Long categoryId,
		int page,
		int size
	) {
		validateRequest(from, to, categoryId, page, size);
		long userId = requireUserId(email);
		Instant fromInclusive = startOfDay(from);
		Instant toExclusive = endOfDayExclusive(to);
		long totalElements = workHistoryRepository.countSessions(
			userId,
			fromInclusive,
			toExclusive,
			categoryId
		);
		long offset = Math.multiplyExact((long) page, size);
		List<WorkSession> sessions = workHistoryRepository.findSessions(
			userId,
			fromInclusive,
			toExclusive,
			categoryId,
			size,
			offset
		).stream().map(row -> toWorkSession(userId, row)).toList();

		int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);
		PageMeta pageMeta = new PageMeta(
			page,
			size,
			totalElements,
			totalPages,
			page == 0,
			totalPages == 0 || page >= totalPages - 1
		);
		return new WorkSessionPage(sessions, pageMeta);
	}

	public WorkSessionSummary summarize(String email, LocalDate from, LocalDate to) {
		validateRequest(from, to, null, 0, 1);
		long userId = requireUserId(email);
		Instant fromInclusive = startOfDay(from);
		Instant toExclusive = endOfDayExclusive(to);

		List<CategoryWorkSummary> byCategory = workHistoryRepository.summarizeByCategory(
			userId,
			fromInclusive,
			toExclusive
		).stream().map(row -> new CategoryWorkSummary(
			new RoomCategory(
				row.categoryId(),
				row.categoryName(),
				row.categoryDescription(),
				row.categorySortOrder()
			),
			row.durationSeconds()
		)).toList();
		long totalDurationSeconds = byCategory.stream()
			.mapToLong(CategoryWorkSummary::durationSeconds)
			.sum();
		List<DailyWorkSummary> byDate = workHistoryRepository.summarizeByDate(
			userId,
			fromInclusive,
			toExclusive
		).stream().map(row -> new DailyWorkSummary(row.workDate(), row.durationSeconds())).toList();

		return new WorkSessionSummary(totalDurationSeconds, byCategory, byDate);
	}

	private WorkSession toWorkSession(long userId, SessionRow row) {
		RoomCategory category = new RoomCategory(
			row.categoryId(),
			row.categoryName(),
			row.categoryDescription(),
			row.categorySortOrder()
		);
		UserSummary creator = new UserSummary(
			row.creatorId(),
			row.creatorName(),
			row.creatorIconUrl()
		);
		String joinRestriction = joinRestriction(userId, row);
		RoomSummary room = new RoomSummary(
			row.roomId(),
			row.roomName(),
			category,
			row.workStyle(),
			row.maxMembers(),
			row.currentMembers(),
			row.visibility(),
			row.roomStatus(),
			creator,
			joinRestriction == null,
			joinRestriction,
			row.roomCreatedAt()
		);
		List<UserSummary> participants = workHistoryRepository.findParticipants(
			row.roomId(),
			row.startedAt(),
			row.endedAt()
		).stream().map(participant -> new UserSummary(
			participant.id(),
			participant.name(),
			participant.iconUrl()
		)).toList();

		return new WorkSession(
			row.sessionId(),
			room,
			category,
			participants,
			row.startedAt(),
			row.endedAt(),
			row.durationSeconds()
		);
	}

	private String joinRestriction(long userId, SessionRow row) {
		if (!"OPEN".equals(row.roomStatus())) {
			return "CLOSED";
		}
		if (row.blocked()) {
			return "BLOCKED";
		}
		if (row.currentMembers() >= row.maxMembers()) {
			return "FULL";
		}
		return switch (row.visibility()) {
			case "FRIENDS_ONLY" ->
				userId == row.creatorId() || row.creatorFriend() ? null : "FRIEND_REQUIRED";
			default -> null;
		};
	}

	private long requireUserId(String email) {
		if (email == null || email.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
		}
		Long userId = workHistoryRepository.findActiveUserIdByEmail(email);
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user was not found");
		}
		return userId;
	}

	private void validateRequest(LocalDate from, LocalDate to, Long categoryId, int page, int size) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "from must not be after to");
		}
		if (categoryId != null && categoryId <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryId must be positive");
		}
		if (page < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must not be negative");
		}
		if (size < 1 || size > 100) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and 100");
		}
	}

	private Instant startOfDay(LocalDate date) {
		return date == null ? null : date.atStartOfDay(ZoneOffset.UTC).toInstant();
	}

	private Instant endOfDayExclusive(LocalDate date) {
		if (date == null) {
			return null;
		}
		try {
			return date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
		}
		catch (RuntimeException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "to is outside the supported range", exception);
		}
	}
}
