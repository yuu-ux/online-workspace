package com.example.online_workspace.repositories;

import java.time.Instant;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.example.online_workspace.models.RoomDraft;

@Mapper
public interface RoomRepository {

	@Select("SELECT id FROM users WHERE email = #{email} AND deleted_at IS NULL")
	Long findActiveUserIdByEmail(@Param("email") String email);

	@Select("""
		SELECT EXISTS (
			SELECT 1
			FROM room_categories rc
			JOIN room_category_statuses rcs ON rcs.id = rc.status_id
			WHERE rc.id = #{categoryId} AND rcs.code = 'ACTIVE'
		)
		""")
	boolean isActiveCategory(@Param("categoryId") long categoryId);

	@Insert("""
		INSERT INTO rooms (
			name, description, created_by, category_id,
			work_style_id, max_members
		)
		VALUES (
			#{name}, #{description}, #{createdBy}, #{categoryId},
			(SELECT id FROM work_styles WHERE code = #{workStyle}),
			#{maxMembers}
		)
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
		       rs.code AS status,
		       u.id AS creator_id, u.name AS creator_name, p.icon_url AS creator_icon_url,
		       (SELECT COUNT(*) FROM room_members rm WHERE rm.room_id = r.id AND rm.left_at IS NULL) AS current_members,
		       EXISTS (
		       	SELECT 1 FROM room_members self_rm
		       	WHERE self_rm.room_id = r.id AND self_rm.user_id = #{viewerId} AND self_rm.left_at IS NULL
		       ) AS member,
		       r.created_at, r.updated_at
		FROM rooms r
		JOIN room_categories rc ON rc.id = r.category_id
		JOIN work_styles ws ON ws.id = r.work_style_id
		JOIN room_statuses rs ON rs.id = r.status_id
		JOIN users u ON u.id = r.created_by
		LEFT JOIN profiles p ON p.user_id = u.id
		WHERE r.id = #{roomId}
		""")
	RoomView findById(@Param("roomId") long roomId, @Param("viewerId") long viewerId);

	@Select("""
		SELECT r.id, r.created_by AS creator_id, rs.code AS status,
		       (SELECT COUNT(*) FROM room_members rm WHERE rm.room_id = r.id AND rm.left_at IS NULL) AS current_members
		FROM rooms r
		JOIN room_statuses rs ON rs.id = r.status_id
		WHERE r.id = #{roomId}
		""")
	RoomState findRoomState(@Param("roomId") long roomId);

	@Select("""
		SELECT EXISTS (
			SELECT 1
			FROM room_categories rc
			JOIN room_category_statuses rcs ON rcs.id = rc.status_id
			WHERE rc.id = #{categoryId} AND rcs.code = 'ACTIVE'
		)
		""")
	boolean existsActiveCategory(@Param("categoryId") long categoryId);

	@Update("""
		UPDATE rooms
		SET name = #{name},
		    description = #{description},
		    category_id = #{categoryId},
		    work_style_id = (SELECT id FROM work_styles WHERE code = #{workStyle}),
		    max_members = #{maxMembers},
		    updated_at = CURRENT_TIMESTAMP
		WHERE id = #{roomId}
		""")
	int updateRoom(
		@Param("roomId") long roomId,
		@Param("name") String name,
		@Param("description") String description,
		@Param("categoryId") long categoryId,
		@Param("workStyle") String workStyle,
		@Param("maxMembers") int maxMembers
	);

	@Update("""
		UPDATE rooms
		SET status_id = (SELECT id FROM room_statuses WHERE code = 'CLOSED'),
		    closed_at = CURRENT_TIMESTAMP,
		    updated_at = CURRENT_TIMESTAMP
		WHERE id = #{roomId}
		""")
	int closeRoom(@Param("roomId") long roomId);

	record RoomView(
		long id,
		String name,
		String description,
		long categoryId,
		String categoryName,
		String categoryDescription,
		int categorySortOrder,
		String workStyle,
		short maxMembers,
		String status,
		long creatorId,
		String creatorName,
		String creatorIconUrl,
		int currentMembers,
		boolean member,
		Instant createdAt,
		Instant updatedAt
	) {
	}

	record RoomState(long id, long creatorId, String status, int currentMembers) {
	}
}
