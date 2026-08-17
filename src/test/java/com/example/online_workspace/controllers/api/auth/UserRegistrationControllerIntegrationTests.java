package com.example.online_workspace.controllers.api.auth;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
class UserRegistrationControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@DisplayName("有効な入力でユーザーを登録できる")
	@Test
	void registerUserWithValidRequest() throws Exception {
		String email = uniqueEmail();

		performRegister(email, "password-123")
			.andExpect(status().isCreated())
			.andExpect(content().string(""));

		String passwordHash = jdbcTemplate.queryForObject(
			"SELECT password_hash FROM users WHERE email = ?",
			String.class,
			email
		);
		assertNotNull(passwordHash);
		assertTrue(passwordEncoder.matches("password-123", passwordHash));
	}

	@DisplayName("不正なメールアドレスではユーザーを登録できない")
	@Test
	void rejectRegistrationWithInvalidEmail() throws Exception {
		performRegister("invalid-email", "password-123")
			.andExpect(status().isUnprocessableEntity());
	}

	@DisplayName("登録済みメールアドレスではユーザーを登録できない")
	@Test
	void rejectRegistrationWithDuplicateEmail() throws Exception {
		String email = uniqueEmail();

		performRegister(email, "password-123")
			.andExpect(status().isCreated());

		performRegister(email, "password-456")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
	}

	@DisplayName("CSRFトークンなしのユーザー登録は拒否される")
	@Test
	void rejectRegistrationWithoutCsrfToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(APPLICATION_JSON)
				.content(registerJson(uniqueEmail(), "password-123")))
			.andExpect(status().isForbidden());
	}

	private org.springframework.test.web.servlet.ResultActions performRegister(
		String email,
		String password
	) throws Exception {
		MvcResult csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isNoContent())
			.andReturn();

		Cookie xsrfCookie = csrfResponse.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(xsrfCookie);

		return mockMvc.perform(post("/api/v1/auth/register")
				.cookie(xsrfCookie)
				.header("X-CSRF-TOKEN", xsrfCookie.getValue())
				.contentType(APPLICATION_JSON)
				.content(registerJson(email, password)));
	}

	private String registerJson(String email, String password) {
		return """
			{"name":"テストユーザー","email":"%s","password":"%s"}
			""".formatted(email, password);
	}

	private String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}
}
