package com.example.online_workspace.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.example.online_workspace.exceptions.ApiException;
import com.example.online_workspace.models.RoomMember;
import com.example.online_workspace.repositories.RoomInviteRepository;
import com.example.online_workspace.repositories.RoomInviteRepository.InviteRow;

@Service
public class RoomInviteService {

	private final RoomInviteRepository repository;
	private final RoomMembershipService membershipService;

	public RoomInviteService(RoomInviteRepository repository, RoomMembershipService membershipService) {
		this.repository = repository;
		this.membershipService = membershipService;
	}

	@Transactional
	public Invite createInvite(long roomId, String email) {
		long userId = requireActiveUserId(email);
		if (!repository.isActiveRoomMember(roomId, userId)) {
			throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "この操作を行う権限がありません。");
		}

		String token = UUID.randomUUID().toString().replace("-", "");
		Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
		if (repository.insertInvite(roomId, userId, token, expiresAt) != 1) {
			throw new IllegalStateException("Invite was not created");
		}
		return new Invite(roomId, token, expiresAt);
	}

	@Transactional
	public JoinResult joinByInvite(String token, String email) {
		long userId = requireActiveUserId(email);
		InviteRow invite = repository.findByTokenForUpdate(token);
		if (invite == null) {
			throw new ApiException(HttpStatus.NOT_FOUND, "INVITE_NOT_FOUND", "招待リンクが見つかりません。");
		}
		if (invite.invalidatedAt() != null || invite.expiresAt().isBefore(Instant.now())) {
			throw new ApiException(HttpStatus.CONFLICT, "INVITE_EXPIRED", "招待リンクの有効期限が切れています。");
		}

		RoomMember membership;
		try {
			membership = membershipService.join(invite.roomId(), email);
		} catch (ResponseStatusException ex) {
			String reason = ex.getReason() == null ? "招待リンクで参加できませんでした。" : ex.getReason();
			throw new ApiException(HttpStatus.valueOf(ex.getStatusCode().value()), "INVITE_JOIN_FAILED", reason);
		}

		if (repository.invalidate(invite.id()) != 1) {
			throw new IllegalStateException("Invite was not invalidated");
		}
		return new JoinResult(
			invite.roomId(),
			userId,
			membership.membershipId(),
			membership.joinedAt()
		);
	}

	private long requireActiveUserId(String email) {
		if (email == null || email.isBlank()) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。");
		}
		Long userId = repository.findActiveUserIdByEmailForUpdate(email);
		if (userId == null) {
			throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "認証が必要です。");
		}
		return userId;
	}

	public record Invite(long roomId, String token, Instant expiresAt) {
	}

	public record JoinResult(long roomId, long userId, long membershipId, Instant joinedAt) {
	}
}
