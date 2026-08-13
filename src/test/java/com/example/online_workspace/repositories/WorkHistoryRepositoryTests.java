package com.example.online_workspace.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

@MybatisTest
@Sql(scripts = "/work-history-repository-test.sql")
class WorkHistoryRepositoryTests {

	private static final Instant STARTED_AT = Instant.parse("2026-08-03T01:00:00Z");
	private static final Instant ENDED_AT = Instant.parse("2026-08-03T02:30:00Z");

	@Autowired
	private WorkHistoryRepository repository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void loadsACompleteHistoryRowAndOverlappingParticipants() {
		assertThat(repository.findActiveUserIdByEmail("me@example.com")).isEqualTo(10L);
		assertThat(repository.countSessions(10L, null, null, null)).isOne();

		var sessions = repository.findSessions(10L, null, null, null, 20, 0L);

		assertThat(sessions).hasSize(1);
		assertThat(sessions.getFirst().roomName()).isEqualTo("朝活ルーム");
		assertThat(sessions.getFirst().categoryName()).isEqualTo("開発");
		assertThat(sessions.getFirst().durationSeconds()).isEqualTo(5400L);
		assertThat(repository.findParticipants(20L, STARTED_AT, ENDED_AT))
			.extracting("name")
			.containsExactly("共同作業者", "自分");
	}

	@Test
	void aggregatesCategoryAndUtcDateDurations() {
		jdbcTemplate.update("""
			INSERT INTO work_sessions (id, user_id, room_id, category_id, started_at, ended_at)
			VALUES (101, 10, 20, 2, TIMESTAMP WITH TIME ZONE '2099-01-01 00:00:00+00', NULL)
			""");
		var byCategory = repository.summarizeByCategory(10L, null, null);
		var byDate = repository.summarizeByDate(10L, null, null);

		assertThat(byCategory).hasSize(1);
		assertThat(byCategory.getFirst().categoryName()).isEqualTo("開発");
		assertThat(byCategory.getFirst().durationSeconds()).isEqualTo(5400L);
		assertThat(byDate).hasSize(2);
		assertThat(byDate.getFirst().workDate()).hasToString("2026-08-03");
		assertThat(byDate.getFirst().durationSeconds()).isEqualTo(5400L);
		assertThat(byDate).allMatch(row -> row.durationSeconds() >= 0);
	}
}
