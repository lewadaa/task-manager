package com.example.taskmanager.dto;

import com.example.taskmanager.entity.TaskStatus;
import com.example.taskmanager.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class TaskRequestDto {

    @NotBlank(message = "Title cannot be empty")
    String title;

    @NotBlank(message = "Description cannot be empty")
    String description;

    @NotNull(message = "Status cannot be empty")
    TaskStatus status;

    @NotNull
    Long userId;
}
