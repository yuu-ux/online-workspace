package com.example.online_workspace.repositories;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.online_workspace.models.RoomListItem;

@Mapper
public interface RoomListRepository {

	@Select("""
		SELECT id FROM users
		WHERE email = #{email} AND deleted_at IS NULL
		""")
	Long findActiveUserIdByEmail(@Param("email") String email);

	@Select("""
		<script>
		SELECT COUNT(*)
		FROM rooms r
		JOIN visibilities v ON v.id = r.visibility_id AND v.code = 'PUBLIC'
		JOIN room_statuses rs ON rs.id = r.status_id AND rs.code = 'OPEN'
		WHERE 1 = 1
		<if test="categoryId != null">
			AND r.category_id = #{categoryId}
		</if>
		<if test="workStyle != null">
			AND r.work_style_id = (SELECT id FROM work_styles WHERE code = #{workStyle})
		</if>
		</script>
		""")
	long countPublicRooms(
		@Param("categoryId") Long categoryId,
		@Param("workStyle") String workStyle
	);

	@Select("""
		<script>
		WITH filtered_rooms AS (
			SELECT r.*
			FROM rooms r
			JOIN visibilities v ON v.id = r.visibility_id AND v.code = 'PUBLIC'
			JOIN room_statuses rs ON rs.id = r.status_id AND rs.code = 'OPEN'
			WHERE 1 = 1
			<if test="categoryId != null">
				AND r.category_id = #{categoryId}
			</if>
			<if test="workStyle != null">
				AND r.work_style_id = (SELECT id FROM work_styles WHERE code = #{workStyle})
			</if>
			ORDER BY r.created_at DESC, r.id DESC
			LIMIT #{size} OFFSET #{offset}
		)
		SELECT r.id, r.name,
		       c.id AS category_id, c.name AS category_name,
		       c.description AS category_description, c.sort_order AS category_sort_order,
		       ws.code AS work_style, r.max_members,
		       (SELECT COUNT(*) FROM room_members rm
		        WHERE rm.room_id = r.id AND rm.left_at IS NULL) AS current_members,
		       v.code AS visibility, rs.code AS status,
		       u.id AS creator_id, u.name AS creator_name, p.icon_url AS creator_icon_url,
		       EXISTS (
		           SELECT 1
		           FROM room_members rm
		           JOIN blocks b
		             ON (b.blocker_user_id = #{userId} AND b.blocked_user_id = rm.user_id)
		             OR (b.blocked_user_id = #{userId} AND b.blocker_user_id = rm.user_id)
		           WHERE rm.room_id = r.id AND rm.left_at IS NULL
		       ) AS blocked,
		       r.created_at
		FROM filtered_rooms r
		JOIN room_categories c ON c.id = r.category_id
		JOIN work_styles ws ON ws.id = r.work_style_id
		JOIN visibilities v ON v.id = r.visibility_id
		JOIN room_statuses rs ON rs.id = r.status_id
		JOIN users u ON u.id = r.created_by
		LEFT JOIN profiles p ON p.user_id = u.id
		ORDER BY r.created_at DESC, r.id DESC
		</script>
		""")
	List<RoomListItem> findPublicRooms(
		@Param("userId") long userId,
		@Param("categoryId") Long categoryId,
		@Param("workStyle") String workStyle,
		@Param("size") int size,
		@Param("offset") long offset
	);
}
