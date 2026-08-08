package com.example.online_workspace.configs.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void unauthenticatedApiRequestDoesNotRedirectToLoginPage() throws Exception {
		mockMvc.perform(get("/api/v1/rooms"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().doesNotExist("Location"));
	}

	@Test
	void sessionApiDoesNotRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/auth/session"))
			.andExpect(status().isNotFound());
	}

	@Test
	void registerApiDoesNotRequireSessionAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register").with(csrf()))
			.andExpect(status().isNotFound());
	}

	@Test
	void loginApiDoesNotRequireSessionAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login").with(csrf()))
			.andExpect(status().isNotFound());
	}

	@Test
	void stateChangingApiRejectsRequestWithoutCsrfToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login"))
			.andExpect(status().isForbidden());
	}
}
