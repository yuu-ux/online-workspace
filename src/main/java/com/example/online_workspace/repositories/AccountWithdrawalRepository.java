package com.example.online_workspace.repositories;

import java.util.Optional;

import com.example.online_workspace.models.WithdrawalAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AccountWithdrawalRepository {

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
}
