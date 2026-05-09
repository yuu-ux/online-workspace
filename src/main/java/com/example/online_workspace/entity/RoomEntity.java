package com.example.online_workspace.entity;

import java.time.OffsetDateTime;

import lombok.Data;

@Data
public class RoomEntity {
    private Long id;
    private String name;
    private Long createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
