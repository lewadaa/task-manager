package com.example.taskmanager.dto;

import lombok.Value;

import java.util.Date;

@Value
public class AuthenticationResponse {
    String accessToken;

    String refreshToken;
}
