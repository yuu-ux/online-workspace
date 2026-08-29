package com.example.online_workspace.repositories;

import java.time.Instant;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProfileRepository {

	@Select("""
		SELECT u.id AS user_id, u.name, p.icon_url,
		       COALESCE(p.is_public, TRUE) AS is_public,
		       COALESCE(p.bio, '') AS bio,
		       c.id AS work_category_id, c.name AS work_category_name,
		       c.description AS work_category_description, c.sort_order AS work_category_sort_order,
		       u.email, s.code AS account_status, u.created_at
		FROM users u
		JOIN account_statuses s ON s.id = u.account_status_id
		LEFT JOIN profiles p ON p.user_id = u.id
		LEFT JOIN room_categories c ON c.id = p.work_category_id
		WHERE u.email = #{email}
		  AND u.deleted_at IS NULL
		  AND s.code = 'ACTIVE'
		  AND (u.suspended_until IS NULL OR u.suspended_until <= CURRENT_TIMESTAMP)
		""")
	Optional<ProfileRow> findByEmail(@Param("email") String email);

	@Select("""
		SELECT u.id
		FROM users u
		JOIN account_statuses s ON s.id = u.account_status_id
		WHERE u.email = #{email}
		  AND u.deleted_at IS NULL
		  AND s.code = 'ACTIVE'
		  AND (u.suspended_until IS NULL OR u.suspended_until <= CURRENT_TIMESTAMP)
		FOR UPDATE OF u
		""")
	Long lockActiveUserIdByEmail(@Param("email") String email);

	@Select("""
		SELECT EXISTS (
			SELECT 1
			FROM room_categories c
			JOIN room_category_statuses s ON s.id = c.status_id
			WHERE c.id = #{categoryId} AND s.code = 'ACTIVE'
		)
		""")
	boolean activeCategoryExists(@Param("categoryId") long categoryId);

	@Update("""
		UPDATE users
		SET name = #{name}, updated_at = CURRENT_TIMESTAMP
		WHERE id = #{userId}
		""")
	int updateName(@Param("userId") long userId, @Param("name") String name);

	@Update("""
		MERGE INTO profiles AS target
		USING (VALUES (#{userId}, #{iconUrl}, #{bio}, #{workCategoryId}, #{isPublic}))
			AS source(user_id, icon_url, bio, work_category_id, is_public)
		ON target.user_id = source.user_id
		WHEN MATCHED THEN UPDATE SET
			icon_url = source.icon_url,
			bio = source.bio,
			work_category_id = source.work_category_id,
			is_public = source.is_public,
			updated_at = CURRENT_TIMESTAMP
		WHEN NOT MATCHED THEN INSERT (user_id, icon_url, bio, work_category_id, is_public)
			VALUES (source.user_id, source.icon_url, source.bio, source.work_category_id, source.is_public)
		""")
	int upsertProfile(
		@Param("userId") long userId,
		@Param("iconUrl") String iconUrl,
		@Param("bio") String bio,
		@Param("workCategoryId") Long workCategoryId,
		@Param("isPublic") boolean isPublic
	);

	record ProfileRow(
		long userId,
		String name,
		String iconUrl,
		boolean isPublic,
		String bio,
		Long workCategoryId,
		String workCategoryName,
		String workCategoryDescription,
		Integer workCategorySortOrder,
		String email,
		String accountStatus,
		Instant createdAt
	) {
	}
}
