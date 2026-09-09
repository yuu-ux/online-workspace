package com.example.online_workspace.controllers.workhistory;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/work-history-api-test.sql")
class WorkHistoryControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void listReturnsHistoryItems() throws Exception {
		mockMvc.perform(get("/api/v1/workhistories?page=0&size=20").with(user("history@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(2))
			.andExpect(jsonPath("$.items[0].roomId").value(11))
			.andExpect(jsonPath("$.items[0].roomName").value("雑談ルーム"))
			.andExpect(jsonPath("$.items[1].durationMinutes").value(60))
			.andExpect(jsonPath("$.page.totalElements").value(2))
			.andExpect(jsonPath("$.page.totalPages").value(1))
			.andExpect(jsonPath("$.page.first").value(true))
			.andExpect(jsonPath("$.page.last").value(true));
	}

	@Test
	void listRejectsSuspendedUser() throws Exception {
		mockMvc.perform(get("/api/v1/workhistories").with(user("suspended@example.com")))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}
}
