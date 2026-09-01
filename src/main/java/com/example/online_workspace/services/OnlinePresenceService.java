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
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.example.online_workspace.repositories.RoomMembershipRepository;
import com.example.online_workspace.repositories.RoomMembershipRepository.ActivePresence;
import com.example.online_workspace.services.auth.EmailNormalizer;

@Service
public class OnlinePresenceService {

	// ponytail: process-local presence; use a shared store if the app runs on multiple instances.
	private final Map<String, String> usersBySession = new ConcurrentHashMap<>();
	private final Map<String, Set<String>> sessionsByUser = new ConcurrentHashMap<>();
	private final RoomMembershipRepository repository;
	private final SimpMessagingTemplate messagingTemplate;

	public OnlinePresenceService(
		RoomMembershipRepository repository,
		SimpMessagingTemplate messagingTemplate
	) {
		this.repository = repository;
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
			publish(email, "room:user_joined", true);
		}
	}

	@EventListener
	public void disconnected(SessionDisconnectEvent event) {
		String email = usersBySession.remove(event.getSessionId());
		if (email != null && removeSession(email, event.getSessionId())) {
			publish(email, "room:user_left", false);
		}
	}

	public boolean isOnline(String email) {
		return email != null && !email.isBlank()
			&& sessionsByUser.containsKey(EmailNormalizer.normalize(email));
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
		RoomPresence payload = new RoomPresence(
			presence.roomId(),
			presence.userId(),
			online,
			Instant.now()
		);
		RoomPresenceEvent event = new RoomPresenceEvent(type, payload);
		String destination = "/queue/rooms/" + presence.roomId() + "/presence";
		repository.findActiveMemberEmails(presence.roomId())
			.forEach(memberEmail -> messagingTemplate.convertAndSendToUser(memberEmail, destination, event));
	}

	public record RoomPresence(long roomId, long userId, boolean online, Instant occurredAt) {
	}

	public record RoomPresenceEvent(String type, RoomPresence payload) {
	}
}
