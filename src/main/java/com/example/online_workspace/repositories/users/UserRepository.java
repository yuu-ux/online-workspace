package com.example.online_workspace.repositories.users;

import com.example.online_workspace.models.users.UserAccount;
import com.example.online_workspace.models.users.AuthenticatedUser;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * usersテーブルへのアクセスを提供するMyBatis Mapper。
 */
@Mapper
public interface UserRepository {

	/**
	 * 指定したメールアドレスの登録有無を確認する。
	 *
	 * @param email 検索するメールアドレス
	 * @return 登録済みの場合はtrue
	 */
	@Select("SELECT EXISTS (SELECT 1 FROM users WHERE email = #{email})")
	boolean existsByEmail(@Param("email") String email);

	@Select("""
		SELECT users.id,
		       users.name,
		       users.email,
		       account_statuses.code AS account_status,
		       users.suspended_until
		FROM users
		INNER JOIN account_statuses ON account_statuses.id = users.account_status_id
		WHERE users.email = #{email}
		  AND users.deleted_at IS NULL
		""")
	Optional<AuthenticatedUser> findAuthenticatedByEmail(@Param("email") String email);

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
