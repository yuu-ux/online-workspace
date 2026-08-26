package com.example.online_workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.online_workspace.repositories.AccountWithdrawalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext
class AccountWithdrawalIntegrationTests {

	private static final String EMAIL = "member@example.com";
	private static final String PASSWORD = "correct-password";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AccountWithdrawalRepository repository;

	@Autowired
	private SessionRegistry sessionRegistry;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@BeforeEach
	void prepareDatabase() {
		createTables();
		clearTables();
		jdbcTemplate.update("""
			INSERT INTO account_statuses (id, code) VALUES
				(1, 'ACTIVE'),
				(2, 'SUSPENDED'),
				(3, 'BANNED')
			""");
	}

	@Test
	void withdrawalRequiresPasswordAndInvalidatesCurrentSession() throws Exception {
		insertUser(1, EMAIL);
		jdbcTemplate.update("INSERT INTO profiles (user_id, bio) VALUES (?, ?)", 1, "profile");
		jdbcTemplate.update("INSERT INTO room_members (user_id, left_at) VALUES (?, NULL)", 1);

		MockHttpSession session = new MockHttpSession();
		UserDetails principal = User.withUsername(EMAIL)
			.password(PASSWORD)
			.authorities(new String[0])
			.build();
		sessionRegistry.registerNewSession("other-session", principal);
		mockMvc.perform(delete("/api/v1/users/me")
				.session(session)
				.with(user(principal))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"correct-password\"}"))
			.andExpect(status().isNoContent());

		assertThat(repository.findActiveByEmail(EMAIL)).isEmpty();
		assertThat(count("SELECT COUNT(*) FROM users WHERE id = 1 AND deleted_at IS NOT NULL")).isOne();
		assertThat(count("SELECT COUNT(*) FROM room_members WHERE user_id = 1 AND left_at IS NOT NULL")).isOne();
		assertThat(count("""
			SELECT COUNT(*)
			FROM users
			INNER JOIN room_members ON room_members.user_id = users.id
			WHERE users.id = 1
			  AND users.deleted_at = users.updated_at
			  AND room_members.left_at = users.deleted_at
			""")).isOne();
		assertThat(session.isInvalid()).isTrue();
		assertThat(sessionRegistry.getSessionInformation("other-session").isExpired()).isTrue();
	}

	@Test
	void withdrawalIsRejectedWhenPasswordDoesNotMatch() throws Exception {
		insertUser(1, EMAIL);
		MockHttpSession session = new MockHttpSession();

		mockMvc.perform(delete("/api/v1/users/me")
				.session(session)
				.with(user(EMAIL))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"wrong-password\"}"))
			.andExpect(status().isBadRequest());

		assertThat(repository.findActiveByEmail(EMAIL)).isPresent();
		assertThat(session.isInvalid()).isFalse();
	}

	@Test
	void withdrawalRejectsMissingConfirmationPassword() throws Exception {
		insertUser(1, EMAIL);

		mockMvc.perform(delete("/api/v1/users/me")
				.with(user(EMAIL))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"\"}"))
			.andExpect(status().isUnprocessableEntity());

		assertThat(repository.findActiveByEmail(EMAIL)).isPresent();
	}

	@Test
	void withdrawalAllowsWhitespaceOnlyRegisteredPassword() throws Exception {
		String whitespacePassword = "        ";
		insertUser(1, EMAIL, whitespacePassword);

		mockMvc.perform(delete("/api/v1/users/me")
				.with(user(EMAIL))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"        \"}"))
			.andExpect(status().isNoContent());

		assertThat(repository.findWithdrawableByEmail(EMAIL)).isEmpty();
	}

	@Test
	void withdrawalRejectsPasswordOver72Utf8Bytes() throws Exception {
		insertUser(1, EMAIL);

		mockMvc.perform(delete("/api/v1/users/me")
				.with(user(EMAIL))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"あああああああああああああああああああああああああ\"}"))
			.andExpect(status().isBadRequest());

		assertThat(repository.findWithdrawableByEmail(EMAIL)).isPresent();
	}

	@Test
	void passwordEncoderRejectsInputBeyondBcryptLimit() {
		String password = "a".repeat(72);

		assertThat(passwordEncoder.matches(password + "a", passwordEncoder.encode(password))).isFalse();
	}

	@Test
	void authenticationLookupRejectsUnavailableAccounts() {
		insertUser(1, EMAIL);
		insertUser(2, "suspended@example.com");
		insertUser(3, "banned@example.com");
		jdbcTemplate.update(
			"UPDATE users SET suspended_until = DATEADD('DAY', 1, CURRENT_TIMESTAMP) WHERE id = 2"
		);
		jdbcTemplate.update("UPDATE users SET account_status_id = 3 WHERE id = 3");

		assertThat(repository.findActiveByEmail(EMAIL)).isPresent();
		assertThat(repository.findActiveByEmail("suspended@example.com")).isEmpty();
		assertThat(repository.findActiveByEmail("banned@example.com")).isEmpty();
	}

	private void insertUser(long id, String email) {
		insertUser(id, email, PASSWORD);
	}

	private void insertUser(long id, String email, String password) {
		String passwordHash = new BCryptPasswordEncoder().encode(password);
		jdbcTemplate.update("""
			INSERT INTO users (
				id, name, email, password_hash, deleted_at, created_at, updated_at
			) VALUES (?, 'member', ?, ?, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
			""", id, email, passwordHash);
	}

	private int count(String sql) {
		Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
		return result == null ? 0 : result;
	}

	private void clearTables() {
		jdbcTemplate.update("DELETE FROM room_members");
		jdbcTemplate.update("DELETE FROM profiles");
		jdbcTemplate.update("DELETE FROM users");
		jdbcTemplate.update("DELETE FROM account_statuses");
	}

	private void createTables() {
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS account_statuses (
				id SMALLINT PRIMARY KEY,
				code VARCHAR(50) NOT NULL UNIQUE
			)
			""");
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS users (
				id BIGINT PRIMARY KEY,
				name VARCHAR(100) NOT NULL,
				email VARCHAR(255) NOT NULL UNIQUE,
				password_hash VARCHAR(255) NOT NULL,
				account_status_id SMALLINT NOT NULL DEFAULT 1,
				suspended_until TIMESTAMP WITH TIME ZONE,
				deleted_at TIMESTAMP WITH TIME ZONE,
				created_at TIMESTAMP WITH TIME ZONE NOT NULL,
				updated_at TIMESTAMP WITH TIME ZONE NOT NULL
			)
			""");
		jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS profiles (user_id BIGINT PRIMARY KEY, bio VARCHAR(500))");
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS room_members (
				id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
				user_id BIGINT NOT NULL,
				left_at TIMESTAMP WITH TIME ZONE
			)
			""");
	}
}
