package com.example.online_workspace.controllers.api.rooms;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/room-create-api-test.sql")
class RoomControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@WithMockUser(username = "creator@example.com")
	void createsRoomAndJoinsCreator() throws Exception {
		mockMvc.perform(post("/api/v1/rooms")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody(12, "PUBLIC")))
			.andExpect(status().isCreated())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.name").value("集中ルーム"))
			.andExpect(jsonPath("$.description").value("一緒に作業します"))
			.andExpect(jsonPath("$.category.id").value(1))
			.andExpect(jsonPath("$.category.name").value("開発"))
			.andExpect(jsonPath("$.workStyle").value("FOCUS"))
			.andExpect(jsonPath("$.maxMembers").value(12))
			.andExpect(jsonPath("$.currentMembers").value(1))
			.andExpect(jsonPath("$.visibility").value("PUBLIC"))
			.andExpect(jsonPath("$.status").value("OPEN"))
			.andExpect(jsonPath("$.createdBy.id").value(1))
			.andExpect(jsonPath("$.member").value(true));

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rooms", Integer.class)).isOne();
		assertThat(jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM room_members WHERE room_id = 1 AND user_id = 1 AND left_at IS NULL",
			Integer.class
		)).isOne();
	}

	@ParameterizedTest
	@ValueSource(ints = {1, 13})
	@WithMockUser(username = "creator@example.com")
	void rejectsMemberLimitOutsideTwoThroughTwelve(int maxMembers) throws Exception {
		mockMvc.perform(post("/api/v1/rooms")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody(maxMembers, "PUBLIC")))
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.fieldErrors[0].field").value("maxMembers"))
			.andExpect(jsonPath("$.fieldErrors[0].code").value("RANGE"));

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rooms", Integer.class)).isZero();
	}

	@Test
	@WithMockUser(username = "creator@example.com")
	void acceptsSupportedVisibilities() throws Exception {
		for (String visibility : new String[] {"PUBLIC", "FRIENDS_ONLY"}) {
			mockMvc.perform(post("/api/v1/rooms")
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody(2, visibility)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.visibility").value(visibility))
				.andExpect(jsonPath("$.member").value(true));
		}

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rooms", Integer.class)).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM room_members", Integer.class)).isEqualTo(2);
	}

	@Test
	@WithMockUser(username = "creator@example.com")
	void rejectsInviteOnlyVisibility() throws Exception {
		mockMvc.perform(post("/api/v1/rooms")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody(2, "INVITE_ONLY")))
			.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(username = "creator@example.com")
	void rejectsInactiveCategoryWithoutCreatingRoom() throws Exception {
		mockMvc.perform(post("/api/v1/rooms")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody(2, "PUBLIC").replace("\"categoryId\": 1", "\"categoryId\": 2")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_CATEGORY"));

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rooms", Integer.class)).isZero();
	}

	private String requestBody(int maxMembers, String visibility) {
		return """
			{
			  "name": "集中ルーム",
			  "description": "一緒に作業します",
			  "categoryId": 1,
			  "workStyle": "FOCUS",
			  "maxMembers": %d,
			  "visibility": "%s"
			}
			""".formatted(maxMembers, visibility);
	}
}
