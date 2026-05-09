package com.example.online_workspace.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.online_workspace.form.MessageForm;
import com.example.online_workspace.form.RoomCreateForm;
import com.example.online_workspace.security.AppUserPrincipal;
import com.example.online_workspace.service.ChatService;
import com.example.online_workspace.service.RoomService;
import com.example.online_workspace.web.RoomViewModel;

@Controller
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;
    private final ChatService chatService;

    public RoomController(RoomService roomService, ChatService chatService) {
        this.roomService = roomService;
        this.chatService = chatService;
    }

    @GetMapping("/new")
    public String newRoom(Model model) {
        if (!model.containsAttribute("roomCreateForm")) {
            model.addAttribute("roomCreateForm", new RoomCreateForm());
        }
        return "rooms/new";
    }

    @PostMapping
    public String create(
        @AuthenticationPrincipal AppUserPrincipal principal,
        @Valid @ModelAttribute("roomCreateForm") RoomCreateForm roomCreateForm,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.roomCreateForm", bindingResult);
            redirectAttributes.addFlashAttribute("roomCreateForm", roomCreateForm);
            return "redirect:/rooms/new";
        }
        try {
            var room = roomService.createRoom(roomCreateForm.getName(), principal.getUserId(), principal.getDisplayName());
            redirectAttributes.addFlashAttribute("successMessage", "ルームを作成しました。");
            return "redirect:/rooms/" + room.getId();
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/rooms/new";
        }
    }

    @PostMapping("/{roomId}/join")
    public String join(
        @PathVariable Long roomId,
        @AuthenticationPrincipal AppUserPrincipal principal,
        RedirectAttributes redirectAttributes
    ) {
        try {
            roomService.joinRoom(roomId, principal.getUserId(), principal.getDisplayName());
            redirectAttributes.addFlashAttribute("successMessage", "ルームに参加しました。");
            return "redirect:/rooms/" + roomId;
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/";
        }
    }

    @PostMapping("/{roomId}/leave")
    public String leave(
        @PathVariable Long roomId,
        @AuthenticationPrincipal AppUserPrincipal principal,
        RedirectAttributes redirectAttributes
    ) {
        try {
            roomService.leaveRoom(roomId, principal.getUserId(), principal.getDisplayName());
            redirectAttributes.addFlashAttribute("successMessage", "ルームから退出しました。");
            return "redirect:/";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/rooms/" + roomId;
        }
    }

    @GetMapping("/{roomId}")
    public String show(
        @PathVariable Long roomId,
        @AuthenticationPrincipal AppUserPrincipal principal,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        RoomViewModel roomView;
        try {
            roomView = roomService.getRoomViewModel(roomId, principal.getUserId());
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/";
        }

        if (!roomView.joined()) {
            redirectAttributes.addFlashAttribute("errorMessage", "先にルームに参加してください。");
            return "redirect:/";
        }

        model.addAttribute("room", roomView.room());
        model.addAttribute("members", roomView.members());
        model.addAttribute("messages", roomView.messages());
        model.addAttribute("messageForm", new MessageForm());
        model.addAttribute("displayName", principal.getDisplayName());
        return "rooms/show";
    }

    @PostMapping("/{roomId}/messages")
    public String postMessage(
        @PathVariable Long roomId,
        @AuthenticationPrincipal AppUserPrincipal principal,
        @Valid @ModelAttribute("messageForm") MessageForm messageForm,
        BindingResult bindingResult,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "メッセージ送信に失敗しました。");
            return "redirect:/rooms/" + roomId;
        }
        try {
            chatService.sendMessage(roomId, principal.getUserId(), principal.getDisplayName(), messageForm.getContent());
            redirectAttributes.addFlashAttribute("successMessage", "メッセージを送信しました。");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/rooms/" + roomId;
    }
}
