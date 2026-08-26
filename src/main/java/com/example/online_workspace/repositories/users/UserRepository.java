package com.example.online_workspace.repositories.users;

import com.example.online_workspace.models.users.UserAccount;
import com.example.online_workspace.models.users.UserAuthentication;
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

	/**
	 * ログインに必要なユーザー情報を取得する。
	 *
	 * @param email 正規化済みのメールアドレス
	 * @return ユーザー情報。未登録の場合はnull
	 */
	@Select("""
		SELECT u.id,
		       u.name,
		       u.email,
		       u.password_hash,
		       s.code AS account_status,
		       u.suspended_until
		FROM users u
		JOIN account_statuses s ON s.id = u.account_status_id
		WHERE u.email = #{email}
		  AND u.deleted_at IS NULL
		""")
	UserAuthentication findByEmail(@Param("email") String email);

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
