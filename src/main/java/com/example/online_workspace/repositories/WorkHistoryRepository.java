package com.example.online_workspace.repositories;

import java.time.Instant;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WorkHistoryRepository {

	record WorkHistoryRow(long roomId, String roomName, Instant joinedAt, Instant leftAt) {
	}

	@Select("""
		SELECT u.id
		FROM users u
		JOIN account_statuses s ON s.id = u.account_status_id
		WHERE u.email = #{email}
		  AND u.deleted_at IS NULL
		  AND s.code = 'ACTIVE'
		  AND (u.suspended_until IS NULL OR u.suspended_until <= CURRENT_TIMESTAMP)
		""")
	Long findActiveUserIdByEmail(@Param("email") String email);

	@Select("""
		SELECT COUNT(*)
		FROM room_members rm
		WHERE rm.user_id = #{userId}
		""")
	long countByUserId(@Param("userId") long userId);

	@Select("""
		SELECT rm.room_id, r.name AS room_name, rm.joined_at, rm.left_at
		FROM room_members rm
		JOIN rooms r ON r.id = rm.room_id
		WHERE rm.user_id = #{userId}
		ORDER BY rm.joined_at DESC, rm.id DESC
		LIMIT #{size} OFFSET #{offset}
		""")
	List<WorkHistoryRow> findByUserId(
		@Param("userId") long userId,
		@Param("size") int size,
		@Param("offset") long offset
	);
}
