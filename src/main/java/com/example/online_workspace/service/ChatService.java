package com.example.online_workspace.service;

import java.time.OffsetDateTime;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_workspace.entity.MessageEntity;
import com.example.online_workspace.repository.MessageRepository;
import com.example.online_workspace.repository.RoomMemberRepository;
import com.example.online_workspace.web.ChatMessagePayload;

@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatService(
        MessageRepository messageRepository,
        RoomMemberRepository roomMemberRepository,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.messageRepository = messageRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void sendMessage(Long roomId, Long userId, String userName, String content) {
        if (roomMemberRepository.countMember(roomId, userId) == 0) {
            throw new IllegalArgumentException("ルームに参加していないため送信できません。");
        }
        String normalized = content == null ? "" : content.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("空のメッセージは送信できません。");
        }
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("メッセージは500文字以内で入力してください。");
        }

        MessageEntity message = new MessageEntity();
        message.setRoomId(roomId);
        message.setUserId(userId);
        message.setContent(normalized);
        message.setSentAt(OffsetDateTime.now());
        messageRepository.insert(message);

        messagingTemplate.convertAndSend(
            "/topic/rooms/" + roomId,
            new ChatMessagePayload("chat:message", userName, normalized, message.getSentAt())
        );
    }
}
