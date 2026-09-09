package com.example.online_workspace.controllers.rooms;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/room-category-test-data.sql")
class RoomCategoryControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@DisplayName("利用中のカテゴリだけを表示順で返す")
	@Test
	void listsActiveCategoriesInDisplayOrder() throws Exception {
		mockMvc.perform(get("/api/v1/room-categories").with(authenticatedAs("user@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].id").value(3))
			.andExpect(jsonPath("$[0].name").value("読書"))
			.andExpect(jsonPath("$[0].description").value("本を読む"))
			.andExpect(jsonPath("$[0].sortOrder").value(10))
			.andExpect(jsonPath("$[1].id").value(1));
	}

	@DisplayName("未認証では401を返す")
	@Test
	void rejectsUnauthenticatedRequest() throws Exception {
		mockMvc.perform(get("/api/v1/room-categories"))
			.andExpect(status().isUnauthorized());
	}

	private static RequestPostProcessor authenticatedAs(String username) {
		return authentication(UsernamePasswordAuthenticationToken.authenticated(username, null, java.util.List.of()));
	}
}
