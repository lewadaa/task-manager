package com.example.taskmanager.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate stringRedisTemplate;
    private static final String REFRESH_PREFIX = "refresh:";

    public void storeRefreshToken(String username, String refreshToken, long ttlMillis) {
        stringRedisTemplate.opsForValue().set(
                REFRESH_PREFIX + username,
                refreshToken,
                Duration.ofMillis(ttlMillis));
    }

    public String getRefreshToken(String username) {
        return stringRedisTemplate.opsForValue().get(REFRESH_PREFIX + username);
    }

    public void deleteRefreshToken(String username) {
        stringRedisTemplate.delete(REFRESH_PREFIX + username);
    }
}
