package com.example.online_workspace.repositories;

import java.time.Instant;
import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ChatMessageRepository {

	@Select("SELECT EXISTS (SELECT 1 FROM rooms WHERE id = #{roomId})")
	boolean roomExists(@Param("roomId") long roomId);

	@Select("""
		SELECT EXISTS (
			SELECT 1 FROM room_members
			WHERE room_id = #{roomId}
			  AND user_id = #{userId}
			  AND left_at IS NULL
		)
		""")
	boolean isActiveMember(@Param("roomId") long roomId, @Param("userId") long userId);

	@Insert("""
		INSERT INTO messages (room_id, user_id, content)
		VALUES (#{roomId}, #{userId}, #{content})
		""")
	@Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
	int insert(Draft message);

	@Select("""
		SELECT m.id, m.room_id,
		       u.id AS sender_id, u.name AS sender_name, p.icon_url AS sender_icon_url,
		       m.content, m.sent_at
		FROM messages m
		JOIN users u ON u.id = m.user_id
		LEFT JOIN profiles p ON p.user_id = u.id
		WHERE m.id = #{messageId}
		""")
	Row findById(@Param("messageId") long messageId);

	@Select("SELECT COUNT(*) FROM messages WHERE room_id = #{roomId}")
	long countByRoomId(@Param("roomId") long roomId);

	@Select("""
		SELECT m.id, m.room_id,
		       u.id AS sender_id, u.name AS sender_name, p.icon_url AS sender_icon_url,
		       m.content, m.sent_at
		FROM messages m
		JOIN users u ON u.id = m.user_id
		LEFT JOIN profiles p ON p.user_id = u.id
		WHERE m.room_id = #{roomId}
		ORDER BY m.sent_at DESC, m.id DESC
		LIMIT #{size} OFFSET #{offset}
		""")
	List<Row> findByRoomId(
		@Param("roomId") long roomId,
		@Param("size") int size,
		@Param("offset") long offset
	);

	@Select("""
		SELECT u.email
		FROM room_members rm
		JOIN users u ON u.id = rm.user_id
		JOIN account_statuses s ON s.id = u.account_status_id AND s.code = 'ACTIVE'
		WHERE rm.room_id = #{roomId}
		  AND rm.left_at IS NULL
		  AND u.deleted_at IS NULL
		  AND (u.suspended_until IS NULL OR u.suspended_until <= CURRENT_TIMESTAMP)
		""")
	List<String> findActiveMemberEmails(@Param("roomId") long roomId);

	record Row(
		long id,
		long roomId,
		long senderId,
		String senderName,
		String senderIconUrl,
		String content,
		Instant sentAt
	) {
	}

	class Draft {
		private Long id;
		private final long roomId;
		private final long userId;
		private final String content;

		public Draft(long roomId, long userId, String content) {
			this.roomId = roomId;
			this.userId = userId;
			this.content = content;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public long getRoomId() {
			return roomId;
		}

		public long getUserId() {
			return userId;
		}

		public String getContent() {
			return content;
		}
	}
}
