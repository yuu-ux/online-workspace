package com.example.online_workspace.services;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_workspace.exceptions.ApiException;
import com.example.online_workspace.repositories.WorkHistoryRepository;
import com.example.online_workspace.repositories.WorkHistoryRepository.WorkHistoryRow;

@Service
public class WorkHistoryService {

	private final WorkHistoryRepository repository;

	public WorkHistoryService(WorkHistoryRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public Result list(String email, int page, int size) {
		Long userId = repository.findActiveUserIdByEmail(email);
		if (userId == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。");
		}
		long totalElements = repository.countByUserId(userId);
		long offset = (long) page * size;
		List<WorkHistory> items = offset >= totalElements
			? List.of()
			: repository.findByUserId(userId, size, offset).stream().map(this::toItem).toList();
		return new Result(items, totalElements);
	}

	private WorkHistory toItem(WorkHistoryRow row) {
		Instant end = row.leftAt() == null ? Instant.now() : row.leftAt();
		long minutes = Math.max(0, Duration.between(row.joinedAt(), end).toMinutes());
		return new WorkHistory(
			row.roomId(),
			row.roomName(),
			row.joinedAt(),
			row.leftAt(),
			minutes
		);
	}

	public record Result(List<WorkHistory> items, long totalElements) {
	}

	public record WorkHistory(
		long roomId,
		String roomName,
		Instant joinedAt,
		Instant leftAt,
		long durationMinutes
	) {
	}
}
