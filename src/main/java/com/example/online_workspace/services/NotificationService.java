package com.example.online_workspace.services;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

	private static final String DESTINATION = "/queue/notifications";

	private final SimpMessagingTemplate messagingTemplate;

	public NotificationService(SimpMessagingTemplate messagingTemplate) {
		this.messagingTemplate = messagingTemplate;
	}

	public void publishTo(String email, String message) {
		messagingTemplate.convertAndSendToUser(
			email,
			DESTINATION,
			new NotificationEvent("notification", message)
		);
	}

	public record NotificationEvent(String type, String message) {
	}
}
