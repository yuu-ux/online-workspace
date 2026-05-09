package com.example.online_workspace.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.online_workspace.form.RoomCreateForm;
import com.example.online_workspace.security.AppUserPrincipal;
import com.example.online_workspace.service.RoomService;

@Controller
public class HomeController {

    private final RoomService roomService;

    public HomeController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/")
    public String index(@AuthenticationPrincipal AppUserPrincipal principal, Model model) {
        model.addAttribute("rooms", roomService.listRoomSummaries());
        model.addAttribute("roomCreateForm", new RoomCreateForm());
        model.addAttribute("displayName", principal.getDisplayName());
        return "index";
    }
}
