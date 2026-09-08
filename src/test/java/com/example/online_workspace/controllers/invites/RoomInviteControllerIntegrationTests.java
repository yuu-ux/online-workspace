package com.example.online_workspace.controllers.invites;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
@Sql("/room-invite-api-test.sql")
class RoomInviteControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void createInviteByRoomMember() throws Exception {
		mockMvc.perform(post("/api/v1/rooms/10/invites")
				.with(user("creator@example.com"))
				.with(csrf()))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andExpect(jsonPath("$.inviteUrl").exists())
			.andExpect(jsonPath("$.expiresAt").exists());
	}

	@Test
	void createInviteRejectsNonMember() throws Exception {
		mockMvc.perform(post("/api/v1/rooms/10/invites")
				.with(user("viewer@example.com"))
				.with(csrf()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void joinByValidInvite() throws Exception {
		mockMvc.perform(post("/api/v1/room-invites/validtoken/join")
				.with(user("member@example.com"))
				.with(csrf()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.roomId").value(10))
			.andExpect(jsonPath("$.membershipId").isNumber())
			.andExpect(jsonPath("$.joinedAt").exists());

		Integer activeMembershipCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM room_members WHERE room_id = 10 AND user_id = 2 AND left_at IS NULL",
			Integer.class
		);
		assertThat(activeMembershipCount).isEqualTo(1);

		Integer invalidatedCount = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM room_invites WHERE token = 'validtoken' AND invalidated_at IS NOT NULL",
			Integer.class
		);
		assertThat(invalidatedCount).isEqualTo(1);
	}

	@Test
	void joinRejectsExpiredInvite() throws Exception {
		mockMvc.perform(post("/api/v1/room-invites/expiredtoken/join")
				.with(user("member@example.com"))
				.with(csrf()))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("INVITE_EXPIRED"));
	}
}
