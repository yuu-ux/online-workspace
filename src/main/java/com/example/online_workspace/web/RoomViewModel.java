package com.example.online_workspace.web;

import java.util.List;

import com.example.online_workspace.entity.MessageEntity;
import com.example.online_workspace.entity.RoomEntity;
import com.example.online_workspace.entity.RoomMemberEntity;

public record RoomViewModel(
    RoomEntity room,
    List<RoomMemberEntity> members,
    List<MessageEntity> messages,
    boolean joined
) {
}
