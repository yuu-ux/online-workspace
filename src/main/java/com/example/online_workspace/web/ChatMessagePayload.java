package com.example.online_workspace.web;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatMessagePayload {
    private String type;
    private String senderName;
    private String content;
    private OffsetDateTime sentAt;
}
