package com.example.online_workspace.repositories;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import com.example.online_workspace.models.WorkSession;
import com.example.online_workspace.repositories.users.UserRepository;

@MybatisTest
@Sql(scripts = "/work-session-repository-test.sql")
class WorkSessionRepositoryTests {

	private static final Instant STARTED_AT = Instant.parse("2026-08-04T09:00:00Z");
	private static final Instant ENDED_AT = Instant.parse("2026-08-04T10:00:00Z");

	@Autowired
	private WorkSessionRepository repository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void insertsCategorySnapshotFromRoom() {
		assertThat(userRepository.lockById(10L)).isTrue();
		assertThat(repository.insertFromRoom(10L, 20L, STARTED_AT)).isOne();

		WorkSession active = repository.findActiveByUserIdForUpdate(10L);

		assertThat(active.userId()).isEqualTo(10L);
		assertThat(active.roomId()).isEqualTo(20L);
		assertThat(active.categoryId()).isEqualTo(30L);
		assertThat(active.startedAt()).isEqualTo(STARTED_AT);
		assertThat(active.endedAt()).isNull();
	}

	@Test
	void endsActiveSession() {
		repository.insertFromRoom(10L, 20L, STARTED_AT);
		WorkSession active = repository.findActiveByUserIdForUpdate(10L);

		assertThat(repository.endById(active.id(), ENDED_AT)).isOne();
		assertThat(repository.findActiveByUserIdForUpdate(10L)).isNull();
	}

	@Test
	void doesNotInsertForMissingRoom() {
		assertThat(repository.insertFromRoom(10L, 999L, STARTED_AT)).isZero();
		assertThat(repository.findActiveByUserIdForUpdate(10L)).isNull();
	}
}
