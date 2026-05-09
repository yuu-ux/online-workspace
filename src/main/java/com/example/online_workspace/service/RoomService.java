package com.example.online_workspace.service;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.online_workspace.dto.RoomSummary;
import com.example.online_workspace.entity.MessageEntity;
import com.example.online_workspace.entity.RoomEntity;
import com.example.online_workspace.entity.RoomMemberEntity;
import com.example.online_workspace.repository.MessageRepository;
import com.example.online_workspace.repository.RoomMemberRepository;
import com.example.online_workspace.repository.RoomRepository;
import com.example.online_workspace.web.RoomRealtimeEvent;
import com.example.online_workspace.web.RoomViewModel;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomService(
        RoomRepository roomRepository,
        RoomMemberRepository roomMemberRepository,
        MessageRepository messageRepository,
        SimpMessagingTemplate messagingTemplate
    ) {
        this.roomRepository = roomRepository;
        this.roomMemberRepository = roomMemberRepository;
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public List<RoomSummary> listRoomSummaries() {
        return roomRepository.findAllSummaries();
    }

    @Transactional
    public RoomEntity createRoom(String roomName, Long userId, String userName) {
        RoomEntity room = new RoomEntity();
        room.setName(roomName.trim());
        room.setCreatedBy(userId);
        roomRepository.insert(room);
        roomMemberRepository.addMember(room.getId(), userId);
        publishRoomEvent(room.getId(), "room:user_joined", userName);
        publishRoomSummaryUpdate(room.getId());
        return room;
    }

    @Transactional
    public void joinRoom(Long roomId, Long userId, String userName) {
        roomRepository.findById(roomId).orElseThrow(() -> new IllegalArgumentException("ルームが見つかりません。"));
        roomMemberRepository.addMember(roomId, userId);
        publishRoomEvent(roomId, "room:user_joined", userName);
        publishRoomSummaryUpdate(roomId);
    }

    @Transactional
    public void leaveRoom(Long roomId, Long userId, String userName) {
        roomRepository.findById(roomId).orElseThrow(() -> new IllegalArgumentException("ルームが見つかりません。"));
        int deleted = roomMemberRepository.removeMember(roomId, userId);
        if (deleted == 0) {
            throw new IllegalArgumentException("このルームに参加していません。");
        }
        publishRoomEvent(roomId, "room:user_left", userName);
        publishRoomSummaryUpdate(roomId);
    }

    public RoomViewModel getRoomViewModel(Long roomId, Long userId) {
        RoomEntity room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("ルームが見つかりません。"));

        boolean joined = roomMemberRepository.countMember(roomId, userId) > 0;
        List<RoomMemberEntity> members = roomMemberRepository.findMembersByRoomId(roomId);
        List<MessageEntity> messages = messageRepository.findByRoomId(roomId);
        return new RoomViewModel(room, members, messages, joined);
    }

    private void publishRoomEvent(Long roomId, String type, String actorName) {
        int participants = roomMemberRepository.countParticipants(roomId);
        messagingTemplate.convertAndSend(
            "/topic/rooms/" + roomId,
            new RoomRealtimeEvent(type, roomId, actorName, participants)
        );
    }

    private void publishRoomSummaryUpdate(Long roomId) {
        int participants = roomMemberRepository.countParticipants(roomId);
        messagingTemplate.convertAndSend(
            "/topic/rooms",
            new RoomRealtimeEvent("room:updated", roomId, null, participants)
        );
    }
}
