package com.example.online_workspace;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@SpringBootTest(properties = "app.management.api-key=test-management-key")
@AutoConfigureMockMvc
class OperationsEndpointsIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApplicationEventPublisher events;

	@Test
	void operationsEndpointsRequireApiKey() throws Exception {
		mockMvc.perform(get("/health"))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/metrics"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	void healthAndPrometheusMetricsAreAvailable() throws Exception {
		mockMvc.perform(get("/health").header("X-API-Key", "test-management-key"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"))
			.andExpect(jsonPath("$.components.db.status").value("UP"));

		mockMvc.perform(get("/metrics").header("X-API-Key", "test-management-key"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("http_server_requests_seconds")))
			.andExpect(content().string(containsString("jvm_memory_used_bytes")))
			.andExpect(content().string(containsString("hikaricp_connections")));
	}

	@Test
	void websocketConnectionsArePublished() throws Exception {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
		accessor.setSessionId("test-session");
		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

		events.publishEvent(new SessionConnectEvent(this, message));
		mockMvc.perform(get("/metrics").header("X-API-Key", "test-management-key"))
			.andExpect(content().string(containsString("websocket_connections_active 1.0")));

		events.publishEvent(new SessionDisconnectEvent(this, message, "test-session", CloseStatus.NORMAL));
		mockMvc.perform(get("/metrics").header("X-API-Key", "test-management-key"))
			.andExpect(content().string(containsString("websocket_connections_active 0.0")));
	}
}
