package com.example.online_workspace.web;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomRealtimeEvent {
    private String type;
    private Long roomId;
    private String actorName;
    private int participantCount;
}
