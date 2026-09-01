package com.example.online_workspace.controllers.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.online_workspace.services.auth.LoginRateLimiter;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@DisplayName("登録済みユーザーがログインでき、認証状態をリロード後も復元できる")
	@Test
	void loginCreatesRestorableSession() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");

		MvcResult login = performLogin(" " + email.toUpperCase() + " ", "password-123")
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.name").value("テストユーザー"))
			.andExpect(jsonPath("$.email").value(email))
			.andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		assertNotNull(session);
		SecurityContext context = (SecurityContext) session.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
		assertEquals(email, context.getAuthentication().getName());

		mockMvc.perform(get("/api/v1/auth/session").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(true))
			.andExpect(jsonPath("$.user.email").value(email));
	}

	@DisplayName("不正な認証情報はユーザーの有無にかかわらず401を返す")
	@Test
	void invalidCredentialsReturnUnauthorized() throws Exception {
		performLogin(uniqueEmail(), "wrong-password")
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@DisplayName("ログイン失敗が5回続いた後はRetry-After付きの429を返す")
	@Test
	void fiveFailuresRateLimitFurtherAttempts() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");
		for (int attempt = 0; attempt < LoginRateLimiter.MAX_FAILURES; attempt++) {
			performLogin(email, "wrong-password").andExpect(status().isUnauthorized());
		}

		performLogin(email, "wrong-password")
			.andExpect(status().isTooManyRequests())
			.andExpect(header().string("Retry-After", "900"))
			.andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
	}

	@DisplayName("ログインAPIはCSRFトークンを必須とする")
	@Test
	void loginRequiresCsrfToken() throws Exception {
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content(loginJson(uniqueEmail(), "password-123")))
			.andExpect(status().isForbidden());
	}

	@DisplayName("ログアウトするとサーバー側セッションとCookieが無効化される")
	@Test
	void logoutInvalidatesSessionAndCookie() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");
		MvcResult login = performLogin(email, "password-123").andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

		performLogout(session)
			.andExpect(status().isNoContent())
			.andExpect(content().string(""))
			.andExpect(cookie().maxAge("SESSION", 0));
		assertTrue(session.isInvalid());
	}

	@DisplayName("未ログイン状態ではログアウトできない")
	@Test
	void logoutRequiresAuthentication() throws Exception {
		MvcResult csrf = csrf();
		Cookie cookie = csrfCookie(csrf);
		mockMvc.perform(post("/api/v1/auth/logout")
				.cookie(cookie)
				.header("X-CSRF-TOKEN", cookie.getValue()))
			.andExpect(status().isUnauthorized());
	}

	private void register(String email, String password) throws Exception {
		MvcResult csrf = csrf();
		Cookie cookie = csrfCookie(csrf);
		mockMvc.perform(post("/api/v1/auth/register")
				.cookie(cookie)
				.header("X-CSRF-TOKEN", cookie.getValue())
				.contentType(APPLICATION_JSON)
				.content("""
					{"name":"テストユーザー","email":"%s","password":"%s"}
					""".formatted(email, password)))
			.andExpect(status().isCreated());
	}

	private ResultActions performLogin(String email, String password) throws Exception {
		MvcResult csrf = csrf();
		Cookie cookie = csrfCookie(csrf);
		return mockMvc.perform(post("/api/v1/auth/login")
			.cookie(cookie)
			.header("X-CSRF-TOKEN", cookie.getValue())
			.contentType(APPLICATION_JSON)
			.content(loginJson(email, password)));
	}

	private ResultActions performLogout(MockHttpSession session) throws Exception {
		MvcResult csrf = mockMvc.perform(get("/api/v1/auth/csrf").session(session)).andReturn();
		Cookie cookie = csrfCookie(csrf);
		return mockMvc.perform(post("/api/v1/auth/logout")
			.session(session)
			.cookie(cookie)
			.header("X-CSRF-TOKEN", cookie.getValue()));
	}

	private MvcResult csrf() throws Exception {
		return mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isNoContent())
			.andReturn();
	}

	private Cookie csrfCookie(MvcResult result) {
		Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
		assertNotNull(cookie);
		return cookie;
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
