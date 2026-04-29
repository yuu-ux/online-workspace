package com.example.online_workspace.entity;

import java.security.Timestamp;

import lombok.Data;
/**
 * UserEntity
 */
@Data
public class UserEntity {
    private Integer id;
    private String name;
    private String email;
    private String password;
    private String passwordConfirmation;
    private Timestamp createdAt;
    private Timestamp modifiedAt;
}
