package com.example.online_workspace.entity;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class MessageEntity {
    private Long id;
    private Long roomId;
    private Long userId;
    private String userName;
    private String content;
    private OffsetDateTime sentAt;
}
