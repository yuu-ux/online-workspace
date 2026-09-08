package com.example.online_workspace.repositories;

import java.time.Instant;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface RoomInviteRepository {

	@Select("""
		SELECT u.id
		FROM users u
		JOIN account_statuses s ON s.id = u.account_status_id
		WHERE u.email = #{email}
		  AND u.deleted_at IS NULL
		  AND s.code = 'ACTIVE'
		  AND (u.suspended_until IS NULL OR u.suspended_until <= CURRENT_TIMESTAMP)
		FOR UPDATE
		""")
	Long findActiveUserIdByEmailForUpdate(@Param("email") String email);

	@Select("""
		SELECT EXISTS (
			SELECT 1
			FROM room_members rm
			WHERE rm.room_id = #{roomId}
			  AND rm.user_id = #{userId}
			  AND rm.left_at IS NULL
		)
		""")
	boolean isActiveRoomMember(@Param("roomId") long roomId, @Param("userId") long userId);

	@Insert("""
		INSERT INTO room_invites (room_id, created_by, token, expires_at)
		VALUES (#{roomId}, #{createdBy}, #{token}, #{expiresAt})
		""")
	int insertInvite(
		@Param("roomId") long roomId,
		@Param("createdBy") long createdBy,
		@Param("token") String token,
		@Param("expiresAt") Instant expiresAt
	);

	@Select("""
		SELECT id, room_id, token, expires_at, invalidated_at
		FROM room_invites
		WHERE token = #{token}
		FOR UPDATE
		""")
	InviteRow findByTokenForUpdate(@Param("token") String token);

	@Update("""
		UPDATE room_invites
		SET invalidated_at = CURRENT_TIMESTAMP
		WHERE id = #{inviteId}
		  AND invalidated_at IS NULL
		""")
	int invalidate(@Param("inviteId") long inviteId);

	record InviteRow(
		long id,
		long roomId,
		String token,
		Instant expiresAt,
		Instant invalidatedAt
	) {
	}
}
