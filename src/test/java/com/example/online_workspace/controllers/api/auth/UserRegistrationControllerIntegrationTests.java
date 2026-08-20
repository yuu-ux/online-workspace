package com.example.online_workspace.controllers.api.auth;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
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

		String registeredName = jdbcTemplate.queryForObject(
			"SELECT name FROM users WHERE email = ?",
			String.class,
			email
		);
		String registeredEmail = jdbcTemplate.queryForObject(
			"SELECT email FROM users WHERE email = ?",
			String.class,
			email
		);
		String passwordHash = jdbcTemplate.queryForObject(
			"SELECT password_hash FROM users WHERE email = ?",
			String.class,
			email
		);
		assertEquals("テストユーザー", registeredName);
		assertEquals(email, registeredEmail);
		assertNotNull(passwordHash);
		assertTrue(passwordHash.matches("\\A\\$2[aby]?\\$\\d{2}\\$[./0-9A-Za-z]{53}\\z"));
		assertTrue(passwordEncoder.matches("password-123", passwordHash));
	}

	@DisplayName("名前未入力ではユーザーを登録できない")
	@Test
	void rejectRegistrationWithBlankName() throws Exception {
		performRegister("", uniqueEmail(), "password-123")
			.andExpect(status().isUnprocessableEntity());
	}

	@DisplayName("パスワード未入力ではユーザーを登録できない")
	@Test
	void rejectRegistrationWithBlankPassword() throws Exception {
		performRegister("テストユーザー", uniqueEmail(), "")
			.andExpect(status().isUnprocessableEntity());
	}

	@DisplayName("8文字未満のパスワードではユーザーを登録できない")
	@Test
	void rejectRegistrationWithShortPassword() throws Exception {
		performRegister("テストユーザー", uniqueEmail(), "1234567")
			.andExpect(status().isUnprocessableEntity());
	}

	@DisplayName("ASCII以外を含むパスワードではユーザーを登録できない")
	@Test
	void rejectRegistrationWithNonAsciiPassword() throws Exception {
		performRegister("テストユーザー", uniqueEmail(), "password-あ")
			.andExpect(status().isUnprocessableEntity());
	}

	@DisplayName("72文字を超えるパスワードではユーザーを登録できない")
	@Test
	void rejectRegistrationWithPasswordOver72Characters() throws Exception {
		performRegister("テストユーザー", uniqueEmail(), "a".repeat(73))
			.andExpect(status().isUnprocessableEntity());
	}

	@DisplayName("メールアドレスは小文字に正規化して保存される")
	@Test
	void normalizeEmailBeforeRegistration() throws Exception {
		String email = "User-" + UUID.randomUUID() + "@Example.COM";

		performRegister("テストユーザー", email, "password-123")
			.andExpect(status().isCreated());

		String registeredEmail = jdbcTemplate.queryForObject(
			"SELECT email FROM users WHERE email = ?",
			String.class,
			email.toLowerCase(Locale.ROOT)
		);
		assertEquals(email.toLowerCase(Locale.ROOT), registeredEmail);
	}

	@DisplayName("前後に空白があるメールアドレスではユーザーを登録できない")
	@Test
	void rejectRegistrationWithSurroundingWhitespaceInEmail() throws Exception {
		performRegister("テストユーザー", " " + uniqueEmail() + " ", "password-123")
			.andExpect(status().isUnprocessableEntity());
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

	@DisplayName("未定義の項目を含む登録リクエストは拒否される")
	@Test
	void rejectRegistrationWithUnknownProperty() throws Exception {
		String email = uniqueEmail();

		performRegisterJson("""
			{"name":"テストユーザー","email":"%s","password":"password-123","pasword":"typo"}
			""".formatted(email))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("MALFORMED_JSON"));
	}

	@DisplayName("CSRFトークンなしのユーザー登録は拒否される")
	@Test
	void rejectRegistrationWithoutCsrfToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(APPLICATION_JSON)
				.content(registerJson("テストユーザー", uniqueEmail(), "password-123")))
			.andExpect(status().isForbidden());
	}

	private org.springframework.test.web.servlet.ResultActions performRegister(
		String email,
		String password
	) throws Exception {
		return performRegister("テストユーザー", email, password);
	}

	private org.springframework.test.web.servlet.ResultActions performRegister(
		String name,
		String email,
		String password
	) throws Exception {
		return performRegisterJson(registerJson(name, email, password));
	}

	private org.springframework.test.web.servlet.ResultActions performRegisterJson(String json) throws Exception {
		MvcResult csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isNoContent())
			.andReturn();

		Cookie xsrfCookie = csrfResponse.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(xsrfCookie);

		return mockMvc.perform(post("/api/v1/auth/register")
				.cookie(xsrfCookie)
				.header("X-CSRF-TOKEN", xsrfCookie.getValue())
				.contentType(APPLICATION_JSON)
				.content(json));
	}

	private String registerJson(String name, String email, String password) {
		return """
			{"name":"%s","email":"%s","password":"%s"}
			""".formatted(name, email, password);
	}

	private String uniqueEmail() {
		return "user-" + UUID.randomUUID() + "@example.com";
	}
}
