package com.example.online_workspace.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.online_workspace.models.WorkSession;
import com.example.online_workspace.repositories.WorkSessionRepository;

@ExtendWith(MockitoExtension.class)
class WorkSessionServiceTests {

	private static final long USER_ID = 10L;
	private static final long ROOM_ID = 20L;
	private static final long OTHER_ROOM_ID = 21L;
	private static final Instant STARTED_AT = Instant.parse("2026-08-04T09:00:00Z");
	private static final Instant ENDED_AT = Instant.parse("2026-08-04T10:00:00Z");

	@Mock
	private WorkSessionRepository repository;

	private WorkSessionService service;

	@BeforeEach
	void setUp() {
		service = new WorkSessionService(repository);
		when(repository.lockUserById(USER_ID)).thenReturn(USER_ID);
	}

	@Test
	void startsSessionWithRoomCategorySnapshot() {
		WorkSession started = session(1L, ROOM_ID, STARTED_AT, null);
		when(repository.findActiveByUserIdForUpdate(USER_ID)).thenReturn(null, started);
		when(repository.insertFromRoom(USER_ID, ROOM_ID, STARTED_AT)).thenReturn(1);

		assertThat(service.start(USER_ID, ROOM_ID, STARTED_AT)).isEqualTo(started);

		verify(repository).insertFromRoom(USER_ID, ROOM_ID, STARTED_AT);
	}

	@Test
	void repeatedStartForSameRoomIsIdempotent() {
		WorkSession active = session(1L, ROOM_ID, STARTED_AT, null);
		when(repository.findActiveByUserIdForUpdate(USER_ID)).thenReturn(active);

		assertThat(service.start(USER_ID, ROOM_ID, ENDED_AT)).isEqualTo(active);

		verify(repository, never()).endById(active.id(), ENDED_AT);
		verify(repository, never()).insertFromRoom(USER_ID, ROOM_ID, ENDED_AT);
	}

	@Test
	void startingAnotherRoomRecoversPreviousUnfinishedSession() {
		WorkSession unfinished = session(1L, ROOM_ID, STARTED_AT, null);
		WorkSession started = session(2L, OTHER_ROOM_ID, ENDED_AT, null);
		when(repository.findActiveByUserIdForUpdate(USER_ID)).thenReturn(unfinished, started);
		when(repository.endById(unfinished.id(), ENDED_AT)).thenReturn(1);
		when(repository.insertFromRoom(USER_ID, OTHER_ROOM_ID, ENDED_AT)).thenReturn(1);

		assertThat(service.start(USER_ID, OTHER_ROOM_ID, ENDED_AT)).isEqualTo(started);

		verify(repository).endById(unfinished.id(), ENDED_AT);
	}

	@Test
	void endsActiveSessionWhenUserLeavesItsRoom() {
		WorkSession active = session(1L, ROOM_ID, STARTED_AT, null);
		when(repository.findActiveByUserIdForUpdate(USER_ID)).thenReturn(active);
		when(repository.endById(active.id(), ENDED_AT)).thenReturn(1);

		assertThat(service.end(USER_ID, ROOM_ID, ENDED_AT)).isTrue();
	}

	@Test
	void repeatedEndIsIdempotent() {
		when(repository.findActiveByUserIdForUpdate(USER_ID)).thenReturn(null);

		assertThat(service.end(USER_ID, ROOM_ID, ENDED_AT)).isFalse();

		verify(repository, never()).endById(1L, ENDED_AT);
	}

	@Test
	void leavingAnotherRoomDoesNotEndCurrentSession() {
		WorkSession active = session(1L, OTHER_ROOM_ID, STARTED_AT, null);
		when(repository.findActiveByUserIdForUpdate(USER_ID)).thenReturn(active);

		assertThat(service.end(USER_ID, ROOM_ID, ENDED_AT)).isFalse();

		verify(repository, never()).endById(active.id(), ENDED_AT);
	}

	@Test
	void explicitlyRecoversUnfinishedSession() {
		WorkSession active = session(1L, ROOM_ID, STARTED_AT, null);
		when(repository.findActiveByUserIdForUpdate(USER_ID)).thenReturn(active);
		when(repository.endById(active.id(), ENDED_AT)).thenReturn(1);

		assertThat(service.recoverUnfinished(USER_ID, ENDED_AT)).isTrue();
	}

	@Test
	void rejectsEndBeforeSessionStart() {
		WorkSession active = session(1L, ROOM_ID, ENDED_AT, null);
		when(repository.findActiveByUserIdForUpdate(USER_ID)).thenReturn(active);

		assertThatThrownBy(() -> service.end(USER_ID, ROOM_ID, STARTED_AT))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("endedAt must not be before startedAt");
	}

	@Test
	void rejectsMissingUser() {
		when(repository.lockUserById(USER_ID)).thenReturn(null);

		assertThatThrownBy(() -> service.start(USER_ID, ROOM_ID, STARTED_AT))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("User not found: 10");
	}

	@Test
	void rejectsMissingRoom() {
		when(repository.findActiveByUserIdForUpdate(USER_ID)).thenReturn(null);
		when(repository.insertFromRoom(USER_ID, ROOM_ID, STARTED_AT)).thenReturn(0);

		assertThatThrownBy(() -> service.start(USER_ID, ROOM_ID, STARTED_AT))
			.isInstanceOf(NoSuchElementException.class)
			.hasMessage("Room not found: 20");
	}

	private WorkSession session(long id, long roomId, Instant startedAt, Instant endedAt) {
		return new WorkSession(
			id,
			USER_ID,
			roomId,
			30L,
			startedAt,
			endedAt,
			startedAt,
			startedAt
		);
	}
}
