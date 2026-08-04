package com.example.online_workspace.controllers;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.online_workspace.repositories.WorkHistoryRepository;
import com.example.online_workspace.repositories.WorkHistoryRepository.CategoryDurationRow;
import com.example.online_workspace.repositories.WorkHistoryRepository.DailyDurationRow;
import com.example.online_workspace.repositories.WorkHistoryRepository.ParticipantRow;
import com.example.online_workspace.repositories.WorkHistoryRepository.SessionRow;

@SpringBootTest
@AutoConfigureMockMvc
class WorkHistoryControllerTests {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WorkHistoryRepository repository;

	@Test
	void exposesWorkHistoryUsingTheAuthenticatedUsersEmail() throws Exception {
		Instant startedAt = Instant.parse("2026-08-03T01:00:00Z");
		Instant endedAt = Instant.parse("2026-08-03T02:30:00Z");
		when(repository.findActiveUserIdByEmail("me@example.com")).thenReturn(1L);
		when(repository.countSessions(1L, null, null, null)).thenReturn(1L);
		when(repository.findSessions(1L, null, null, null, 20, 0L)).thenReturn(List.of(
			new SessionRow(
				10L, 20L, "朝活ルーム", 3L, "資格勉強", "試験勉強", 2,
				"FOCUS", 8, 2, "PUBLIC", "OPEN", 30L, "Alice", null,
				Instant.parse("2026-08-01T00:00:00Z"), startedAt, endedAt, 5400L
			)
		));
		when(repository.findParticipants(20L, startedAt, endedAt)).thenReturn(List.of(
			new ParticipantRow(1L, "Me", null)
		));

		mockMvc.perform(get("/api/v1/work-sessions").with(user("me@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].room.name").value("朝活ルーム"))
			.andExpect(jsonPath("$.items[0].category.name").value("資格勉強"))
			.andExpect(jsonPath("$.items[0].participants[0].name").value("Me"))
			.andExpect(jsonPath("$.items[0].durationSeconds").value(5400))
			.andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@Test
	void exposesWorkHistorySummary() throws Exception {
		when(repository.findActiveUserIdByEmail("me@example.com")).thenReturn(1L);
		when(repository.summarizeByCategory(1L, null, null)).thenReturn(List.of(
			new CategoryDurationRow(2L, "開発", "プログラミング", 1, 7200L)
		));
		when(repository.summarizeByDate(1L, null, null)).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/work-sessions/summary").with(user("me@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalDurationSeconds").value(7200))
			.andExpect(jsonPath("$.byCategory[0].category.name").value("開発"));
	}

	@Test
	void rejectsInvalidDateRanges() throws Exception {
		mockMvc.perform(get("/api/v1/work-sessions")
				.with(user("me@example.com"))
				.param("from", "2026-08-02")
				.param("to", "2026-08-01"))
			.andExpect(status().isBadRequest());
	}
}
