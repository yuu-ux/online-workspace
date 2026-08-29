package com.example.online_workspace.configs.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:api-key-rate-limit-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
	"app.security.api-key.value=test-api-key",
	"app.security.api-key.principal=creator@example.com",
	"app.security.rate-limit.requests-per-minute=2"
})
@AutoConfigureMockMvc
@Sql("/room-create-api-test.sql")
class ApiKeyRateLimitIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void authenticatesApiKeySkipsCsrfAndRateLimitsRequests() throws Exception {
		mockMvc.perform(createRoom("invalid-api-key"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_API_KEY"));

		mockMvc.perform(createRoom("test-api-key"))
			.andExpect(status().isCreated());
		mockMvc.perform(createRoom("test-api-key"))
			.andExpect(status().isCreated());
		mockMvc.perform(createRoom("test-api-key"))
			.andExpect(status().isTooManyRequests())
			.andExpect(header().exists("Retry-After"))
			.andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));

		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rooms", Integer.class)).isEqualTo(2);
	}

	private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder createRoom(String apiKey) {
		return post("/api/v1/rooms")
			.header(ApiKeyAuthenticationFilter.HEADER_NAME, apiKey)
			.contentType(MediaType.APPLICATION_JSON)
			.content("""
				{
				  "name": "集中ルーム",
				  "description": "一緒に作業します",
				  "categoryId": 1,
				  "workStyle": "FOCUS",
				  "maxMembers": 12
				}
				""");
	}
}
