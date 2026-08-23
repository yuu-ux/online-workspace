package com.example.online_workspace.services;

import java.time.Instant;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_workspace.models.WorkSession;
import com.example.online_workspace.repositories.users.UserRepository;
import com.example.online_workspace.repositories.WorkSessionRepository;

@Service
public class WorkSessionService {

	private final WorkSessionRepository workSessionRepository;
	private final UserRepository userRepository;

	public WorkSessionService(WorkSessionRepository workSessionRepository, UserRepository userRepository) {
		this.workSessionRepository = workSessionRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public WorkSession start(long userId, long roomId, Instant startedAt) {
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
	public boolean end(long userId, long roomId, Instant endedAt) {
		requireTimestamp(endedAt, "endedAt");
		lockUser(userId);

		WorkSession activeSession = workSessionRepository.findActiveByUserIdForUpdate(userId);
		if (activeSession == null || activeSession.roomId() != roomId) {
			return false;
		}

		endLockedSession(activeSession, endedAt);
		return true;
	}

	@Transactional
	public boolean recoverUnfinished(long userId, Instant recoveredAt) {
		if (userId <= 0) {
			throw new IllegalArgumentException("userId must be positive");
		}
		requireTimestamp(recoveredAt, "recoveredAt");
		lockUser(userId);

		WorkSession activeSession = workSessionRepository.findActiveByUserIdForUpdate(userId);
		if (activeSession == null) {
			return false;
		}

		endLockedSession(activeSession, recoveredAt);
		return true;
	}

	private void lockUser(long userId) {
		if (!userRepository.lockById(userId)) {
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

	private void requireTimestamp(Instant timestamp, String name) {
		if (timestamp == null) {
			throw new IllegalArgumentException(name + " must not be null");
		}
	}
}
