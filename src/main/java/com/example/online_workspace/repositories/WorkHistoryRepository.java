package com.example.online_workspace.repositories;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WorkHistoryRepository {

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
		<script>
		SELECT COUNT(*)
		FROM work_sessions ws
		WHERE ws.user_id = #{userId}
		<if test="fromInclusive != null">
		  AND ws.started_at &gt;= #{fromInclusive}
		</if>
		<if test="toExclusive != null">
		  AND ws.started_at &lt; #{toExclusive}
		</if>
		<if test="categoryId != null">
		  AND ws.category_id = #{categoryId}
		</if>
		</script>
		""")
	long countSessions(
		@Param("userId") long userId,
		@Param("fromInclusive") Instant fromInclusive,
		@Param("toExclusive") Instant toExclusive,
		@Param("categoryId") Long categoryId
	);

	@Select("""
		<script>
		SELECT
		  ws.id AS session_id,
		  r.id AS room_id,
		  r.name AS room_name,
		  rc.id AS category_id,
		  rc.name AS category_name,
		  rc.description AS category_description,
		  rc.sort_order AS category_sort_order,
		  work_style.code AS work_style,
		  r.max_members,
		  (SELECT COUNT(*)
		     FROM room_members active_members
		    WHERE active_members.room_id = r.id
		      AND active_members.left_at IS NULL) AS current_members,
		  visibility.code AS visibility,
		  room_status.code AS room_status,
		  creator.id AS creator_id,
		  creator.name AS creator_name,
		  creator_profile.icon_url AS creator_icon_url,
		  r.created_at AS room_created_at,
		  ws.started_at,
		  ws.ended_at,
		  CAST(GREATEST(0, EXTRACT(EPOCH FROM (COALESCE(ws.ended_at, CURRENT_TIMESTAMP) - ws.started_at))) AS BIGINT) AS duration_seconds,
		  EXISTS (
		    SELECT 1
		    FROM room_members active_member
		    JOIN blocks b
		      ON (b.blocker_user_id = #{userId} AND b.blocked_user_id = active_member.user_id)
		      OR (b.blocked_user_id = #{userId} AND b.blocker_user_id = active_member.user_id)
		    WHERE active_member.room_id = r.id
		      AND active_member.left_at IS NULL
		  ) AS blocked
		FROM work_sessions ws
		JOIN rooms r ON r.id = ws.room_id
		JOIN room_categories rc ON rc.id = ws.category_id
		JOIN work_styles work_style ON work_style.id = r.work_style_id
		JOIN visibilities visibility ON visibility.id = r.visibility_id
		JOIN room_statuses room_status ON room_status.id = r.status_id
		JOIN users creator ON creator.id = r.created_by
		LEFT JOIN profiles creator_profile ON creator_profile.user_id = creator.id
		WHERE ws.user_id = #{userId}
		<if test="fromInclusive != null">
		  AND ws.started_at &gt;= #{fromInclusive}
		</if>
		<if test="toExclusive != null">
		  AND ws.started_at &lt; #{toExclusive}
		</if>
		<if test="categoryId != null">
		  AND ws.category_id = #{categoryId}
		</if>
		ORDER BY ws.started_at DESC, ws.id DESC
		LIMIT #{size} OFFSET #{offset}
		</script>
		""")
	List<SessionRow> findSessions(
		@Param("userId") long userId,
		@Param("fromInclusive") Instant fromInclusive,
		@Param("toExclusive") Instant toExclusive,
		@Param("categoryId") Long categoryId,
		@Param("size") int size,
		@Param("offset") long offset
	);

	@Select("""
		SELECT DISTINCT u.id, u.name, profile.icon_url
		FROM room_members rm
		JOIN users u ON u.id = rm.user_id
		LEFT JOIN profiles profile ON profile.user_id = u.id
		WHERE rm.room_id = #{roomId}
		  AND rm.joined_at < COALESCE(#{endedAt}, CURRENT_TIMESTAMP)
		  AND (rm.left_at IS NULL OR rm.left_at > #{startedAt})
		ORDER BY u.name, u.id
		""")
	List<ParticipantRow> findParticipants(
		@Param("roomId") long roomId,
		@Param("startedAt") Instant startedAt,
		@Param("endedAt") Instant endedAt
	);

	@Select("""
		<script>
		SELECT
		  rc.id AS category_id,
		  rc.name AS category_name,
		  rc.description AS category_description,
		  rc.sort_order AS category_sort_order,
		  CAST(COALESCE(SUM(GREATEST(0, EXTRACT(EPOCH FROM (COALESCE(ws.ended_at, CURRENT_TIMESTAMP) - ws.started_at)))), 0) AS BIGINT) AS duration_seconds
		FROM work_sessions ws
		JOIN room_categories rc ON rc.id = ws.category_id
		WHERE ws.user_id = #{userId}
		<if test="fromInclusive != null">
		  AND ws.started_at &gt;= #{fromInclusive}
		</if>
		<if test="toExclusive != null">
		  AND ws.started_at &lt; #{toExclusive}
		</if>
		GROUP BY rc.id, rc.name, rc.description, rc.sort_order
		ORDER BY duration_seconds DESC, rc.sort_order, rc.id
		</script>
		""")
	List<CategoryDurationRow> summarizeByCategory(
		@Param("userId") long userId,
		@Param("fromInclusive") Instant fromInclusive,
		@Param("toExclusive") Instant toExclusive
	);

	@Select("""
		<script>
		SELECT
		  CAST(ws.started_at AT TIME ZONE 'UTC' AS DATE) AS work_date,
		  CAST(COALESCE(SUM(GREATEST(0, EXTRACT(EPOCH FROM (COALESCE(ws.ended_at, CURRENT_TIMESTAMP) - ws.started_at)))), 0) AS BIGINT) AS duration_seconds
		FROM work_sessions ws
		WHERE ws.user_id = #{userId}
		<if test="fromInclusive != null">
		  AND ws.started_at &gt;= #{fromInclusive}
		</if>
		<if test="toExclusive != null">
		  AND ws.started_at &lt; #{toExclusive}
		</if>
		GROUP BY CAST(ws.started_at AT TIME ZONE 'UTC' AS DATE)
		ORDER BY work_date
		</script>
		""")
	List<DailyDurationRow> summarizeByDate(
		@Param("userId") long userId,
		@Param("fromInclusive") Instant fromInclusive,
		@Param("toExclusive") Instant toExclusive
	);

	record SessionRow(
		long sessionId,
		long roomId,
		String roomName,
		long categoryId,
		String categoryName,
		String categoryDescription,
		int categorySortOrder,
		String workStyle,
		int maxMembers,
		int currentMembers,
		String visibility,
		String roomStatus,
		long creatorId,
		String creatorName,
		String creatorIconUrl,
		Instant roomCreatedAt,
		Instant startedAt,
		Instant endedAt,
		long durationSeconds,
		boolean blocked
	) {
	}

	record ParticipantRow(long id, String name, String iconUrl) {
	}

	record CategoryDurationRow(
		long categoryId,
		String categoryName,
		String categoryDescription,
		int categorySortOrder,
		long durationSeconds
	) {
	}

	record DailyDurationRow(LocalDate workDate, long durationSeconds) {
	}
}
