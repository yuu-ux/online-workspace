package com.example.online_workspace.repositories.users;

import com.example.online_workspace.models.users.UserAccount;
import java.time.Instant;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * usersテーブルへのアクセスを提供するMyBatis Mapper。
 */
@Mapper
public interface UserRepository {

	record MyProfileRow(
		long id,
		String name,
		String iconUrl,
		boolean isPublic,
		String bio,
		Long categoryId,
		String categoryName,
		String categoryDescription,
		Integer categorySortOrder,
		String email,
		String role,
		String accountStatus,
		Instant createdAt
	) {
	}

	/**
	 * 指定したメールアドレスの登録有無を確認する。
	 *
	 * @param email 検索するメールアドレス
	 * @return 登録済みの場合はtrue
	 */
	@Select("SELECT EXISTS (SELECT 1 FROM users WHERE email = #{email})")
	boolean existsByEmail(@Param("email") String email);

	/**
	 * ユーザーの認証情報を登録する。
	 *
	 * @param user 登録する認証情報
	 * @return 登録した行数
	 */
	@Insert("""
		INSERT INTO users (name, email, password_hash)
		VALUES (#{name}, #{email}, #{passwordHash})
		""")
	int insert(UserAccount user);

	@Select("""
		SELECT
			u.id,
			u.name,
			p.icon_url,
			COALESCE(p.is_public, TRUE) AS is_public,
			COALESCE(p.bio, '') AS bio,
			c.id AS category_id,
			c.name AS category_name,
			c.description AS category_description,
			c.sort_order AS category_sort_order,
			u.email,
			r.code AS role,
			s.code AS account_status,
			u.created_at
		FROM users u
		JOIN roles r ON r.id = u.role_id
		JOIN account_statuses s ON s.id = u.account_status_id
		LEFT JOIN profiles p ON p.user_id = u.id
		LEFT JOIN room_categories c ON c.id = p.work_category_id
		WHERE u.email = #{email}
		  AND u.deleted_at IS NULL
		  AND s.code = 'ACTIVE'
		  AND (u.suspended_until IS NULL OR u.suspended_until <= CURRENT_TIMESTAMP)
		""")
	MyProfileRow findMyProfileByEmail(@Param("email") String email);

	/**
	 * 指定したユーザー行をロックし、存在を確認する。
	 *
	 * @param userId ロックするユーザーID
	 * @return ユーザーが存在する場合はtrue
	 */
	@Select("""
		SELECT EXISTS (
			SELECT 1
			FROM users
			WHERE id = #{userId}
			FOR UPDATE
		)
		""")
	boolean lockById(@Param("userId") long userId);

}
