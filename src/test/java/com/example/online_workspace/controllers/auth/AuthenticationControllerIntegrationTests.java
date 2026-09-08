package com.example.online_workspace.controllers.auth;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerIntegrationTests {

	private static final String SECURITY_AUDIT_LOGGER = "SECURITY_AUDIT";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private SessionRegistry sessionRegistry;

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

	@DisplayName("APIログインのセッションはSpring SecurityのSessionRegistryへ登録される")
	@Test
	void apiLoginRegistersSession() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");

		MvcResult login = performLogin(email, "password-123")
			.andExpect(status().isOk())
			.andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		assertNotNull(session);
		SecurityContext context = (SecurityContext) session.getAttribute(SPRING_SECURITY_CONTEXT_KEY);
		assertNotNull(context);
		Authentication authentication = context.getAuthentication();

		assertTrue(sessionRegistry.getAllSessions(authentication.getPrincipal(), false).stream()
			.anyMatch(info -> info.getSessionId().equals(session.getId())));
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

	@DisplayName("ログイン後の保護APIは認証ユーザーのメールアドレスで利用できる")
	@Test
	void loggedInSessionUsesEmailAsAuthenticationName() throws Exception {
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

	@DisplayName("ログイン成功はSECURITY_AUDITへ記録される")
	@Test
	void successfulLoginIsWrittenToSecurityAuditLog() throws Exception {
		ListAppender<ILoggingEvent> appender = attachSecurityAuditAppender();
		String email = uniqueEmail();
		String password = "password-123";
		try {
			register(email, password);
			performLogin(email, password)
				.andExpect(status().isOk());

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

	@DisplayName("レート制限中のログイン試行はSECURITY_AUDITへ記録される")
	@Test
	void rateLimitedLoginIsWrittenToSecurityAuditLog() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");
		for (int attempt = 1; attempt <= 5; attempt++) {
			performLogin(email, "wrong-password")
				.andExpect(status().isUnauthorized());
		}

		ListAppender<ILoggingEvent> appender = attachSecurityAuditAppender();
		try {
			performLogin(email, "wrong-password")
				.andExpect(status().isTooManyRequests());

			assertTrue(hasAuditMessage(appender, "event=authentication outcome=denied reason=rate_limit"));
			assertTrue(hasAuditMessage(appender, "retryAfterSeconds=900"));
		} finally {
			detachSecurityAuditAppender(appender);
		}
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
			.andExpect(content().contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON))
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

	@DisplayName("認証状態APIはユーザー情報の変更後に最新情報を返す")
	@Test
	void sessionStatusReturnsUpdatedUserDetails() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");
		MvcResult login = performLogin(email, "password-123")
			.andExpect(status().isOk())
			.andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		assertNotNull(session);

		jdbcTemplate.update("UPDATE users SET name = ? WHERE email = ?", "変更後ユーザー", email);

		mockMvc.perform(get("/api/v1/auth/session").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(true))
			.andExpect(jsonPath("$.user.name").value("変更後ユーザー"));
	}

	@DisplayName("認証済みセッションはアカウント停止後に利用できない")
	@Test
	void authenticatedSessionIsRejectedAfterAccountSuspension() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");
		MvcResult login = performLogin(email, "password-123")
			.andExpect(status().isOk())
			.andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		assertNotNull(session);

		jdbcTemplate.update("""
			UPDATE users
			SET account_status_id = (SELECT id FROM account_statuses WHERE code = 'SUSPENDED'),
			    suspended_until = DATEADD('DAY', 1, CURRENT_TIMESTAMP)
			WHERE email = ?
			""", email);

		mockMvc.perform(get("/api/v1/auth/session").session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authenticated").value(false))
			.andExpect(jsonPath("$.user").value(nullValue()));
	}

	@DisplayName("X-Real-IPヘッダーの偽装ではログインレート制限を回避できない")
	@Test
	void spoofedRealIpHeaderDoesNotBypassRateLimit() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");

		for (int attempt = 1; attempt <= 5; attempt++) {
			performLoginWithSpoofedHeader(email, "wrong-password", "198.51.100." + attempt)
				.andExpect(status().isUnauthorized());
		}

		performLoginWithSpoofedHeader(email, "wrong-password", "203.0.113.1")
			.andExpect(status().isTooManyRequests());
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

	@DisplayName("ログアウト成功はSECURITY_AUDITへ記録される")
	@Test
	void successfulLogoutIsWrittenToSecurityAuditLog() throws Exception {
		String email = uniqueEmail();
		register(email, "password-123");
		MvcResult login = performLogin(email, "password-123")
			.andExpect(status().isOk())
			.andReturn();
		MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
		assertNotNull(session);

		ListAppender<ILoggingEvent> appender = attachSecurityAuditAppender();
		try {
			performLogout(session)
				.andExpect(status().isNoContent());

			assertTrue(hasAuditMessage(appender, "event=logout outcome=success"));
		} finally {
			detachSecurityAuditAppender(appender);
		}
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
				.with(request -> {
					request.setRemoteAddr(clientAddress);
					return request;
				})
				.contentType(APPLICATION_JSON)
				.content(loginJson(email, password)));
	}

	private org.springframework.test.web.servlet.ResultActions performLoginWithSpoofedHeader(
		String email,
		String password,
		String spoofedAddress
	) throws Exception {
		MvcResult csrfResponse = mockMvc.perform(get("/api/v1/auth/csrf"))
			.andExpect(status().isNoContent())
			.andReturn();
		Cookie xsrfCookie = csrfCookie(csrfResponse);

		return mockMvc.perform(post("/api/v1/auth/login")
			.cookie(xsrfCookie)
			.header("X-CSRF-TOKEN", xsrfCookie.getValue())
			.header("X-Real-IP", spoofedAddress)
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
