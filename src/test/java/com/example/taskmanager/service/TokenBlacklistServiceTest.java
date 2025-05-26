package com.example.taskmanager.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class TokenBlacklistServiceTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private TokenBlacklistService tokenBlacklistService;

    private static final String BLACKLIST_PREFIX = "blacklist:";

    private static final String TOKEN = "testToken";

    @BeforeEach
    void setUp() {
        tokenBlacklistService = new TokenBlacklistService(redisTemplate);
    }

    @Test
    void blacklistToken_shouldSetKeyInRedis() {
        //arrange
        long ttlMillis = 10000L;

        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        //act
        tokenBlacklistService.blacklistToken(TOKEN, ttlMillis);

        //assert
        Mockito.verify(redisTemplate.opsForValue()).set(
                BLACKLIST_PREFIX + TOKEN,
                "true",
                Duration.ofMillis(ttlMillis)
        );
    }

    @Test
    void isTokenBlacklisted_shouldReturnTrue_whenKeyExists() {
        //arrange
        Mockito.when(redisTemplate.hasKey(BLACKLIST_PREFIX + TOKEN)).thenReturn(true);

        //act
        boolean result = tokenBlacklistService.isTokenBlacklisted(TOKEN);

        //assert
        assertTrue(result);
    }

    @Test
    void isTokenBlacklisted_shouldReturnFalse_whenKeyDoesNotExists() {
        //arrange
        Mockito.when(redisTemplate.hasKey(BLACKLIST_PREFIX + TOKEN)).thenReturn(false);

        //act
        boolean result = tokenBlacklistService.isTokenBlacklisted(TOKEN);

        //assert
        assertFalse(result);
    }
}
