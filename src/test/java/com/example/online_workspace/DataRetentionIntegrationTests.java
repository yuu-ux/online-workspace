package com.example.online_workspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import com.example.online_workspace.models.DataRetentionCleanupResult;
import com.example.online_workspace.services.DataRetentionCleanupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
	"spring.datasource.url=jdbc:h2:mem:data-retention-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
	"app.data-retention.scheduling-enabled=false"
})
class DataRetentionIntegrationTests {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DataRetentionCleanupService cleanupService;

	@BeforeEach
	void prepareDatabase() {
		createTables();
		jdbcTemplate.update("DELETE FROM work_sessions");
		jdbcTemplate.update("DELETE FROM messages");
		jdbcTemplate.update("DELETE FROM users");
	}

	@Test
	void deletesOnlyDataBeyondTheBoundaryAndRerunsIdempotently() {
		OffsetDateTime executionTime = OffsetDateTime.of(2026, 8, 4, 3, 0, 0, 0, ZoneOffset.UTC);
		OffsetDateTime chatCutoff = executionTime.minusMonths(3);
		OffsetDateTime withdrawalCutoff = executionTime.minusDays(30);

		insertMessage(1, chatCutoff.minusSeconds(1));
		insertMessage(2, chatCutoff);
		insertMessage(3, chatCutoff.plusSeconds(1));

		insertUser(1, withdrawalCutoff.minusSeconds(1));
		insertUser(2, withdrawalCutoff);
		insertUser(3, withdrawalCutoff.plusSeconds(1));
		insertUser(4, null);
		for (long userId = 1; userId <= 4; userId++) {
			jdbcTemplate.update("INSERT INTO work_sessions (id, user_id) VALUES (?, ?)", userId, userId);
		}

		DataRetentionCleanupResult firstRun = cleanupService.cleanupExpiredData(executionTime);

		assertThat(firstRun.deletedMessages()).isOne();
		assertThat(firstRun.deletedWorkSessions()).isOne();
		assertThat(ids("messages")).containsExactly(2L, 3L);
		assertThat(ids("work_sessions")).containsExactly(2L, 3L, 4L);

		DataRetentionCleanupResult secondRun = cleanupService.cleanupExpiredData(executionTime);
		assertThat(secondRun.deletedMessages()).isZero();
		assertThat(secondRun.deletedWorkSessions()).isZero();
		assertThat(ids("messages")).containsExactly(2L, 3L);
		assertThat(ids("work_sessions")).containsExactly(2L, 3L, 4L);
	}

	private void insertMessage(long id, OffsetDateTime sentAt) {
		jdbcTemplate.update("INSERT INTO messages (id, sent_at) VALUES (?, ?)", id, sentAt);
	}

	private void insertUser(long id, OffsetDateTime deletedAt) {
		jdbcTemplate.update("INSERT INTO users (id, deleted_at) VALUES (?, ?)", id, deletedAt);
	}

	private java.util.List<Long> ids(String table) {
		return jdbcTemplate.queryForList("SELECT id FROM " + table + " ORDER BY id", Long.class);
	}

	private void createTables() {
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS messages (
				id BIGINT PRIMARY KEY,
				sent_at TIMESTAMP WITH TIME ZONE NOT NULL
			)
			""");
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS users (
				id BIGINT PRIMARY KEY,
				deleted_at TIMESTAMP WITH TIME ZONE
			)
			""");
		jdbcTemplate.execute("""
			CREATE TABLE IF NOT EXISTS work_sessions (
				id BIGINT PRIMARY KEY,
				user_id BIGINT NOT NULL
			)
			""");
	}
}
