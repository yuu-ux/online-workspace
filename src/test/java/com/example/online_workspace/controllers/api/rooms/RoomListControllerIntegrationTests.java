package com.example.online_workspace.controllers.api.rooms;

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
@Sql("/room-list-test-data.sql")
class RoomListControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@DisplayName("公開中のルームだけを人数と参加可否付きで返す")
	@Test
	void listsOnlyOpenPublicRoomsWithJoinability() throws Exception {
		mockMvc.perform(get("/api/v1/rooms").with(user("viewer@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", hasSize(3)))
			.andExpect(jsonPath("$.items[0].id").value(14))
			.andExpect(jsonPath("$.items[0].currentMembers").value(2))
			.andExpect(jsonPath("$.items[0].joinable").value(false))
			.andExpect(jsonPath("$.items[0].joinRestriction").value("FULL"))
			.andExpect(jsonPath("$.items[1].id").value(13))
			.andExpect(jsonPath("$.items[1].joinable").value(false))
			.andExpect(jsonPath("$.items[1].joinRestriction").value("BLOCKED"))
			.andExpect(jsonPath("$.items[2].name").value("参加可能"))
			.andExpect(jsonPath("$.items[2].category.name").value("開発"))
			.andExpect(jsonPath("$.items[2].workStyle").value("FOCUS"))
			.andExpect(jsonPath("$.items[2].currentMembers").value(1))
			.andExpect(jsonPath("$.items[2].createdAt").exists())
			.andExpect(jsonPath("$.items[2].joinable").value(true))
			.andExpect(jsonPath("$.page.totalElements").value(3))
			.andExpect(jsonPath("$.page.totalPages").value(1));
	}

	@DisplayName("作業スタイルで絞り込みページングできる")
	@Test
	void filtersAndPaginatesRooms() throws Exception {
		mockMvc.perform(get("/api/v1/rooms")
				.param("categoryId", "1")
				.param("workStyle", "FOCUS")
				.param("page", "1")
				.param("size", "1")
				.with(user("viewer@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items", hasSize(1)))
			.andExpect(jsonPath("$.items[0].id").value(10))
			.andExpect(jsonPath("$.page.totalElements").value(2))
			.andExpect(jsonPath("$.page.totalPages").value(2))
			.andExpect(jsonPath("$.page.first").value(false))
			.andExpect(jsonPath("$.page.last").value(true));
	}

	@DisplayName("不正な一覧パラメーターは400を返す")
	@Test
	void rejectsInvalidParameters() throws Exception {
		mockMvc.perform(get("/api/v1/rooms")
				.param("size", "101")
				.with(user("viewer@example.com")))
			.andExpect(status().isBadRequest());
	}

	@DisplayName("DBに存在しない認証ユーザーは401を返す")
	@Test
	void rejectsUnknownAuthenticatedUser() throws Exception {
		mockMvc.perform(get("/api/v1/rooms").with(user("unknown@example.com")))
			.andExpect(status().isUnauthorized());
	}
}
