package com.example.online_workspace.controllers.messages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import com.example.online_workspace.services.ChatMessageService.ChatMessageEvent;

@SpringBootTest(properties =
	"spring.datasource.url=jdbc:h2:mem:chat-message-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
)
@AutoConfigureMockMvc
@Sql("/chat-message-api-test.sql")
class ChatMessageControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private SimpMessagingTemplate messagingTemplate;

	@Test
	void createsAndPublishesMessageToActiveRoomMembers() throws Exception {
		mockMvc.perform(post("/api/v1/rooms/10/messages")
				.with(user("member@example.com"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"content\":\"こんにちは\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.roomId").value(10))
			.andExpect(jsonPath("$.sender.id").value(1))
			.andExpect(jsonPath("$.sender.name").value("参加者"))
			.andExpect(jsonPath("$.sender.iconUrl").value("https://example.com/member.png"))
			.andExpect(jsonPath("$.content").value("こんにちは"))
			.andExpect(jsonPath("$.sentAt").exists());

		assertThat(jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM messages WHERE room_id = 10 AND user_id = 1 AND content = 'こんにちは'",
			Integer.class
		)).isOne();
		verifyPublishedTo("member@example.com");
		verifyPublishedTo("member2@example.com");
		verifyNoMoreInteractions(messagingTemplate);
	}

	@Test
	void listsNewestMessagesForRoomMembers() throws Exception {
		mockMvc.perform(get("/api/v1/rooms/10/messages")
				.with(user("member@example.com"))
				.param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].id").value(101))
			.andExpect(jsonPath("$.items[0].sender.name").value("参加者2"))
			.andExpect(jsonPath("$.items[0].content").value("新しいメッセージ"))
			.andExpect(jsonPath("$.page.totalElements").value(2))
			.andExpect(jsonPath("$.page.totalPages").value(2))
			.andExpect(jsonPath("$.page.first").value(true))
			.andExpect(jsonPath("$.page.last").value(false));
	}

	@Test
	void rejectsChatFromNonMembers() throws Exception {
		mockMvc.perform(post("/api/v1/rooms/10/messages")
				.with(user("outsider@example.com"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"content\":\"送信不可\"}"))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ROOM_MEMBERSHIP_REQUIRED"));
		mockMvc.perform(get("/api/v1/rooms/10/messages").with(user("outsider@example.com")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("ROOM_MEMBERSHIP_REQUIRED"));

		verifyNoInteractions(messagingTemplate);
	}

	@Test
	void rejectsBlankAndOversizedMessages() throws Exception {
		for (String content : new String[] {"   ", "a".repeat(501)}) {
			mockMvc.perform(post("/api/v1/rooms/10/messages")
					.with(user("member@example.com"))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"content\":\"%s\"}".formatted(content)))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("content"));
		}

		verifyNoInteractions(messagingTemplate);
	}

	@Test
	void returnsNotFoundForMissingRoom() throws Exception {
		mockMvc.perform(get("/api/v1/rooms/999/messages").with(user("member@example.com")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
	}

	private void verifyPublishedTo(String email) {
		verify(messagingTemplate).convertAndSendToUser(
			eq(email),
			eq("/queue/rooms/10/messages"),
			argThat(payload -> payload instanceof ChatMessageEvent event
				&& "chat:message".equals(event.type())
				&& "こんにちは".equals(event.payload().content()))
		);
	}
}
