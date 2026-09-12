package com.example.online_workspace.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.server.ResponseStatusException;

import com.example.online_workspace.models.RoomMember;
import com.example.online_workspace.repositories.FriendRepository;
import com.example.online_workspace.repositories.RoomMembershipRepository;

@MybatisTest
@Sql(scripts = "/room-membership-service-test.sql")
class RoomMembershipServiceTests {

	@Autowired
	private RoomMembershipRepository membershipRepository;

	private FriendRepository friendRepository;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	private SimpMessagingTemplate messagingTemplate;
	private RoomMembershipService service;
	private OnlinePresenceService presence;
	private NotificationService notificationService;

	@BeforeEach
	void setUp() {
		messagingTemplate = mock(SimpMessagingTemplate.class);
		friendRepository = mock(FriendRepository.class);
		presence = new OnlinePresenceService(membershipRepository, friendRepository, messagingTemplate);
		notificationService = mock(NotificationService.class);
		service = new RoomMembershipService(membershipRepository, presence, notificationService);
	}

	@Test
	void notifiesFriendsWhenUserBecomesOnline() {
		when(friendRepository.findActiveFriendPresenceRecipients("member@example.com"))
			.thenReturn(List.of(new FriendRepository.FriendPresenceRecipient(2L, "creator@example.com")));

		connected("member-1", "member@example.com");

		ArgumentCaptor<OnlinePresenceService.FriendPresenceEvent> event =
			ArgumentCaptor.forClass(OnlinePresenceService.FriendPresenceEvent.class);
		verify(messagingTemplate).convertAndSendToUser(
			eq("creator@example.com"),
			eq("/queue/friends/presence"),
			event.capture()
		);
		assertThat(event.getValue().type()).isEqualTo("friend:presence_changed");
		assertThat(event.getValue().payload().userId()).isEqualTo(2L);
		assertThat(event.getValue().payload().online()).isTrue();
	}

	@Test
	void notifiesFriendsOnlyWhenTheLastConnectionGoesOffline() {
		when(friendRepository.findActiveFriendPresenceRecipients("member@example.com"))
			.thenReturn(List.of(new FriendRepository.FriendPresenceRecipient(2L, "creator@example.com")));

		Message<byte[]> firstTab = connected("member-1", "member@example.com");
		Message<byte[]> secondTab = connected("member-2", "member@example.com");
		clearInvocations(messagingTemplate);

		presence.disconnected(new SessionDisconnectEvent(this, firstTab, "member-1", CloseStatus.NORMAL));
		verify(messagingTemplate, times(0)).convertAndSendToUser(
			eq("creator@example.com"),
			eq("/queue/friends/presence"),
			org.mockito.ArgumentMatchers.any(OnlinePresenceService.FriendPresenceEvent.class)
		);

		presence.disconnected(new SessionDisconnectEvent(this, secondTab, "member-2", CloseStatus.NORMAL));

		ArgumentCaptor<OnlinePresenceService.FriendPresenceEvent> event =
			ArgumentCaptor.forClass(OnlinePresenceService.FriendPresenceEvent.class);
		verify(messagingTemplate).convertAndSendToUser(
			eq("creator@example.com"),
			eq("/queue/friends/presence"),
			event.capture()
		);
		assertThat(event.getValue().type()).isEqualTo("friend:presence_changed");
		assertThat(event.getValue().payload().userId()).isEqualTo(2L);
		assertThat(event.getValue().payload().online()).isFalse();
	}

	@Test
	void publishesMemberDetailsWhenUserConnectsToRoomWebSocket() {
		service.join(10L, "member@example.com");
		clearInvocations(messagingTemplate);

		connected("member-1", "member@example.com");

		ArgumentCaptor<OnlinePresenceService.RoomPresenceEvent> event =
			ArgumentCaptor.forClass(OnlinePresenceService.RoomPresenceEvent.class);
		verify(messagingTemplate).convertAndSendToUser(
			eq("creator@example.com"),
			eq("/queue/rooms/10/presence"),
			event.capture()
		);
		assertThat(event.getValue().type()).isEqualTo("room:user_joined");
		assertThat(event.getValue().payload().userId()).isEqualTo(2L);
		assertThat(event.getValue().payload().name()).isEqualTo("member");
		assertThat(event.getValue().payload().iconUrl())
			.isEqualTo("https://example.com/member.png");
		assertThat(event.getValue().payload().online()).isTrue();
	}

	@Test
	void listsOnlineMembersForAnyAuthenticatedUser() {
		service.join(10L, "member@example.com");
		Message<byte[]> firstTab = connected("member-1", "member@example.com");
		Message<byte[]> secondTab = connected("member-2", "member@example.com");

		assertThat(service.list(10L, "creator@example.com"))
			.anySatisfy(member -> {
				assertThat(member.member().userId()).isEqualTo(2L);
				assertThat(member.online()).isTrue();
			});

		presence.disconnected(new SessionDisconnectEvent(this, firstTab, "member-1", CloseStatus.NORMAL));
		assertThat(presence.isOnline("member@example.com")).isTrue();
		assertThat(jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM room_members WHERE user_id = 2 AND left_at IS NULL",
			Integer.class
		)).isOne();
		presence.disconnected(new SessionDisconnectEvent(this, secondTab, "member-2", CloseStatus.NORMAL));
		assertThat(presence.isOnline("member@example.com")).isFalse();

