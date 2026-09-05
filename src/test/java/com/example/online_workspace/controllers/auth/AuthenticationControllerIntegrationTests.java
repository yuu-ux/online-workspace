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
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.slf4j.LoggerFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerIntegrationTests {

	private static final String SECURITY_AUDIT_LOGGER = "SECURITY_AUDIT";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

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

	@DisplayName("ログイン成功はSECURITY_AUDITへ記録される")
	@Test
	void successfulLoginIsWrittenToSecurityAuditLog() throws Exception {
		ListAppender<ILoggingEvent> appender = attachSecurityAuditAppender();
		try {
			String email = uniqueEmail();
			String password = "password-123";
			register(email, password);
			performLogin(email, password).andExpect(status().isOk());

			assertTrue(hasAuditMessage(appender, "event=authentication outcome=success"));
			assertTrue(hasNoAuditMessageContaining(appender, email));
			assertTrue(hasNoAuditMessageContaining(appender, password));
		} finally {
			detachSecurityAuditAppender(appender);
		}
	}

	@DisplayName("ログイン失敗はSECURITY_AUDITへ記録される")
	@Test
	void failedLoginIsWrittenToSecurityAuditLog() throws Exception {
		ListAppender<ILoggingEvent> appender = attachSecurityAuditAppender();
		try {
			performLogin(uniqueEmail(), "wrong-password")
				.andExpect(status().isUnauthorized());

			assertTrue(hasAuditMessage(appender, "event=authentication outcome=denied"));
		} finally {
			detachSecurityAuditAppender(appender);
		}
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

	@DisplayName("レート制限中のログイン試行はSECURITY_AUDITへ記録される")
	@Test
	void rateLimitedLoginIsWrittenToSecurityAuditLog() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");
		for (int attempt = 0; attempt < LoginRateLimiter.MAX_FAILURES; attempt++) {
			performLogin(email, "wrong-password").andExpect(status().isUnauthorized());
		}

		ListAppender<ILoggingEvent> appender = attachSecurityAuditAppender();
		try {
			performLogin(email, "wrong-password")
				.andExpect(status().isTooManyRequests());

			assertTrue(hasAuditMessage(appender, "reason=rate_limit"));
		} finally {
			detachSecurityAuditAppender(appender);
		}
	}

	@DisplayName("利用中でも停止期限が未来のユーザーはログインできない")
	@Test
	void rejectLoginForActiveAccountWithFutureSuspension() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");
		jdbcTemplate.update(
			"UPDATE users SET suspended_until = DATEADD('DAY', 1, CURRENT_TIMESTAMP) WHERE email = ?",
			email
		);

		performLogin(email, "password-123")
			.andExpect(status().isUnauthorized());
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
			.andExpect(cookie().maxAge("JSESSIONID", 0));
		assertTrue(session.isInvalid());
	}

	@DisplayName("ログアウト成功はSECURITY_AUDITへ記録される")
	@Test
	void successfulLogoutIsWrittenToSecurityAuditLog() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");
		MvcResult login = performLogin(email, "password-123").andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

		ListAppender<ILoggingEvent> appender = attachSecurityAuditAppender();
		try {
			performLogout(session).andExpect(status().isNoContent());

			assertTrue(hasAuditMessage(appender, "event=logout outcome=success"));
		} finally {
			detachSecurityAuditAppender(appender);
		}
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

	private ListAppender<ILoggingEvent> attachSecurityAuditAppender() {
		Logger logger = (Logger) LoggerFactory.getLogger(SECURITY_AUDIT_LOGGER);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		return appender;
	}

	private void detachSecurityAuditAppender(ListAppender<ILoggingEvent> appender) {
		Logger logger = (Logger) LoggerFactory.getLogger(SECURITY_AUDIT_LOGGER);
		logger.detachAppender(appender);
		appender.stop();
	}

	private boolean hasAuditMessage(ListAppender<ILoggingEvent> appender, String messagePart) {
		return appender.list.stream()
			.anyMatch(event -> event.getFormattedMessage().contains(messagePart));
	}

	private boolean hasNoAuditMessageContaining(ListAppender<ILoggingEvent> appender, String value) {
		return appender.list.stream()
			.noneMatch(event -> event.getFormattedMessage().contains(value));
	}
}
