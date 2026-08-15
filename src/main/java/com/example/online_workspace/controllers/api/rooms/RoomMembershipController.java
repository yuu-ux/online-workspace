package com.example.online_workspace.controllers.api.rooms;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.online_workspace.models.RoomMember;
import com.example.online_workspace.services.RoomMembershipService;
import com.example.online_workspace.services.RoomMembershipService.JoinResult;

@RestController
@RequestMapping("/api/v1")
public class RoomMembershipController {

	private final RoomMembershipService service;

	public RoomMembershipController(RoomMembershipService service) {
		this.service = service;
	}

	@PostMapping("/rooms/{roomId}/members/me")
	@ResponseStatus(HttpStatus.CREATED)
	public RoomMemberResponse join(@PathVariable long roomId, Authentication authentication) {
		return RoomMemberResponse.from(service.join(roomId, authentication.getName()));
	}

	@PostMapping("/invites/{token}/join")
	@ResponseStatus(HttpStatus.CREATED)
	public JoinRoomResponse joinByInvite(@PathVariable String token, Authentication authentication) {
		JoinResult result = service.joinByInvite(token, authentication.getName());
		return new JoinRoomResponse(result.roomId(), RoomMemberResponse.from(result.membership()));
	}

	@DeleteMapping("/rooms/{roomId}/members/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void leave(@PathVariable long roomId, Authentication authentication) {
		service.leave(roomId, authentication.getName());
	}

	public record RoomMemberResponse(long membershipId, UserSummary user, Instant joinedAt) {

		private static RoomMemberResponse from(RoomMember member) {
			return new RoomMemberResponse(
				member.membershipId(),
				new UserSummary(member.userId(), member.userName(), member.iconUrl()),
				member.joinedAt()
			);
		}
	}

	public record UserSummary(long id, String name, String iconUrl) {
	}

	public record JoinRoomResponse(long roomId, RoomMemberResponse membership) {
	}
}
