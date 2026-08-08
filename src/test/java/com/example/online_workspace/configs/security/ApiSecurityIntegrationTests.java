package com.example.online_workspace.configs.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
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

	@DisplayName("未認証の保護APIは401を返しログイン画面へredirectしない")
	@Test
	void unauthenticatedApiRequestDoesNotRedirectToLoginPage() throws Exception {
		mockMvc.perform(get("/api/v1/rooms"))
			.andExpect(status().isUnauthorized())
			.andExpect(header().doesNotExist("Location"));
	}

	@DisplayName("CSRF tokenなしの状態変更APIは403を返す")
	@Test
	void stateChangingApiRejectsRequestWithoutCsrfToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/csrf"))
			.andExpect(status().isForbidden());
	}
}
