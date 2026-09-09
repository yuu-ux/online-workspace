package com.example.online_workspace.controllers.invites;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.online_workspace.services.RoomInviteService;
import com.example.online_workspace.services.RoomInviteService.Invite;
import com.example.online_workspace.services.RoomInviteService.JoinResult;

@RestController
@RequestMapping("/api/v1")
public class RoomInviteController {

	private final RoomInviteService service;

	public RoomInviteController(RoomInviteService service) {
		this.service = service;
	}

	@PostMapping("/rooms/{roomId}/invites")
	@ResponseStatus(HttpStatus.CREATED)
	public InviteResponse create(
		@PathVariable @Positive long roomId,
		Authentication authentication
	) {
		Invite invite = service.createInvite(roomId, authentication.getName());
		return InviteResponse.from(invite);
	}

	@PostMapping("/room-invites/{token}/join")
	public InviteJoinResponse join(
		@PathVariable @NotBlank String token,
		Authentication authentication
	) {
		return InviteJoinResponse.from(service.joinByInvite(token, authentication.getName()));
	}

	public record InviteResponse(String token, String inviteUrl, Instant expiresAt) {
		private static InviteResponse from(Invite invite) {
			return new InviteResponse(
				invite.token(),
				"/invite/" + invite.token(),
				invite.expiresAt()
			);
		}
	}

	public record InviteJoinResponse(long roomId, long membershipId, Instant joinedAt) {
		private static InviteJoinResponse from(JoinResult result) {
			return new InviteJoinResponse(result.roomId(), result.membershipId(), result.joinedAt());
		}
	}
}
