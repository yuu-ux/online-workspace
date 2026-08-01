package com.example.online_workspace.repository;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.online_workspace.entity.RoomMemberEntity;

@Mapper
public interface RoomMemberRepository {
    @Insert("""
        INSERT INTO room_members (room_id, user_id)
        VALUES (#{roomId}, #{userId})
        ON CONFLICT (room_id, user_id) WHERE left_at IS NULL DO NOTHING
        """)
    void addMember(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Delete("""
        DELETE FROM room_members
        WHERE room_id = #{roomId} AND user_id = #{userId}
        """)
    int removeMember(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Select("""
        SELECT COUNT(*)
        FROM room_members
        WHERE room_id = #{roomId} AND user_id = #{userId}
        """)
    int countMember(@Param("roomId") Long roomId, @Param("userId") Long userId);

    @Select("""
        SELECT
            rm.room_id,
            rm.user_id,
            u.name AS user_name,
            rm.joined_at
        FROM room_members rm
        INNER JOIN users u ON u.id = rm.user_id
        WHERE rm.room_id = #{roomId}
        ORDER BY rm.joined_at ASC
        """)
    List<RoomMemberEntity> findMembersByRoomId(@Param("roomId") Long roomId);

    @Select("""
        SELECT COUNT(*)
        FROM room_members
        WHERE room_id = #{roomId}
        """)
    int countParticipants(@Param("roomId") Long roomId);
}
