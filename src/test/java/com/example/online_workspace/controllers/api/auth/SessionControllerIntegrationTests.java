package com.example.online_workspace.controllers.api.auth;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SessionControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@DisplayName("未認証の認証状態APIはauthenticated=falseとuser=nullを返す")
	@Test
	void sessionStatusReturnsAnonymousState() throws Exception {
		mockMvc.perform(get("/api/v1/auth/session"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(false))
			.andExpect(jsonPath("$.user").value(nullValue()));
	}
}
