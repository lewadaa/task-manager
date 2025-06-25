package com.example.taskmanager.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);

    private final StringRedisTemplate stringRedisTemplate;
    private static final String REFRESH_PREFIX = "refresh:";

    public void storeRefreshToken(String username, String refreshToken, long ttlMillis) {
        logger.debug("Сохранение токена в кэш для пользователя={}", username);

        stringRedisTemplate.opsForValue().set(
                REFRESH_PREFIX + username,
                refreshToken,
                Duration.ofMillis(ttlMillis));
    }

    public String getRefreshToken(String username) {
        logger.debug("Получение токена для пользователя={}", username);

        String token = stringRedisTemplate.opsForValue().get(REFRESH_PREFIX + username);
        if (token == null) {
            logger.debug("Токен не найден в кэше для пользователя={}", username);
        } else {
            logger.debug("Токен получен из кэша для пользователя={}", username);
        }
        return token;
    }

    public void deleteRefreshToken(String username) {
        logger.debug("Удаление токена из кэша для пользователя={}", username);

        stringRedisTemplate.delete(REFRESH_PREFIX + username);
    }
}
