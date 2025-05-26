package com.example.taskmanager.dto;

import com.example.taskmanager.entity.RoleType;
import com.example.taskmanager.entity.User;
import jakarta.validation.constraints.NotBlank;
import lombok.Value;

import java.util.Set;

@Value
public class UserResponseDto {
    String username;

    String role;
}
