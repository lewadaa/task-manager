package com.example.taskmanager.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistService.class);

    private final StringRedisTemplate redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";

    public void blacklistToken(String token, long ttlMillis) {
        redisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + token,
                "true",
                Duration.ofMillis(ttlMillis)
        );
        logger.debug("Добавление токена в blacklist: {}, TTL: {} ms", token, ttlMillis);
    }

    public boolean isTokenBlacklisted(String token) {
        boolean blacklisted = Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
        logger.debug("Проверка токена на наличие в blacklist: {}, blacklisted={}", token, blacklisted);
        return blacklisted;
    }
}
