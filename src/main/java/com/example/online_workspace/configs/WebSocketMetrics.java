package com.example.online_workspace.configs;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class WebSocketMetrics {

	private final Set<String> sessions = ConcurrentHashMap.newKeySet();

	public WebSocketMetrics(MeterRegistry meterRegistry) {
		Gauge.builder("websocket.connections.active", sessions, Set::size)
			.description("Current active WebSocket STOMP connections")
			.register(meterRegistry);
	}

	@EventListener
	public void connected(SessionConnectEvent event) {
		String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
		if (sessionId != null) {
			sessions.add(sessionId);
		}
	}

	@EventListener
	public void disconnected(SessionDisconnectEvent event) {
		sessions.remove(event.getSessionId());
	}
}
