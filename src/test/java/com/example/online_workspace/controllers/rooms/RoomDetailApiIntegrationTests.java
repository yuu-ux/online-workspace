package com.example.online_workspace.controllers.rooms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/room-detail-api-test.sql")
class RoomDetailApiIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void getRoomDetail() throws Exception {
		mockMvc.perform(get("/api/v1/rooms/10").with(user("viewer@example.com")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(10))
			.andExpect(jsonPath("$.name").value("参加可能"))
			.andExpect(jsonPath("$.currentMembers").value(3))
			.andExpect(jsonPath("$.joinable").value(false))
			.andExpect(jsonPath("$.joinRestriction").value("FULL"))
			.andExpect(jsonPath("$.member").value(true));
	}

	@Test
	void updateRoomByCreator() throws Exception {
		mockMvc.perform(put("/api/v1/rooms/10")
				.with(user("creator@example.com"))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "name": "更新後ルーム",
					  "description": "更新説明",
					  "categoryId": 2,
					  "workStyle": "CHAT_OK",
					  "maxMembers": 4
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("更新後ルーム"))
			.andExpect(jsonPath("$.description").value("更新説明"))
			.andExpect(jsonPath("$.category.id").value(2))
			.andExpect(jsonPath("$.workStyle").value("CHAT_OK"))
			.andExpect(jsonPath("$.maxMembers").value(4));
	}

	@Test
	void rejectUpdateByNonCreator() throws Exception {
		mockMvc.perform(put("/api/v1/rooms/10")
				.with(user("viewer@example.com"))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "name": "更新後ルーム",
					  "description": "更新説明",
					  "categoryId": 1,
					  "workStyle": "FOCUS",
					  "maxMembers": 4
					}
					"""))
			.andExpect(status().isForbidden());
	}

	@Test
	void rejectUpdateWhenMembersExceedNewLimit() throws Exception {
		mockMvc.perform(put("/api/v1/rooms/10")
				.with(user("creator@example.com"))
				.with(csrf())
				.contentType(APPLICATION_JSON)
				.content("""
					{
					  "name": "更新後ルーム",
					  "description": "更新説明",
					  "categoryId": 1,
					  "workStyle": "FOCUS",
					  "maxMembers": 2
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("MAX_MEMBERS_BELOW_CURRENT"));
	}

	@Test
	void closeRoomByCreator() throws Exception {
		mockMvc.perform(delete("/api/v1/rooms/10")
				.with(user("creator@example.com"))
				.with(csrf()))
			.andExpect(status().isNoContent());

		String status = jdbcTemplate.queryForObject(
			"""
			SELECT rs.code
			FROM rooms r
			JOIN room_statuses rs ON rs.id = r.status_id
			WHERE r.id = 10
			""",
			String.class
		);
		assertThat(status).isEqualTo("CLOSED");
	}

	@Test
	void rejectCloseClosedRoom() throws Exception {
		mockMvc.perform(delete("/api/v1/rooms/11")
				.with(user("creator@example.com"))
				.with(csrf()))
			.andExpect(status().isConflict());
	}
}
