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
	Optional<WithdrawalAccount> findWithdrawableByEmail(@Param("email") String email);

	@Select("""
		SELECT users.id, users.password_hash
		FROM users
		INNER JOIN account_statuses
			ON account_statuses.id = users.account_status_id
		WHERE users.email = #{email}
		  AND users.deleted_at IS NULL
		  AND account_statuses.code = 'ACTIVE'
		  AND (users.suspended_until IS NULL OR users.suspended_until <= CURRENT_TIMESTAMP)
		""")
	Optional<WithdrawalAccount> findActiveByEmail(@Param("email") String email);

	@Select("SELECT CURRENT_TIMESTAMP")
	OffsetDateTime currentTimestamp();

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

}
