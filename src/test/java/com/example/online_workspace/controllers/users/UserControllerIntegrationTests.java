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
			.andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.items[0].name").value("Tomoko"))
			.andExpect(jsonPath("$.items[0].iconUrl").value("https://example.com/tomoko.png"))
			.andExpect(jsonPath("$.page.totalElements").value(1))
			.andExpect(jsonPath("$.page.totalPages").value(1))
			.andExpect(jsonPath("$.page.first").value(true))
			.andExpect(jsonPath("$.page.last").value(true));
	}

	@DisplayName("ブロック関係にあるユーザーは検索結果から除外される")
	@Test
	void excludesBlockedUsers() throws Exception {
		mockMvc.perform(get("/api/v1/users")
				.param("query", "tomo")
				.with(user("tom@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.items[0].name").value("Tomoko"))
			.andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@DisplayName("page/sizeでページングできる")
	@Test
	void paginatesUsers() throws Exception {
		mockMvc.perform(get("/api/v1/users")
				.param("query", "tom")
				.param("page", "1")
				.param("size", "1")
				.with(user("alice@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.items[0].name").value("Tomoaki"))
			.andExpect(jsonPath("$.page.totalElements").value(3))
			.andExpect(jsonPath("$.page.totalPages").value(3))
			.andExpect(jsonPath("$.page.first").value(false))
			.andExpect(jsonPath("$.page.last").value(false));
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

	@DisplayName("公開プロフィールを取得できる")
	@Test
	void getsPublicProfile() throws Exception {
		mockMvc.perform(get("/api/v1/users/{userId}", 3L)
				.with(user("tom@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(3))
			.andExpect(jsonPath("$.name").value("Alice"))
			.andExpect(jsonPath("$.isPublic").value(true))
			.andExpect(jsonPath("$.bio").value("alice bio"))
			.andExpect(jsonPath("$.friendship").value("FRIEND"));
	}

	@DisplayName("非公開プロフィールは名前とアイコンのみ返す")
	@Test
	void getsHiddenProfile() throws Exception {
		mockMvc.perform(get("/api/v1/users/{userId}", 2L)
				.with(user("alice@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("Tomoko"))
			.andExpect(jsonPath("$.isPublic").value(false))
			.andExpect(jsonPath("$.bio").doesNotExist())
			.andExpect(jsonPath("$.friendship").doesNotExist());
	}

	@DisplayName("ブロック関係ユーザーのプロフィール取得は403を返す")
	@Test
	void rejectsBlockedProfile() throws Exception {
		mockMvc.perform(get("/api/v1/users/{userId}", 6L)
				.with(user("tom@example.com")))
			.andExpect(status().isForbidden());
	}
}
