package com.example.online_workspace.services;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.example.online_workspace.repositories.FriendRepository;
import com.example.online_workspace.repositories.FriendRepository.FriendPresenceRecipient;
import com.example.online_workspace.repositories.RoomMembershipRepository;
import com.example.online_workspace.repositories.RoomMembershipRepository.ActivePresence;
import com.example.online_workspace.models.RoomMember;
import com.example.online_workspace.services.auth.EmailNormalizer;

@Service
public class OnlinePresenceService {

	// ponytail: process-local presence; use a shared store if the app runs on multiple instances.
	private final Map<String, String> usersBySession = new ConcurrentHashMap<>();
	private final Map<String, Set<String>> sessionsByUser = new ConcurrentHashMap<>();
	private final RoomMembershipRepository repository;
	private final FriendRepository friendRepository;
	private final SimpMessagingTemplate messagingTemplate;

	public OnlinePresenceService(
		RoomMembershipRepository repository,
		FriendRepository friendRepository,
		SimpMessagingTemplate messagingTemplate
	) {
		this.repository = repository;
		this.friendRepository = friendRepository;
		this.messagingTemplate = messagingTemplate;
	}

	@EventListener
	public void connected(SessionConnectedEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		Principal user = accessor.getUser();
		String sessionId = accessor.getSessionId();
		if (user == null || user.getName() == null || user.getName().isBlank() || sessionId == null) {
			return;
		}

		String email = EmailNormalizer.normalize(user.getName());
		String previousEmail = usersBySession.put(sessionId, email);
		if (previousEmail != null && !previousEmail.equals(email)) {
			if (removeSession(previousEmail, sessionId)) {
				publish(previousEmail, "room:user_left", false);
			}
		}
		if (addSession(email, sessionId)) {
			publishFriendPresence(email, true);
			publish(email, "room:user_joined", true);
		}
	}

	@EventListener
	@Transactional
	public void disconnected(SessionDisconnectEvent event) {
		String email = usersBySession.remove(event.getSessionId());
		if (email != null && removeSession(email, event.getSessionId())) {
			publishFriendPresence(email, false);
			ActivePresence presence = repository.findActivePresence(email);
			RoomMember member = presence == null
				? null
				: repository.findActiveMember(presence.roomId(), presence.userId());
			if (presence != null && repository.leaveActiveMembership(
				presence.roomId(),
				presence.userId(),
				Instant.now()
			) == 1) {
				if (member != null) {
					publish(email, "room:user_left", false, presence, member);
				}
				publishRoomMemberCountChanged(presence.roomId());
			}
		}
	}

	public boolean isOnline(String email) {
		return email != null && !email.isBlank()
			&& sessionsByUser.containsKey(EmailNormalizer.normalize(email));
	}

	private void publishFriendPresence(String email, boolean online) {
		for (FriendPresenceRecipient recipient : friendRepository.findActiveFriendPresenceRecipients(email)) {
			messagingTemplate.convertAndSendToUser(
				recipient.recipientEmail(),
				"/queue/friends/presence",
				new FriendPresenceEvent(
					"friend:presence_changed",
					new FriendPresence(recipient.sourceUserId(), online)
				)
			);
		}
	}

	public void publishRoomLeft(String email, long roomId, long userId, RoomMember member) {
		if (isOnline(email) && member != null) {
			publish(
				email,
				"room:user_left",
				false,
				new ActivePresence(roomId, userId),
				member
			);
		}
	}

	public void publishRoomJoined(String email, long roomId, RoomMember member) {
		if (member != null) {
			publish(
				email,
				"room:user_joined",
				true,
				new ActivePresence(roomId, member.userId()),
				member
			);
		}
	}

	public void publishRoomMemberCountChanged(long roomId) {
		RoomMemberCount payload = new RoomMemberCount(
			roomId,
			repository.countActiveMembers(roomId)
		);
		messagingTemplate.convertAndSend(
			"/topic/rooms/" + roomId + "/presence",
			new RoomMemberCountEvent("room:member_count_changed", payload)
		);
	}

	private boolean addSession(String email, String sessionId) {
		AtomicBoolean firstSession = new AtomicBoolean();
		sessionsByUser.compute(email, (ignored, sessions) -> {
			Set<String> current = sessions == null ? ConcurrentHashMap.newKeySet() : sessions;
			firstSession.set(current.add(sessionId) && current.size() == 1);
			return current;
		});
		return firstSession.get();
	}

	private boolean removeSession(String email, String sessionId) {
		AtomicBoolean lastSession = new AtomicBoolean();
		sessionsByUser.computeIfPresent(email, (ignored, sessions) -> {
			sessions.remove(sessionId);
			lastSession.set(sessions.isEmpty());
			return lastSession.get() ? null : sessions;
		});
		return lastSession.get();
	}

	private void publish(String email, String type, boolean online) {
		ActivePresence presence = repository.findActivePresence(email);
		if (presence == null) {
			return;
		}
		publish(email, type, online, presence);
	}

	private void publish(String email, String type, boolean online, ActivePresence presence) {
		RoomMember member = repository.findActiveMember(presence.roomId(), presence.userId());
		if (member == null) {
			return;
		}
		publish(email, type, online, presence, member);
	}

	private void publish(
		String email,
		String type,
		boolean online,
		ActivePresence presence,
		RoomMember member
	) {
		RoomPresence payload = new RoomPresence(
			presence.roomId(),
			presence.userId(),
			member.userName(),
			member.iconUrl(),
			online,
			Instant.now()
		);
		RoomPresenceEvent event = new RoomPresenceEvent(type, payload);
		String destination = "/queue/rooms/" + presence.roomId() + "/presence";
		messagingTemplate.convertAndSend(
			"/topic/rooms/" + presence.roomId() + "/presence",
			event
		);
		repository.findActiveMemberEmails(presence.roomId())
			.forEach(memberEmail -> messagingTemplate.convertAndSendToUser(memberEmail, destination, event));
	}

	public record RoomPresence(
		long roomId,
		long userId,
		String name,
		String iconUrl,
		boolean online,
		Instant occurredAt
	) {
	}

	public record RoomPresenceEvent(String type, RoomPresence payload) {
	}

	public record FriendPresence(long userId, boolean online) {
	}

	public record FriendPresenceEvent(String type, FriendPresence payload) {
	}

	public record RoomMemberCount(long roomId, int currentMembers) {
	}

	public record RoomMemberCountEvent(String type, RoomMemberCount payload) {
	}
}
