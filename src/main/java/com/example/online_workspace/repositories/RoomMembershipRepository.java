package com.example.online_workspace.repositories;

import java.time.Instant;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.online_workspace.models.RoomMember;

@Mapper
public interface RoomMembershipRepository {

	@Select("SELECT CURRENT_TIMESTAMP")
	Instant currentTimestamp();

	@Select("""
		SELECT id FROM users
		WHERE email = #{email} AND deleted_at IS NULL
		FOR UPDATE
		""")
	Long findActiveUserIdByEmailForUpdate(@Param("email") String email);

	@Select("""
		SELECT r.id, r.max_members, rs.code AS status
		FROM rooms r
		JOIN room_statuses rs ON rs.id = r.status_id
		WHERE r.id = #{roomId}
		FOR UPDATE OF r
		""")
	JoinPolicy lockRoomById(@Param("roomId") long roomId);

	@Select("""
		SELECT EXISTS (
			SELECT 1 FROM room_members
			WHERE user_id = #{userId}
			  AND left_at IS NULL
		)
		""")
	boolean hasActiveMembership(@Param("userId") long userId);

	@Select("""
		SELECT COUNT(*) FROM room_members
		WHERE room_id = #{roomId} AND left_at IS NULL
		""")
	int countActiveMembers(@Param("roomId") long roomId);

	@Select("""
		SELECT EXISTS (
			SELECT 1 FROM room_members rm
			JOIN blocks b
			  ON (b.blocker_user_id = #{userId} AND b.blocked_user_id = rm.user_id)
			  OR (b.blocked_user_id = #{userId} AND b.blocker_user_id = rm.user_id)
			WHERE rm.room_id = #{roomId} AND rm.left_at IS NULL
		)
		""")
	boolean hasBlockConflict(@Param("roomId") long roomId, @Param("userId") long userId);

	@Insert("""
		INSERT INTO room_members (room_id, user_id, joined_at)
		VALUES (#{roomId}, #{userId}, #{joinedAt})
		""")
	int insertMember(
		@Param("roomId") long roomId,
		@Param("userId") long userId,
		@Param("joinedAt") Instant joinedAt
	);

	@Select("""
		SELECT rm.id AS membership_id, u.id AS user_id, u.name AS user_name,
		       p.icon_url, rm.joined_at
		FROM room_members rm
		JOIN users u ON u.id = rm.user_id
		LEFT JOIN profiles p ON p.user_id = u.id
		WHERE rm.room_id = #{roomId}
		  AND rm.user_id = #{userId}
		  AND rm.left_at IS NULL
		""")
	RoomMember findActiveMember(@Param("roomId") long roomId, @Param("userId") long userId);

	@Select("""
		SELECT id FROM room_members
		WHERE room_id = #{roomId}
		  AND user_id = #{userId}
		  AND left_at IS NULL
		FOR UPDATE
		""")
	Long findActiveMembershipIdForUpdate(@Param("roomId") long roomId, @Param("userId") long userId);

	@Update("""
		UPDATE room_members SET left_at = #{leftAt}
		WHERE id = #{membershipId} AND left_at IS NULL
		""")
	int leave(@Param("membershipId") long membershipId, @Param("leftAt") Instant leftAt);

	record JoinPolicy(long id, int maxMembers, String status) {
	}
}
