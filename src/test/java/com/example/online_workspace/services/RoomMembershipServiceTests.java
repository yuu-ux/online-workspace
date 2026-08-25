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
import com.example.online_workspace.repositories.users.UserRepository;
import com.example.online_workspace.repositories.WorkSessionRepository;

@MybatisTest
@Sql(scripts = "/room-membership-service-test.sql")
class RoomMembershipServiceTests {

	@Autowired
	private RoomMembershipRepository membershipRepository;

	@Autowired
	private WorkSessionRepository workSessionRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private RoomMembershipService service;

	@BeforeEach
	void setUp() {
		service = new RoomMembershipService(
			membershipRepository,
			new WorkSessionService(workSessionRepository, userRepository)
		);
	}

	@Test
	void joinsPublicRoomAndStartsWorkSession() {
		RoomMember member = service.join(10L, "member@example.com");

		assertThat(member.userId()).isEqualTo(2L);
		assertThat(member.userName()).isEqualTo("member");
		assertThat(member.iconUrl()).isEqualTo("https://example.com/member.png");
		assertThat(jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM room_members WHERE room_id = 10 AND user_id = 2 AND left_at IS NULL",
			Integer.class
		)).isOne();
		assertThat(workSessionRepository.findActiveByUserIdForUpdate(2L).roomId()).isEqualTo(10L);
	}

	@Test
	void onlyCreatorFriendCanJoinFriendsOnlyRoom() {
		assertThat(service.join(11L, "member@example.com").userId()).isEqualTo(2L);

		assertThatThrownBy(() -> service.join(11L, "other@example.com"))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
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
		jdbcTemplate.update("""
			INSERT INTO work_sessions (user_id, room_id, category_id, started_at)
			VALUES (2, 10, 100, CURRENT_TIMESTAMP)
			""");

		assertThatThrownBy(() -> service.join(11L, "member@example.com"))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
		assertThat(jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM room_members WHERE user_id = 2 AND left_at IS NULL",
			Integer.class
		)).isOne();
		assertThat(workSessionRepository.findActiveByUserIdForUpdate(2L).roomId()).isEqualTo(10L);
	}

	@Test
	void rejectsJoinWhenEitherUserHasBlockedTheOther() {
		jdbcTemplate.update(
			"INSERT INTO blocks (blocker_user_id, blocked_user_id) VALUES (?, ?)",
			1L,
			2L
		);

		assertThatThrownBy(() -> service.join(14L, "member@example.com"))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));

		jdbcTemplate.update("DELETE FROM blocks");
		jdbcTemplate.update(
			"INSERT INTO blocks (blocker_user_id, blocked_user_id) VALUES (?, ?)",
			2L,
			1L
		);

		assertThatThrownBy(() -> service.join(14L, "member@example.com"))
			.isInstanceOfSatisfying(ResponseStatusException.class,
				exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@Test
	void leavingRoomEndsMembershipAndWorkSession() {
		service.join(10L, "member@example.com");

		service.leave(10L, "member@example.com");

		assertThat(membershipRepository.hasActiveMembership(2L)).isFalse();
		assertThat(workSessionRepository.findActiveByUserIdForUpdate(2L)).isNull();
		assertThat(jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM room_members WHERE room_id = 10 AND user_id = 2 AND left_at IS NOT NULL",
			Integer.class
		)).isOne();
	}

}