		assertThat(service.list(10L, "other@example.com"))
			.extracting(member -> member.member().userId())
			.contains(1L)
			.doesNotContain(2L);
	}

	@Test
	void leavesRoomWhenLastWebSocketConnectionDisconnects() {
		service.join(10L, "member@example.com");
		Message<byte[]> tab = connected("member-1", "member@example.com");
		clearInvocations(messagingTemplate);

		presence.disconnected(new SessionDisconnectEvent(this, tab, "member-1", CloseStatus.NORMAL));

		assertThat(jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM room_members WHERE user_id = 2 AND left_at IS NULL",
			Integer.class
		)).isZero();
		assertThat(jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM room_members WHERE room_id = 10 AND user_id = 2 AND left_at IS NOT NULL",
			Integer.class
		)).isOne();

		ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
		verify(messagingTemplate, times(2)).convertAndSend(
			eq("/topic/rooms/10/presence"),
			events.capture()
		);
		assertThat(events.getAllValues())
			.anySatisfy(event -> {
				assertThat(event).isInstanceOf(OnlinePresenceService.RoomPresenceEvent.class);
				OnlinePresenceService.RoomPresenceEvent presenceEvent =
					(OnlinePresenceService.RoomPresenceEvent) event;
				assertThat(presenceEvent.type()).isEqualTo("room:user_left");
			});
		assertThat(events.getAllValues())
			.anySatisfy(event -> {
				assertThat(event).isInstanceOf(OnlinePresenceService.RoomMemberCountEvent.class);
				OnlinePresenceService.RoomMemberCountEvent countEvent =
					(OnlinePresenceService.RoomMemberCountEvent) event;
				assertThat(countEvent.payload().currentMembers()).isEqualTo(1);
			});
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
	void publishesCurrentMemberCountWhenMembershipChanges() {
		service.join(10L, "member@example.com");

		ArgumentCaptor<Object> joinEvents = ArgumentCaptor.forClass(Object.class);
		verify(messagingTemplate, times(2)).convertAndSend(
			eq("/topic/rooms/10/presence"),
			joinEvents.capture()
		);
		assertThat(joinEvents.getAllValues())
			.anySatisfy(event -> {
				assertThat(event).isInstanceOf(OnlinePresenceService.RoomMemberCountEvent.class);
				OnlinePresenceService.RoomMemberCountEvent count =
					(OnlinePresenceService.RoomMemberCountEvent) event;
				assertThat(count.type()).isEqualTo("room:member_count_changed");
				assertThat(count.payload().roomId()).isEqualTo(10L);
				assertThat(count.payload().currentMembers()).isEqualTo(2);
			});

		clearInvocations(messagingTemplate);
		service.leave(10L, "member@example.com");

		ArgumentCaptor<OnlinePresenceService.RoomMemberCountEvent> leaveEvent =
			ArgumentCaptor.forClass(OnlinePresenceService.RoomMemberCountEvent.class);
		verify(messagingTemplate).convertAndSend(
			eq("/topic/rooms/10/presence"),
			leaveEvent.capture()
		);
		assertThat(leaveEvent.getValue().payload().currentMembers()).isEqualTo(1);
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

	@Test
	void notifiesOtherMembersWhenRoomMemberLeaves() {
		service.join(10L, "member@example.com");
		connected("member-1", "member@example.com");
		clearInvocations(messagingTemplate);

		service.leave(10L, "member@example.com");

		ArgumentCaptor<OnlinePresenceService.RoomPresenceEvent> event =
			ArgumentCaptor.forClass(OnlinePresenceService.RoomPresenceEvent.class);
		verify(messagingTemplate).convertAndSendToUser(
			eq("creator@example.com"),
			eq("/queue/rooms/10/presence"),
			event.capture()
		);
		assertThat(event.getValue().type()).isEqualTo("room:user_left");
		assertThat(event.getValue().payload().roomId()).isEqualTo(10L);
		assertThat(event.getValue().payload().userId()).isEqualTo(2L);
		assertThat(event.getValue().payload().online()).isFalse();
	}

	@Test
	void canListAndRejoinPublicRoomAfterLeaving() {
		service.join(10L, "member@example.com");
		service.leave(10L, "member@example.com");

		assertThat(service.list(10L, "member@example.com"))
			.extracting(member -> member.member().userId())
			.doesNotContain(2L);

		RoomMember rejoined = service.join(10L, "member@example.com");

		assertThat(rejoined.userId()).isEqualTo(2L);
		assertThat(jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM room_members WHERE room_id = 10 AND user_id = 2 AND left_at IS NULL",
			Integer.class
		)).isOne();
	}

	private Message<byte[]> connected(String sessionId, String email) {
		StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECTED);
		accessor.setSessionId(sessionId);
		accessor.setUser(() -> email);
		Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
		presence.connected(new SessionConnectedEvent(this, message));
		return message;
	}

}
