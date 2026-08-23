package com.example.online_workspace.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.example.online_workspace.repositories.WorkHistoryRepository;
import com.example.online_workspace.repositories.WorkHistoryRepository.CategoryDurationRow;
import com.example.online_workspace.repositories.WorkHistoryRepository.DailyDurationRow;
import com.example.online_workspace.repositories.WorkHistoryRepository.ParticipantRow;
import com.example.online_workspace.repositories.WorkHistoryRepository.SessionRow;

@ExtendWith(MockitoExtension.class)
class WorkHistoryServiceTests {

	@Mock
	private WorkHistoryRepository repository;

	private WorkHistoryService service;

	@BeforeEach
	void setUp() {
		service = new WorkHistoryService(repository);
	}

	@Test
	void returnsHistoryWithRoomCategoryParticipantsAndPageMetadata() {
		Instant startedAt = Instant.parse("2026-08-03T01:00:00Z");
		Instant endedAt = Instant.parse("2026-08-03T02:30:00Z");
		SessionRow row = new SessionRow(
			10L,
			20L,
			"朝活ルーム",
			3L,
			"資格勉強",
			"試験勉強",
			2,
			"FOCUS",
			8,
			2,
			"FRIENDS_ONLY",
			"OPEN",
			30L,
			"Alice",
			null,
			Instant.parse("2026-08-01T00:00:00Z"),
			startedAt,
			endedAt,
			5400L,
			false,
			true
		);

		when(repository.findActiveUserIdByEmail("me@example.com")).thenReturn(1L);
		when(repository.countSessions(1L, null, null, null)).thenReturn(1L);
		when(repository.findSessions(1L, null, null, null, 20, 0L)).thenReturn(List.of(row));
		when(repository.findParticipants(20L, startedAt, endedAt)).thenReturn(List.of(
			new ParticipantRow(1L, "Me", null),
			new ParticipantRow(2L, "Tom", "https://example.com/tom.png")
		));

		var result = service.findSessions("me@example.com", null, null, null, 0, 20);

		assertThat(result.page().totalElements()).isEqualTo(1);
		assertThat(result.page().first()).isTrue();
		assertThat(result.page().last()).isTrue();
		assertThat(result.items()).hasSize(1);
		assertThat(result.items().getFirst().room().name()).isEqualTo("朝活ルーム");
		assertThat(result.items().getFirst().category().name()).isEqualTo("資格勉強");
		assertThat(result.items().getFirst().participants()).extracting("name")
			.containsExactly("Me", "Tom");
		assertThat(result.items().getFirst().durationSeconds()).isEqualTo(5400L);
		assertThat(result.items().getFirst().room().joinable()).isTrue();
	}

	@Test
	void returnsTotalCategoryAndDailySummaries() {
		Instant from = Instant.parse("2026-08-01T00:00:00Z");
		Instant to = Instant.parse("2026-09-01T00:00:00Z");
		when(repository.findActiveUserIdByEmail("me@example.com")).thenReturn(1L);
		when(repository.summarizeByCategory(1L, from, to)).thenReturn(List.of(
			new CategoryDurationRow(2L, "開発", "プログラミング", 1, 7200L),
			new CategoryDurationRow(3L, "読書", "本を読む", 2, 1800L)
		));
		when(repository.summarizeByDate(1L, from, to)).thenReturn(List.of(
			new DailyDurationRow(LocalDate.of(2026, 8, 3), 9000L)
		));

		var result = service.summarize(
			"me@example.com",
			LocalDate.of(2026, 8, 1),
			LocalDate.of(2026, 8, 31)
		);

		assertThat(result.totalDurationSeconds()).isEqualTo(9000L);
		assertThat(result.byCategory()).extracting(summary -> summary.category().name())
			.containsExactly("開発", "読書");
		assertThat(result.byDate().getFirst().date()).isEqualTo(LocalDate.of(2026, 8, 3));
	}

	@Test
	void rejectsAnInvalidDateRange() {
		assertThatThrownBy(() -> service.findSessions(
			"me@example.com",
			LocalDate.of(2026, 8, 2),
			LocalDate.of(2026, 8, 1),
			null,
			0,
			20
		)).isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("from must not be after to");
	}

	@Test
	void rejectsAnAuthenticatedPrincipalWithoutAnActiveUser() {
		when(repository.findActiveUserIdByEmail("missing@example.com")).thenReturn(null);

		assertThatThrownBy(() -> service.summarize("missing@example.com", null, null))
			.isInstanceOf(ResponseStatusException.class)
			.hasMessageContaining("Authenticated user was not found");
	}
}
