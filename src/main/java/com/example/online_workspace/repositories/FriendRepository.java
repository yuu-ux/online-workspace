package com.example.online_workspace.repositories;

import java.time.Instant;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface FriendRepository {

	@Select("""
		SELECT u.id AS friend_id,
		       u.name AS friend_name,
		       u.email AS friend_email,
		       p.icon_url AS friend_icon_url,
		       f.created_at
		FROM friends f
		JOIN users u ON u.id = f.friend_user_id
		JOIN account_statuses s ON s.id = u.account_status_id
		JOIN friend_statuses fs ON fs.id = f.status_id AND fs.code = 'ACTIVE'
		LEFT JOIN profiles p ON p.user_id = u.id
		WHERE f.user_id = #{userId}
		  AND u.deleted_at IS NULL
		  AND s.code = 'ACTIVE'
		  AND (u.suspended_until IS NULL OR u.suspended_until <= CURRENT_TIMESTAMP)
		ORDER BY f.created_at DESC, f.id DESC
		LIMIT #{size} OFFSET #{offset}
		""")
	List<FriendRow> findActiveFriends(
		@Param("userId") long userId,
		@Param("size") int size,
		@Param("offset") long offset
	);

	@Select("""
		SELECT COUNT(*)
		FROM friends f
		JOIN users u ON u.id = f.friend_user_id
		JOIN account_statuses s ON s.id = u.account_status_id
		JOIN friend_statuses fs ON fs.id = f.status_id AND fs.code = 'ACTIVE'
		WHERE f.user_id = #{userId}
		  AND u.deleted_at IS NULL
		  AND s.code = 'ACTIVE'
		  AND (u.suspended_until IS NULL OR u.suspended_until <= CURRENT_TIMESTAMP)
		""")
	long countActiveFriends(@Param("userId") long userId);

	@Select("""
		SELECT source.id AS source_user_id,
		       recipient.email AS recipient_email
		FROM users source
		JOIN account_statuses source_status ON source_status.id = source.account_status_id
		JOIN friends f ON f.friend_user_id = source.id
		JOIN friend_statuses fs ON fs.id = f.status_id AND fs.code = 'ACTIVE'
		JOIN users recipient ON recipient.id = f.user_id
		JOIN account_statuses recipient_status ON recipient_status.id = recipient.account_status_id
		WHERE source.email = #{email}
		  AND source.deleted_at IS NULL
		  AND source_status.code = 'ACTIVE'
		  AND recipient.deleted_at IS NULL
		  AND recipient_status.code = 'ACTIVE'
		  AND (recipient.suspended_until IS NULL OR recipient.suspended_until <= CURRENT_TIMESTAMP)
		""")
	List<FriendPresenceRecipient> findActiveFriendPresenceRecipients(@Param("email") String email);

	@Select("""
		SELECT u.id, u.name, u.email, p.icon_url
		FROM users u
		JOIN account_statuses s ON s.id = u.account_status_id
		LEFT JOIN profiles p ON p.user_id = u.id
		WHERE u.id = #{friendUserId}
		  AND u.deleted_at IS NULL
		  AND s.code = 'ACTIVE'
		  AND (u.suspended_until IS NULL OR u.suspended_until <= CURRENT_TIMESTAMP)
		""")
	FriendTarget findActiveTargetById(@Param("friendUserId") long friendUserId);

	@Select("""
		SELECT EXISTS (
			SELECT 1
			FROM friends f
			JOIN friend_statuses fs ON fs.id = f.status_id AND fs.code = 'ACTIVE'
			WHERE f.user_id = #{userId}
			  AND f.friend_user_id = #{friendUserId}
		)
		""")
	boolean existsActiveFriend(
		@Param("userId") long userId,
		@Param("friendUserId") long friendUserId
	);

	@Insert("""
		INSERT INTO friends (user_id, friend_user_id, status_id)
		VALUES (
			#{userId},
			#{friendUserId},
			(SELECT id FROM friend_statuses WHERE code = 'ACTIVE')
		)
		""")
	int insertFriend(
		@Param("userId") long userId,
		@Param("friendUserId") long friendUserId
	);

	@Select("""
		SELECT id
		FROM friends
		WHERE user_id = #{userId}
		  AND friend_user_id = #{friendUserId}
		""")
	Long findFriendId(
		@Param("userId") long userId,
		@Param("friendUserId") long friendUserId
	);

	@Update("""
		UPDATE friends
		SET status_id = (SELECT id FROM friend_statuses WHERE code = 'ACTIVE'),
		    updated_at = CURRENT_TIMESTAMP
		WHERE id = #{friendId}
		""")
	int reactivateFriend(@Param("friendId") long friendId);

	@Update("""
		UPDATE friends
		SET status_id = (SELECT id FROM friend_statuses WHERE code = 'REMOVED'),
		    updated_at = CURRENT_TIMESTAMP
		WHERE user_id = #{userId}
		  AND friend_user_id = #{friendUserId}
		  AND status_id = (SELECT id FROM friend_statuses WHERE code = 'ACTIVE')
		""")
	int removeFriend(
		@Param("userId") long userId,
		@Param("friendUserId") long friendUserId
	);

	@Select("""
		SELECT u.id AS friend_id,
		       u.name AS friend_name,
		       u.email AS friend_email,
		       p.icon_url AS friend_icon_url,
		       f.created_at
		FROM friends f
		JOIN users u ON u.id = f.friend_user_id
		JOIN account_statuses s ON s.id = u.account_status_id
		JOIN friend_statuses fs ON fs.id = f.status_id AND fs.code = 'ACTIVE'
		LEFT JOIN profiles p ON p.user_id = u.id
		WHERE f.user_id = #{userId}
		  AND f.friend_user_id = #{friendUserId}
		  AND u.deleted_at IS NULL
		  AND s.code = 'ACTIVE'
		  AND (u.suspended_until IS NULL OR u.suspended_until <= CURRENT_TIMESTAMP)
		""")
	FriendRow findActiveFriend(
		@Param("userId") long userId,
		@Param("friendUserId") long friendUserId
	);

	record FriendRow(
		long friendId,
		String friendName,
		String friendEmail,
		String friendIconUrl,
		Instant createdAt
	) {
	}

	record FriendTarget(long id, String name, String email, String iconUrl) {
	}

	record FriendPresenceRecipient(long sourceUserId, String recipientEmail) {
	}
}
