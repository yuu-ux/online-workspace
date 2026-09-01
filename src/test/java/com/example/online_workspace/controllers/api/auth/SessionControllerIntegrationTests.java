package com.example.online_workspace.controllers.api.auth;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@AutoConfigureMockMvc
class SessionControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@DisplayName("未認証の認証状態APIはauthenticated=falseとuser=nullを返す")
	@Test
	void sessionStatusReturnsAnonymousState() throws Exception {
		mockMvc.perform(get("/api/v1/auth/session"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(false))
			.andExpect(jsonPath("$.user").value(nullValue()));
	}

	@DisplayName("ログイン後のセッション状態にユーザー情報を返しログアウトできる")
	@Test
	void loginSessionAndLogout() throws Exception {
		String email = "session@example.com";
		jdbcTemplate.update("DELETE FROM users WHERE email = ?", email);
		jdbcTemplate.update(
			"INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)",
			"セッション利用者",
			email,
			passwordEncoder.encode("password-123")
		);

		MvcResult csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isNoContent())
			.andReturn();
		Cookie csrf = csrfResponse.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(csrf);

		MvcResult loginResponse = mockMvc.perform(post("/api/v1/auth/login")
				.cookie(csrf)
				.header("X-CSRF-TOKEN", csrf.getValue())
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
					{"email":"session@example.com","password":"password-123"}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("セッション利用者"))
			.andReturn();
		MockHttpSession session = (MockHttpSession) loginResponse.getRequest().getSession(false);
		assertNotNull(session);

		mockMvc.perform(get("/api/v1/auth/session").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(true))
			.andExpect(jsonPath("$.user.email").value(email));

		MvcResult logoutCsrfResponse = mockMvc.perform(get("/api/v1/auth/csrf").session(session))
			.andExpect(status().isNoContent())
			.andReturn();
		Cookie logoutCsrf = logoutCsrfResponse.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(logoutCsrf);

		mockMvc.perform(post("/api/v1/auth/logout")
				.session(session)
				.cookie(logoutCsrf)
				.header("X-CSRF-TOKEN", logoutCsrf.getValue()))
			.andExpect(status().isNoContent())
			.andExpect(content().string(""));
		assertTrue(session.isInvalid());

		mockMvc.perform(get("/api/v1/auth/session"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(false));
	}
}
