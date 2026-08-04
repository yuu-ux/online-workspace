package com.example.online_workspace.repositories;

import java.time.OffsetDateTime;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DataRetentionRepository {

	@Delete("DELETE FROM messages WHERE sent_at < #{cutoff}")
	int deleteMessagesSentBefore(@Param("cutoff") OffsetDateTime cutoff);

	@Delete("""
		DELETE FROM work_sessions
		WHERE user_id IN (
			SELECT id
			FROM users
			WHERE deleted_at IS NOT NULL
			  AND deleted_at < #{cutoff}
		)
		""")
	int deleteWorkSessionsForUsersWithdrawnBefore(@Param("cutoff") OffsetDateTime cutoff);
}
