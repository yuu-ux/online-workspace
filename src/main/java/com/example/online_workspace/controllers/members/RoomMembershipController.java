package com.example.online_workspace.controllers.members;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.online_workspace.models.RoomMember;
import com.example.online_workspace.services.RoomMembershipService;
import com.example.online_workspace.services.RoomMembershipService.OnlineRoomMember;

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
		return RoomMemberResponse.from(
			service.join(roomId, authentication.getName()),
			service.isOnline(authentication.getName())
		);
	}

	@GetMapping("/rooms/{roomId}/members")
	public List<RoomMemberResponse> list(@PathVariable long roomId, Authentication authentication) {
		return service.list(roomId, authentication.getName()).stream()
			.map(RoomMemberResponse::from)
			.toList();
	}

	@DeleteMapping("/rooms/{roomId}/members/me")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void leave(@PathVariable long roomId, Authentication authentication) {
		service.leave(roomId, authentication.getName());
	}

	public record RoomMemberResponse(long membershipId, UserSummary user, boolean online, Instant joinedAt) {

		private static RoomMemberResponse from(OnlineRoomMember member) {
			return from(member.member(), member.online());
		}

		private static RoomMemberResponse from(RoomMember member, boolean online) {
			return new RoomMemberResponse(
				member.membershipId(),
				new UserSummary(member.userId(), member.userName(), member.iconUrl()),
				online,
				member.joinedAt()
			);
		}
	}

	public record UserSummary(long id, String name, String iconUrl) {
	}

}
