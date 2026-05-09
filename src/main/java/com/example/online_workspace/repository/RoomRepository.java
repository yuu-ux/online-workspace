package com.example.online_workspace.repository;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.online_workspace.dto.RoomSummary;
import com.example.online_workspace.entity.RoomEntity;

@Mapper
public interface RoomRepository {
    @Insert("""
        INSERT INTO rooms (name, created_by)
        VALUES (#{name}, #{createdBy})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RoomEntity room);

    @Select("""
        SELECT id, name, created_by, created_at, updated_at
        FROM rooms
        WHERE id = #{id}
        """)
    Optional<RoomEntity> findById(@Param("id") Long id);

    @Select("""
        SELECT
            r.id,
            r.name,
            COUNT(rm.user_id)::int AS participant_count,
            r.created_at
        FROM rooms r
        LEFT JOIN room_members rm ON rm.room_id = r.id
        GROUP BY r.id
        ORDER BY r.created_at DESC
        """)
    List<RoomSummary> findAllSummaries();
}
