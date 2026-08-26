package com.example.online_workspace.controllers.api.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@DisplayName("#16で登録したユーザーがログインでき、サーバーセッションとユーザー情報が返る")
	@Test
	void registeredUserCanLoginAndSessionIsCreated() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");

		MvcResult login = performLogin(email, "password-123")
			.andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(APPLICATION_JSON))
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.name").value("テストユーザー"))
			.andExpect(jsonPath("$.email").value(email))
			.andExpect(jsonPath("$.accountStatus").value("ACTIVE"))
			.andReturn();

		assertNotNull(login.getRequest().getSession(false));
	}

	@DisplayName("ログイン時のメールアドレスは前後の空白と大文字を正規化して照合される")
	@Test
	void loginNormalizesEmailBeforeLookup() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");

		performLogin(" " + email.toUpperCase() + " ", "password-123")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(email));
	}

	@DisplayName("認証情報が不正なログインは401を返す")
	@Test
	void rejectLoginWithInvalidCredentials() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");

		performLogin(email, "wrong-password")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@DisplayName("未登録メールアドレスのログインは401を返す")
	@Test
	void rejectLoginForUnknownEmail() throws Exception {
		performLogin(uniqueEmail(), "password-123")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@DisplayName("ログイン失敗が5回続いた後は429を返す")
	@Test
	void rateLimitLoginAfterFiveConsecutiveFailures() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");

		for (int attempt = 1; attempt <= 5; attempt++) {
			performLogin(email, "wrong-password")
				.andExpect(status().isUnauthorized());
		}

		performLogin(email, "wrong-password")
			.andExpect(status().isTooManyRequests())
			.andExpect(header().string("Retry-After", "900"))
			.andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
	}

	@DisplayName("別の接続元からのログインは同じメールアドレスでもロックされない")
	@Test
	void rateLimitDoesNotLockOutAnotherClient() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");

		for (int attempt = 1; attempt <= 5; attempt++) {
			performLogin(email, "wrong-password", "198.51.100.10")
				.andExpect(status().isUnauthorized());
		}

		performLogin(email, "wrong-password", "198.51.100.10")
			.andExpect(status().isTooManyRequests());
		performLogin(email, "wrong-password", "198.51.100.11")
			.andExpect(status().isUnauthorized());
	}

	@DisplayName("メールアドレス形式が不正なログインは422を返す")
	@Test
	void rejectLoginWithInvalidEmailFormat() throws Exception {
		performLogin("not-an-email", "password-123")
			.andExpect(status().isUnprocessableContent())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@DisplayName("正しいログインに成功するとログイン失敗回数がリセットされる")
	@Test
	void successfulLoginResetsConsecutiveFailures() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");

		for (int attempt = 1; attempt <= 4; attempt++) {
			performLogin(email, "wrong-password")
				.andExpect(status().isUnauthorized());
		}
		performLogin(email, "password-123")
			.andExpect(status().isOk());

		for (int attempt = 1; attempt <= 4; attempt++) {
			performLogin(email, "wrong-password")
				.andExpect(status().isUnauthorized());
		}
	}

	@DisplayName("ログインAPIはCSRFトークンなしでは403を返す")
	@Test
	void rejectLoginWithoutCsrfToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content(loginJson(uniqueEmail(), "password-123")))
			.andExpect(status().isForbidden());
	}

	@DisplayName("認証済みセッションの状態APIはユーザー情報を返す")
	@Test
	void sessionStatusReturnsAuthenticatedUser() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");
		MvcResult login = performLogin(email, "password-123")
			.andExpect(status().isOk())
			.andReturn();

		mockMvc.perform(get("/api/v1/auth/session")
				.session((MockHttpSession) login.getRequest().getSession(false)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(true))
			.andExpect(jsonPath("$.user.email").value(email));
	}

	@DisplayName("ログアウトするとサーバー側セッションが無効化される")
	@Test
	void logoutInvalidatesServerSession() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");
		MvcResult login = performLogin(email, "password-123")
			.andExpect(status().isOk())
			.andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		assertNotNull(session);

		performLogout(session)
			.andExpect(status().isNoContent())
			.andExpect(content().string(""))
			.andExpect(cookie().maxAge("JSESSIONID", 0));

		assertTrue(session.isInvalid());
	}

	@DisplayName("ログアウトAPIはCSRFトークンなしでは403を返す")
	@Test
	void rejectLogoutWithoutCsrfToken() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");
		MvcResult login = performLogin(email, "password-123")
			.andExpect(status().isOk())
			.andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		assertNotNull(session);

		mockMvc.perform(post("/api/v1/auth/logout").session(session))
			.andExpect(status().isForbidden());
	}

	@DisplayName("未ログイン状態ではログアウトできない")
	@Test
	void rejectLogoutWithoutAuthenticatedSession() throws Exception {
		MvcResult csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isNoContent())
			.andReturn();
		Cookie xsrfCookie = csrfCookie(csrfResponse);

		mockMvc.perform(post("/api/v1/auth/logout")
				.cookie(xsrfCookie)
				.header("X-CSRF-TOKEN", xsrfCookie.getValue()))
			.andExpect(status().isUnauthorized());
	}

	private void register(String email, String password) throws Exception {
		MvcResult csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isNoContent())
			.andReturn();
		Cookie xsrfCookie = csrfCookie(csrfResponse);

		mockMvc.perform(post("/api/v1/auth/register")
				.cookie(xsrfCookie)
				.header("X-CSRF-TOKEN", xsrfCookie.getValue())
				.contentType(APPLICATION_JSON)
				.content(registerJson(email, password)))
			.andExpect(status().isCreated());
	}

	private org.springframework.test.web.servlet.ResultActions performLogin(String email, String password)
		throws Exception {
		return performLogin(email, password, "127.0.0.1");
	}

	private org.springframework.test.web.servlet.ResultActions performLogin(
		String email,
		String password,
		String clientAddress
	) throws Exception {
		MvcResult csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isNoContent())
			.andReturn();
		Cookie xsrfCookie = csrfCookie(csrfResponse);

		return mockMvc.perform(post("/api/v1/auth/login")
				.cookie(xsrfCookie)
				.header("X-CSRF-TOKEN", xsrfCookie.getValue())
				.header("X-Real-IP", clientAddress)
				.contentType(APPLICATION_JSON)
				.content(loginJson(email, password)));
	}

	private org.springframework.test.web.servlet.ResultActions performLogout(MockHttpSession session)
		throws Exception {
		MvcResult csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf").session(session))
			.andExpect(status().isNoContent())
			.andReturn();
		Cookie xsrfCookie = csrfCookie(csrfResponse);

		return mockMvc.perform(post("/api/v1/auth/logout")
				.session(session)
				.cookie(xsrfCookie)
				.header("X-CSRF-TOKEN", xsrfCookie.getValue()));
	}

	private Cookie csrfCookie(MvcResult csrfResponse) {
		Cookie xsrfCookie = csrfResponse.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(xsrfCookie);
		return xsrfCookie;
	}

	private String registerJson(String email, String password) {
		return """
			{"name":"テストユーザー","email":"%s","password":"%s"}
			""".formatted(email, password);
	}

	private String loginJson(String email, String password) {
		return """
			{"email":"%s","password":"%s"}
			""".formatted(email, password);
	}

	private String uniqueEmail() {
		return "login-" + UUID.randomUUID() + "@example.com";
	}
}
