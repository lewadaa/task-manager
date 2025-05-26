package com.example.taskmanager.dto;

import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class TaskResponseDto {
    String title;

    String description;

    TaskStatus status;

    UserResponseDto user;
}
