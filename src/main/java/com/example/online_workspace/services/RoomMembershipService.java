package com.example.online_workspace.services;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.online_workspace.models.RoomMember;
import com.example.online_workspace.repositories.RoomMembershipRepository;
import com.example.online_workspace.repositories.RoomMembershipRepository.JoinPolicy;

@Service
public class RoomMembershipService {

	private final RoomMembershipRepository repository;
	private final WorkSessionService workSessionService;

	public RoomMembershipService(RoomMembershipRepository repository, WorkSessionService workSessionService) {
		this.repository = repository;
		this.workSessionService = workSessionService;
	}

	@Transactional
	public RoomMember join(long roomId, String userEmail) {
		Instant joinedAt = repository.currentTimestamp();
		long userId = requireUserId(userEmail);
		JoinPolicy room = lockOpenRoom(roomId);

		if ("INVITE_ONLY".equals(room.visibility())) {
			throw forbidden("An invitation is required");
		}
		if ("FRIENDS_ONLY".equals(room.visibility())
			&& userId != room.createdBy()
			&& !repository.isCreatorFriend(room.createdBy(), userId)) {
			throw forbidden("The room is limited to the creator's friends");
		}

		return joinLockedRoom(room, userId, joinedAt);
	}

	@Transactional
	public JoinResult joinByInvite(String token, String userEmail) {
		Instant joinedAt = repository.currentTimestamp();
		long userId = requireUserId(userEmail);
		Long roomId = repository.findInvitedRoomId(token);
		if (roomId == null) {
			throw notFound("Invitation not found");
		}

		JoinPolicy room = lockOpenRoom(roomId);
		if (!repository.isInviteValid(token, roomId, joinedAt)) {
			throw notFound("Invitation not found");
		}

		return new JoinResult(roomId, joinLockedRoom(room, userId, joinedAt));
	}

	@Transactional
	public void leave(long roomId, String userEmail) {
		Instant leftAt = Instant.now();
		long userId = requireUserId(userEmail);
		Long membershipId = repository.findActiveMembershipIdForUpdate(roomId, userId);
		if (membershipId == null) {
			throw notFound("Active room membership not found");
		}
		if (repository.leave(membershipId, leftAt) != 1) {
			throw conflict("The room membership could not be ended");
		}
		workSessionService.end(userId, roomId, leftAt);
	}

	private RoomMember joinLockedRoom(JoinPolicy room, long userId, Instant joinedAt) {
		if (repository.hasActiveMembership(userId)) {
			throw conflict("The user is already in a room");
		}
		if (repository.hasBlockConflict(room.id(), userId)) {
			throw forbidden("A block relationship prevents joining this room");
		}
		if (repository.countActiveMembers(room.id()) >= room.maxMembers()) {
			throw conflict("The room is full");
		}
		if (repository.insertMember(room.id(), userId, joinedAt) != 1) {
			throw conflict("The room membership could not be created");
		}

		workSessionService.start(userId, room.id(), joinedAt);
		RoomMember member = repository.findActiveMember(room.id(), userId);
		if (member == null) {
			throw conflict("The room membership was not created");
		}
		return member;
	}

	private long requireUserId(String email) {
		if (email == null || email.isBlank()) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		Long userId = repository.findActiveUserIdByEmailForUpdate(email);
		if (userId == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		return userId;
	}

	private JoinPolicy lockOpenRoom(long roomId) {
		if (roomId <= 0) {
			throw notFound("Room not found");
		}
		JoinPolicy room = repository.lockRoomById(roomId);
		if (room == null) {
			throw notFound("Room not found");
		}
		if (!"OPEN".equals(room.status())) {
			throw conflict("The room is closed");
		}
		return room;
	}

	private ResponseStatusException forbidden(String reason) {
		return new ResponseStatusException(HttpStatus.FORBIDDEN, reason);
	}

	private ResponseStatusException notFound(String reason) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, reason);
	}

	private ResponseStatusException conflict(String reason) {
		return new ResponseStatusException(HttpStatus.CONFLICT, reason);
	}

	public record JoinResult(long roomId, RoomMember membership) {
	}
}
