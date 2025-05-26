package com.example.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Value;

@Value
public class AuthenticationRequest {

    @NotBlank
    String username;

    @NotBlank
    String password;
}
