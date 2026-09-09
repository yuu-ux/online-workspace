package com.example.online_workspace.configs.security;

import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class WebSecurityIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@DisplayName("ログイン画面は未認証でも表示できる")
	@Test
	void loginPageIsAvailable() throws Exception {
		mockMvc.perform(get("/login"))
			.andExpect(status().isOk());
	}

	@DisplayName("未認証のWebアクセスはログイン画面へリダイレクトする")
	@Test
	void unauthenticatedWebRequestRedirectsToLoginPage() throws Exception {
		mockMvc.perform(get("/protected-page"))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login"));
	}

	@DisplayName("停止されたAPIセッションはWebSocket接続できない")
	@Test
	void suspendedApiSessionCannotAccessWebSocketEndpoint() throws Exception {
		String email = "suspended-web-session@example.com";
		jdbcTemplate.update("DELETE FROM users WHERE email = ?", email);
		jdbcTemplate.update(
			"INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)",
			"停止ユーザー",
			email,
			passwordEncoder.encode("password-123")
		);

		UserDetails user = User.withUsername(email)
			.password("")
			.authorities(new String[0])
			.build();
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
			user,
			null,
			List.of()
		));
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, context);

		jdbcTemplate.update(
			"UPDATE users SET suspended_until = DATEADD('DAY', 1, CURRENT_TIMESTAMP) WHERE email = ?",
			email
		);

		mockMvc.perform(get("/ws/info").session(session))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/login"));
	}
}
