package com.example.online_workspace.services;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_workspace.exceptions.ApiException;
import com.example.online_workspace.models.ChatMessage;
import com.example.online_workspace.models.ChatMessage.UserSummary;
import com.example.online_workspace.models.WithdrawalAccount;
import com.example.online_workspace.repositories.AccountWithdrawalRepository;
import com.example.online_workspace.repositories.ChatMessageRepository;
import com.example.online_workspace.repositories.ChatMessageRepository.Draft;
import com.example.online_workspace.repositories.ChatMessageRepository.Row;

@Service
public class ChatMessageService {

	private final ChatMessageRepository repository;
	private final AccountWithdrawalRepository accountRepository;
	private final SimpMessagingTemplate messagingTemplate;

	public ChatMessageService(
		ChatMessageRepository repository,
		AccountWithdrawalRepository accountRepository,
		SimpMessagingTemplate messagingTemplate
	) {
		this.repository = repository;
		this.accountRepository = accountRepository;
		this.messagingTemplate = messagingTemplate;
	}

	@Transactional
	public ChatMessage create(long roomId, String email, String content) {
		long userId = requireActiveMember(roomId, email);
		Draft draft = new Draft(roomId, userId, content);
		if (repository.insert(draft) != 1 || draft.getId() == null) {
			throw new IllegalStateException("Message was not created");
		}
		Row created = repository.findById(draft.getId());
		if (created == null) {
			throw new IllegalStateException("Created message was not found");
		}
		return toMessage(created);
	}

	@Transactional(readOnly = true)
	public Result list(long roomId, String email, int page, int size) {
		requireActiveMember(roomId, email);
		long totalElements = repository.countByRoomId(roomId);
		long offset = (long) page * size;
		List<ChatMessage> messages = offset >= totalElements
			? List.of()
			: repository.findByRoomId(roomId, size, offset).stream().map(this::toMessage).toList();
		return new Result(messages, totalElements);
	}

	public void publish(ChatMessage message) {
		ChatMessageEvent event = new ChatMessageEvent("chat:message", message);
		String destination = "/queue/rooms/" + message.roomId() + "/messages";
		repository.findActiveMemberEmails(message.roomId())
			.forEach(email -> messagingTemplate.convertAndSendToUser(email, destination, event));
	}

	private long requireActiveMember(long roomId, String email) {
		WithdrawalAccount account = accountRepository.findActiveByEmail(email)
			.orElseThrow(() -> new ApiException(
				HttpStatus.UNAUTHORIZED,
				"UNAUTHORIZED",
				"認証が必要です。"
			));
		if (roomId <= 0 || !repository.roomExists(roomId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "ROOM_NOT_FOUND", "ルームが見つかりません。");
		}
		if (!repository.isActiveMember(roomId, account.id())) {
			throw new ApiException(
				HttpStatus.FORBIDDEN,
				"ROOM_MEMBERSHIP_REQUIRED",
				"ルーム参加者だけがチャットを利用できます。"
			);
		}
		return account.id();
	}

	private ChatMessage toMessage(Row row) {
		return new ChatMessage(
			row.id(),
			row.roomId(),
			new UserSummary(row.senderId(), row.senderName(), row.senderIconUrl()),
			row.content(),
			row.sentAt()
		);
	}

	public record Result(List<ChatMessage> items, long totalElements) {
	}

	public record ChatMessageEvent(String type, ChatMessage payload) {
	}
}
