package com.example.online_workspace.dto;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class RoomSummary {
    private Long id;
    private String name;
    private int participantCount;
    private OffsetDateTime createdAt;
}
