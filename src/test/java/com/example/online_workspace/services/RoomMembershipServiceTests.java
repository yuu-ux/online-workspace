package com.example.online_workspace.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.server.ResponseStatusException;

import com.example.online_workspace.models.RoomMember;
import com.example.online_workspace.repositories.RoomMembershipRepository;

@MybatisTest
@Sql(scripts = "/room-membership-service-test.sql")
class RoomMembershipServiceTests {

	@Autowired
	private RoomMembershipRepository membershipRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private RoomMembershipService service;

	@BeforeEach
	void setUp() {
		service = new RoomMembershipService(membershipRepository);
	}

	@Test
	void joinsRoom() {
		RoomMember member = service.join(10L, "member@example.com");

		assertThat(member.userId()).isEqualTo(2L);
		assertThat(member.userName()).isEqualTo("member");
		assertThat(member.iconUrl()).isEqualTo("https://example.com/member.png");
		assertThat(jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM room_members WHERE room_id = 10 AND user_id = 2 AND left_at IS NULL",
			Integer.class
		)).isOne();
	}

	@Test
	void rejectsJoinWhenRoomIsFull() {
		assertThatThrownBy(() -> service.join(13L, "member@example.com"))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
	}

	@Test
	void rejectsJoinWhenUserIsActiveInAnotherRoom() {
		jdbcTemplate.update(
			"INSERT INTO room_members (room_id, user_id, joined_at) VALUES (10, 2, CURRENT_TIMESTAMP)"
		);
		assertThatThrownBy(() -> service.join(11L, "member@example.com"))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		assertThat(jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM room_members WHERE user_id = 2 AND left_at IS NULL",
			Integer.class
		)).isOne();
	}

	@Test
	void leavesRoom() {
		service.join(10L, "member@example.com");

		service.leave(10L, "member@example.com");

		assertThat(membershipRepository.hasActiveMembership(2L)).isFalse();
		assertThat(jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM room_members WHERE room_id = 10 AND user_id = 2 AND left_at IS NOT NULL",
			Integer.class
		)).isOne();
	}

}
