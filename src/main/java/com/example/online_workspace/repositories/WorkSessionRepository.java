package com.example.online_workspace.repositories;

import java.time.Instant;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.online_workspace.models.WorkSession;

@Mapper
public interface WorkSessionRepository {

	@Select("""
		SELECT id
		FROM users
		WHERE id = #{userId}
		FOR UPDATE
		""")
	Long lockUserById(@Param("userId") long userId);

	@Select("""
		SELECT id, user_id, room_id, category_id, started_at, ended_at, created_at, updated_at
		FROM work_sessions
		WHERE user_id = #{userId}
		  AND ended_at IS NULL
		FOR UPDATE
		""")
	WorkSession findActiveByUserIdForUpdate(@Param("userId") long userId);

	@Insert("""
		INSERT INTO work_sessions (user_id, room_id, category_id, started_at)
		SELECT #{userId}, id, category_id, #{startedAt}
		FROM rooms
		WHERE id = #{roomId}
		""")
	int insertFromRoom(
		@Param("userId") long userId,
		@Param("roomId") long roomId,
		@Param("startedAt") Instant startedAt
	);

	@Update("""
		UPDATE work_sessions
		SET ended_at = #{endedAt},
		    updated_at = CURRENT_TIMESTAMP
		WHERE id = #{sessionId}
		  AND ended_at IS NULL
		""")
	int endById(@Param("sessionId") long sessionId, @Param("endedAt") Instant endedAt);
}
