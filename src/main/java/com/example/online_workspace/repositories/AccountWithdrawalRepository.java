package com.example.online_workspace.repositories;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.example.online_workspace.models.WithdrawalAccount;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AccountWithdrawalRepository {

	@Select("""
		SELECT id, password_hash
		FROM users
		WHERE email = #{email}
		  AND deleted_at IS NULL
		""")
	Optional<WithdrawalAccount> findActiveByEmail(@Param("email") String email);

	@Update("""
		UPDATE users
		SET deleted_at = #{withdrawnAt},
		    updated_at = #{withdrawnAt}
		WHERE id = #{userId}
		  AND deleted_at IS NULL
		""")
	int markWithdrawn(
		@Param("userId") long userId,
		@Param("withdrawnAt") OffsetDateTime withdrawnAt
	);

	@Update("""
		UPDATE room_members
		SET left_at = #{withdrawnAt}
		WHERE user_id = #{userId}
		  AND left_at IS NULL
		""")
	int leaveActiveRooms(
		@Param("userId") long userId,
		@Param("withdrawnAt") OffsetDateTime withdrawnAt
	);

	@Update("""
		UPDATE work_sessions
		SET ended_at = #{withdrawnAt},
		    updated_at = #{withdrawnAt}
		WHERE user_id = #{userId}
		  AND ended_at IS NULL
		""")
	int finishActiveWorkSessions(
		@Param("userId") long userId,
		@Param("withdrawnAt") OffsetDateTime withdrawnAt
	);

	@Update("""
		UPDATE room_invites
		SET invalidated_at = #{withdrawnAt}
		WHERE created_by = #{userId}
		  AND invalidated_at IS NULL
		""")
	int invalidateRoomInvites(
		@Param("userId") long userId,
		@Param("withdrawnAt") OffsetDateTime withdrawnAt
	);

	@Select("""
		SELECT id
		FROM users
		WHERE deleted_at <= #{cutoff}
		  AND personal_data_purged_at IS NULL
		ORDER BY id
		""")
	List<Long> findAccountsReadyForPurge(@Param("cutoff") OffsetDateTime cutoff);

	@Delete("DELETE FROM work_sessions WHERE user_id = #{userId}")
	int deleteWorkSessions(@Param("userId") long userId);

	@Delete("DELETE FROM profiles WHERE user_id = #{userId}")
	int deleteProfile(@Param("userId") long userId);

	@Delete("DELETE FROM room_members WHERE user_id = #{userId}")
	int deleteRoomMembershipHistory(@Param("userId") long userId);

	@Delete("DELETE FROM room_invites WHERE created_by = #{userId}")
	int deleteRoomInvites(@Param("userId") long userId);

	@Delete("DELETE FROM friends WHERE user_id = #{userId} OR friend_user_id = #{userId}")
	int deleteFriendRelationships(@Param("userId") long userId);

	@Delete("DELETE FROM blocks WHERE blocker_user_id = #{userId} OR blocked_user_id = #{userId}")
	int deleteBlockRelationships(@Param("userId") long userId);

	@Update("""
		UPDATE users
		SET name = '退会済みユーザー',
		    email = CONCAT('withdrawn-', id, '@deleted.invalid'),
		    password_hash = CONCAT('{disabled}', id),
		    personal_data_purged_at = #{purgedAt},
		    updated_at = #{purgedAt}
		WHERE id = #{userId}
		  AND deleted_at IS NOT NULL
		  AND personal_data_purged_at IS NULL
		""")
	int anonymizeAccount(
		@Param("userId") long userId,
		@Param("purgedAt") OffsetDateTime purgedAt
	);
}
