package com.example.online_workspace.entity;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class RoomMemberEntity {
    private Long roomId;
    private Long userId;
    private String userName;
    private OffsetDateTime joinedAt;
}
