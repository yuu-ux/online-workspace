package com.example.online_workspace.configs.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiSecurityIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@DisplayName("未認証の保護APIは401を返しログイン画面へredirectしない")
	@Test
	void unauthenticatedApiRequestDoesNotRedirectToLoginPage() throws Exception {
		ListAppender<ILoggingEvent> appender = attachSecurityAuditAppender();
		try {
			mockMvc.perform(get("/api/v1/rooms"))
				.andExpect(status().isUnauthorized())
				.andExpect(header().doesNotExist("Location"))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
			assertTrue(hasAuditMessage(appender, "event=authorization outcome=denied target=api method=GET status=401"));
		} finally {
			detachSecurityAuditAppender(appender);
		}
	}

	@DisplayName("CSRF tokenなしの状態変更APIは403を返す")
	@Test
	void stateChangingApiRejectsRequestWithoutCsrfToken() throws Exception {
		ListAppender<ILoggingEvent> appender = attachSecurityAuditAppender();
		try {
			mockMvc.perform(post("/api/v1/auth/csrf"))
				.andExpect(status().isForbidden())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
			assertTrue(hasAuditMessage(appender, "event=authorization outcome=denied target=api method=POST status=403"));
		} finally {
			detachSecurityAuditAppender(appender);
		}
	}

	private ListAppender<ILoggingEvent> attachSecurityAuditAppender() {
		Logger logger = (Logger) LoggerFactory.getLogger("SECURITY_AUDIT");
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		return appender;
	}

	private void detachSecurityAuditAppender(ListAppender<ILoggingEvent> appender) {
		Logger logger = (Logger) LoggerFactory.getLogger("SECURITY_AUDIT");
		logger.detachAppender(appender);
		appender.stop();
	}

	private boolean hasAuditMessage(ListAppender<ILoggingEvent> appender, String messagePart) {
		return appender.list.stream()
			.anyMatch(event -> event.getFormattedMessage().contains(messagePart));
	}

	@DisplayName("認可拒否の監査ログへ個人情報や秘密情報を記録しない")
	@Test
	void authorizationDenialAuditLogContainsNoRequestData() throws Exception {
		Logger logger = (Logger) LoggerFactory.getLogger(SecurityAuditLogger.LOGGER_NAME);
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);

		try {
			mockMvc.perform(get("/api/v1/rooms")
					.queryParam("email", "private@example.com")
					.header("Cookie", "JSESSIONID=secret-session"))
				.andExpect(status().isUnauthorized());
		} finally {
			logger.detachAppender(appender);
		}

		assertThat(appender.list)
			.extracting(ILoggingEvent::getFormattedMessage)
			.contains("security_audit event=authorization outcome=denied target=api method=GET status=401")
			.allSatisfy(message -> assertThat(message)
				.doesNotContain("private@example.com", "secret-session", "/api/v1/rooms"));
	}
}
