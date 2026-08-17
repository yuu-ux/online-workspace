package com.example.online_workspace.repositories;

import java.time.Instant;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.online_workspace.models.RoomDraft;

@Mapper
public interface RoomRepository {

	@Select("""
		SELECT u.id FROM users u
		JOIN account_statuses s ON s.id = u.account_status_id
		WHERE u.email = #{email} AND u.deleted_at IS NULL
		  AND s.code = 'ACTIVE'
		  AND (u.suspended_until IS NULL OR u.suspended_until <= CURRENT_TIMESTAMP)
		""")
	Long findActiveUserIdByEmail(@Param("email") String email);

	@Insert("""
		INSERT INTO rooms (
			name, description, created_by, category_id,
			work_style_id, max_members, visibility_id
		)
		SELECT
			#{name}, #{description}, #{createdBy}, #{categoryId},
			(SELECT id FROM work_styles WHERE code = #{workStyle}),
			#{maxMembers},
			(SELECT id FROM visibilities WHERE code = #{visibility})
		FROM room_categories rc
		JOIN room_category_statuses rcs ON rcs.id = rc.status_id
		WHERE rc.id = #{categoryId}
		  AND rcs.code = 'ACTIVE'
		""")
	@Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
	int insert(RoomDraft room);

	@Insert("INSERT INTO room_members (room_id, user_id) VALUES (#{roomId}, #{userId})")
	int insertCreatorMembership(@Param("roomId") long roomId, @Param("userId") long userId);

	@Select("""
		SELECT r.id, r.name, r.description,
		       rc.id AS category_id, rc.name AS category_name,
		       rc.description AS category_description, rc.sort_order AS category_sort_order,
		       ws.code AS work_style, r.max_members,
		       v.code AS visibility, rs.code AS status,
		       u.id AS creator_id, u.name AS creator_name, p.icon_url AS creator_icon_url,
		       r.created_at, r.updated_at
		FROM rooms r
		JOIN room_categories rc ON rc.id = r.category_id
		JOIN work_styles ws ON ws.id = r.work_style_id
		JOIN visibilities v ON v.id = r.visibility_id
		JOIN room_statuses rs ON rs.id = r.status_id
		JOIN users u ON u.id = r.created_by
		LEFT JOIN profiles p ON p.user_id = u.id
		WHERE r.id = #{roomId}
		""")
	RoomView findById(@Param("roomId") long roomId);

	record RoomView(
		long id,
		String name,
		String description,
		long categoryId,
		String categoryName,
		String categoryDescription,
		int categorySortOrder,
		String workStyle,
		int maxMembers,
		String visibility,
		String status,
		long creatorId,
		String creatorName,
		String creatorIconUrl,
		Instant createdAt,
		Instant updatedAt
	) {
	}
}
