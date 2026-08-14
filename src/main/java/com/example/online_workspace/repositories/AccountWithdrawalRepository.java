package com.example.online_workspace.repositories;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.example.online_workspace.models.WithdrawalAccount;
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
}
