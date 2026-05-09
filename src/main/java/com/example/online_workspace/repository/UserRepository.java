package com.example.online_workspace.repository;

import java.util.Optional;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.example.online_workspace.entity.UserEntity;

@Mapper
public interface UserRepository {
    @Insert("""
        INSERT INTO users (name, email, password_hash)
        VALUES (#{name}, #{email}, #{passwordHash})
        """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(UserEntity user);

    @Select("""
        SELECT id, name, email, password_hash, created_at, updated_at
        FROM users
        WHERE email = #{email}
        """)
    Optional<UserEntity> findByEmail(@Param("email") String email);

    @Select("""
        SELECT id, name, email, password_hash, created_at, updated_at
        FROM users
        WHERE id = #{id}
        """)
    Optional<UserEntity> findById(@Param("id") Long id);
}
