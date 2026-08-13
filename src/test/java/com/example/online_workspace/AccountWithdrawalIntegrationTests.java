package com.example.online_workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.online_workspace.repositories.AccountWithdrawalRepository;
import com.example.online_workspace.services.WithdrawnAccountPurgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
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
	private WithdrawnAccountPurgeService purgeService;

	@BeforeEach
	void prepareDatabase() {
		createTables();
		clearTables();
	}

	@Test
	void withdrawalRequiresPasswordAndInvalidatesCurrentSession() throws Exception {
		insertUser(1, EMAIL, null);
		jdbcTemplate.update("INSERT INTO profiles (user_id, bio) VALUES (?, ?)", 1, "profile");
		jdbcTemplate.update("INSERT INTO room_members (user_id, left_at) VALUES (?, NULL)", 1);
		jdbcTemplate.update("INSERT INTO work_sessions (user_id, ended_at, updated_at) VALUES (?, NULL, CURRENT_TIMESTAMP)", 1);
		jdbcTemplate.update("INSERT INTO room_invites (created_by, invalidated_at) VALUES (?, NULL)", 1);

		MockHttpSession session = new MockHttpSession();
		mockMvc.perform(delete("/api/v1/users/me")
				.session(session)
				.with(user(EMAIL))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"correct-password\"}"))
			.andExpect(status().isNoContent());

		assertThat(repository.findActiveByEmail(EMAIL)).isEmpty();
		assertThat(count("SELECT COUNT(*) FROM users WHERE id = 1 AND deleted_at IS NOT NULL")).isOne();
		assertThat(count("SELECT COUNT(*) FROM room_members WHERE user_id = 1 AND left_at IS NOT NULL")).isOne();
		assertThat(count("SELECT COUNT(*) FROM work_sessions WHERE user_id = 1 AND ended_at IS NOT NULL")).isOne();
		assertThat(count("SELECT COUNT(*) FROM room_invites WHERE created_by = 1 AND invalidated_at IS NOT NULL")).isOne();
		assertThat(session.isInvalid()).isTrue();
	}

	@Test
	void withdrawalIsRejectedWhenPasswordDoesNotMatch() throws Exception {
		insertUser(1, EMAIL, null);
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
		insertUser(1, EMAIL, null);

		mockMvc.perform(delete("/api/v1/users/me")
				.with(user(EMAIL))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"password\":\"\"}"))
			.andExpect(status().isBadRequest());

		assertThat(repository.findActiveByEmail(EMAIL)).isPresent();
	}

	@Test
	void purgeRejectsNonPositiveRetentionPeriod() {
		assertThatThrownBy(() -> new WithdrawnAccountPurgeService(repository, 0))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("retentionDays must be positive");
	}

	@Test
	void purgeDeletesDataAtThirtyDayBoundaryAndIsIdempotent() {
		OffsetDateTime executionTime = OffsetDateTime.of(2026, 8, 4, 3, 0, 0, 0, ZoneOffset.UTC);
		insertUser(1, "boundary@example.com", executionTime.minusDays(30));
		insertUser(2, "recent@example.com", executionTime.minusDays(30).plusSeconds(1));
		insertUser(3, "older@example.com", executionTime.minusDays(31));

		for (int userId = 1; userId <= 3; userId++) {
			jdbcTemplate.update("INSERT INTO profiles (user_id, bio) VALUES (?, ?)", userId, "profile");
			jdbcTemplate.update("INSERT INTO room_members (user_id, left_at) VALUES (?, CURRENT_TIMESTAMP)", userId);
			jdbcTemplate.update("INSERT INTO work_sessions (user_id, ended_at, updated_at) VALUES (?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", userId);
			jdbcTemplate.update("INSERT INTO room_invites (created_by, invalidated_at) VALUES (?, CURRENT_TIMESTAMP)", userId);
		}
		jdbcTemplate.update("INSERT INTO friends (user_id, friend_user_id) VALUES (1, 2)");
		jdbcTemplate.update("INSERT INTO blocks (blocker_user_id, blocked_user_id) VALUES (3, 2)");

		assertThat(purgeService.purgeExpiredAccounts(executionTime)).isEqualTo(2);

		assertThat(count("SELECT COUNT(*) FROM work_sessions WHERE user_id IN (1, 3)")).isZero();
		assertThat(count("SELECT COUNT(*) FROM profiles WHERE user_id IN (1, 3)")).isZero();
		assertThat(count("SELECT COUNT(*) FROM room_members WHERE user_id IN (1, 3)")).isZero();
		assertThat(count("SELECT COUNT(*) FROM room_invites WHERE created_by IN (1, 3)")).isZero();
		assertThat(count("SELECT COUNT(*) FROM work_sessions WHERE user_id = 2")).isOne();
		assertThat(count("SELECT COUNT(*) FROM friends WHERE user_id = 1 OR friend_user_id = 1")).isZero();
		assertThat(count("SELECT COUNT(*) FROM blocks WHERE blocker_user_id = 3 OR blocked_user_id = 3")).isZero();
		assertThat(count("SELECT COUNT(*) FROM users WHERE id IN (1, 3) AND personal_data_purged_at IS NOT NULL")).isEqualTo(2);
		assertThat(jdbcTemplate.queryForObject("SELECT email FROM users WHERE id = 1", String.class))
			.isEqualTo("withdrawn-1@deleted.invalid");
		assertThat(purgeService.purgeExpiredAccounts(executionTime)).isZero();
	}

	private void insertUser(long id, String email, OffsetDateTime deletedAt) {
		String passwordHash = new BCryptPasswordEncoder().encode(PASSWORD);
		jdbcTemplate.update("""
			INSERT INTO users (
				id, name, email, password_hash, deleted_at, personal_data_purged_at, created_at, updated_at
			) VALUES (?, 'member', ?, ?, ?, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
			""", id, email, passwordHash, deletedAt);
	}

	private int count(String sql) {
		Integer result = jdbcTemplate.queryForObject(sql, Integer.class);
		return result == null ? 0 : result;
	}

	private void clearTables() {
		jdbcTemplate.update("DELETE FROM blocks");
		jdbcTemplate.update("DELETE FROM friends");
		jdbcTemplate.update("DELETE FROM room_invites");
		jdbcTemplate.update("DELETE FROM room_members");
		jdbcTemplate.update("DELETE FROM work_sessions");
		jdbcTemplate.update("DELETE FROM profiles");
		jdbcTemplate.update("DELETE FROM users");
	}

	private void createTables() {
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS users (
				id BIGINT PRIMARY KEY,
				name VARCHAR(100) NOT NULL,
				email VARCHAR(255) NOT NULL UNIQUE,
				password_hash VARCHAR(255) NOT NULL,
				deleted_at TIMESTAMP WITH TIME ZONE,
				personal_data_purged_at TIMESTAMP WITH TIME ZONE,
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
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS work_sessions (
				id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
				user_id BIGINT NOT NULL,
				ended_at TIMESTAMP WITH TIME ZONE,
				updated_at TIMESTAMP WITH TIME ZONE NOT NULL
			)
			""");
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS room_invites (
				id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
				created_by BIGINT NOT NULL,
				invalidated_at TIMESTAMP WITH TIME ZONE
			)
			""");
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS friends (
				id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
				user_id BIGINT NOT NULL,
				friend_user_id BIGINT NOT NULL
			)
			""");
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS blocks (
				blocker_user_id BIGINT NOT NULL,
				blocked_user_id BIGINT NOT NULL,
				PRIMARY KEY (blocker_user_id, blocked_user_id)
			)
			""");
	}
}
