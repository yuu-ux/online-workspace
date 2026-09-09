package com.example.online_workspace.controllers.users;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/user-search-api-test.sql")
class UserControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@DisplayName("名前の部分一致でユーザー検索できる")
	@Test
	void searchesUsersByPartialName() throws Exception {
		mockMvc.perform(get("/api/v1/users")
				.param("query", "tom")
				.with(user("tom@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", hasSize(2)))
			.andExpect(jsonPath("$.items[0].name").value("Tom"))
			.andExpect(jsonPath("$.items[0].iconUrl").value("https://example.com/tom.png"))
			.andExpect(jsonPath("$.items[1].name").value("Tomoko"))
			.andExpect(jsonPath("$.page.totalElements").value(2))
			.andExpect(jsonPath("$.page.totalPages").value(1))
			.andExpect(jsonPath("$.page.first").value(true))
			.andExpect(jsonPath("$.page.last").value(true));
	}

	@DisplayName("page/sizeでページングできる")
	@Test
	void paginatesUsers() throws Exception {
		mockMvc.perform(get("/api/v1/users")
				.param("query", "tom")
				.param("page", "1")
				.param("size", "1")
				.with(user("tom@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.items[0].name").value("Tomoko"))
			.andExpect(jsonPath("$.page.totalElements").value(2))
			.andExpect(jsonPath("$.page.totalPages").value(2))
			.andExpect(jsonPath("$.page.first").value(false))
			.andExpect(jsonPath("$.page.last").value(true));
	}

	@DisplayName("query未指定は400を返す")
	@Test
	void rejectsMissingQuery() throws Exception {
		mockMvc.perform(get("/api/v1/users").with(user("tom@example.com")))
			.andExpect(status().isBadRequest());
	}

	@DisplayName("存在しない認証ユーザーは401を返す")
	@Test
	void rejectsUnknownAuthenticatedUser() throws Exception {
		mockMvc.perform(get("/api/v1/users")
				.param("query", "tom")
				.with(user("unknown@example.com")))
			.andExpect(status().isUnauthorized());
	}
}
