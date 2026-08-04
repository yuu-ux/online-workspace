package com.example.online_workspace.services;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_workspace.models.WorkSession;
import com.example.online_workspace.repositories.WorkSessionRepository;

@Service
public class WorkSessionService {

	private final WorkSessionRepository workSessionRepository;

	public WorkSessionService(WorkSessionRepository workSessionRepository) {
		this.workSessionRepository = workSessionRepository;
	}

	@Transactional
	public WorkSession start(long userId, long roomId, Instant startedAt) {
		validateIdentifiers(userId, roomId);
		requireTimestamp(startedAt, "startedAt");
		lockUser(userId);

		WorkSession activeSession = workSessionRepository.findActiveByUserIdForUpdate(userId);
		if (activeSession != null && activeSession.roomId() == roomId) {
			return activeSession;
		}

		if (activeSession != null) {
			endLockedSession(activeSession, startedAt);
		}

		int insertedRows = workSessionRepository.insertFromRoom(userId, roomId, startedAt);
		if (insertedRows != 1) {
			throw new NoSuchElementException("Room not found: " + roomId);
		}

		WorkSession startedSession = workSessionRepository.findActiveByUserIdForUpdate(userId);
		if (startedSession == null) {
			throw new IllegalStateException("The work session was not created");
		}
		return startedSession;
	}

	@Transactional
	public Optional<WorkSession> end(long userId, long roomId, Instant endedAt) {
		validateIdentifiers(userId, roomId);
		requireTimestamp(endedAt, "endedAt");
		lockUser(userId);

		WorkSession activeSession = workSessionRepository.findActiveByUserIdForUpdate(userId);
		if (activeSession == null || activeSession.roomId() != roomId) {
			return Optional.empty();
		}

		endLockedSession(activeSession, endedAt);
		return Optional.of(endedCopy(activeSession, endedAt));
	}

	@Transactional
	public Optional<WorkSession> recoverUnfinished(long userId, Instant recoveredAt) {
		if (userId <= 0) {
			throw new IllegalArgumentException("userId must be positive");
		}
		requireTimestamp(recoveredAt, "recoveredAt");
		lockUser(userId);

		WorkSession activeSession = workSessionRepository.findActiveByUserIdForUpdate(userId);
		if (activeSession == null) {
			return Optional.empty();
		}

		endLockedSession(activeSession, recoveredAt);
		return Optional.of(endedCopy(activeSession, recoveredAt));
	}

	private void lockUser(long userId) {
		if (workSessionRepository.lockUserById(userId) == null) {
			throw new NoSuchElementException("User not found: " + userId);
		}
	}

	private void endLockedSession(WorkSession session, Instant endedAt) {
		if (endedAt.isBefore(session.startedAt())) {
			throw new IllegalArgumentException("endedAt must not be before startedAt");
		}
		if (workSessionRepository.endById(session.id(), endedAt) != 1) {
			throw new IllegalStateException("The active work session could not be ended");
		}
	}

	private WorkSession endedCopy(WorkSession session, Instant endedAt) {
		return new WorkSession(
			session.id(),
			session.userId(),
			session.roomId(),
			session.categoryId(),
			session.startedAt(),
			endedAt,
			session.createdAt(),
			session.updatedAt()
		);
	}

	private void validateIdentifiers(long userId, long roomId) {
		if (userId <= 0) {
			throw new IllegalArgumentException("userId must be positive");
		}
		if (roomId <= 0) {
			throw new IllegalArgumentException("roomId must be positive");
		}
	}

	private void requireTimestamp(Instant timestamp, String name) {
		if (timestamp == null) {
			throw new IllegalArgumentException(name + " must not be null");
		}
	}
}
