package com.example.online_workspace.repository;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.online_workspace.entity.MessageEntity;

@Mapper
public interface MessageRepository {
    @Insert("""
        INSERT INTO messages (room_id, user_id, content)
        VALUES (#{roomId}, #{userId}, #{content})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(MessageEntity message);

    @Select("""
        SELECT
            m.id,
            m.room_id,
            m.user_id,
            u.name AS user_name,
            m.content,
            m.sent_at
        FROM messages m
        INNER JOIN users u ON u.id = m.user_id
        WHERE m.room_id = #{roomId}
        ORDER BY m.sent_at ASC
        LIMIT 200
        """)
    List<MessageEntity> findByRoomId(@Param("roomId") Long roomId);
}
