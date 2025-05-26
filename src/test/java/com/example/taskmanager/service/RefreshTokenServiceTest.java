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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RefreshTokenService refreshTokenService;

    private static final String REFRESH_PREFIX = "refresh:";

    private static final String REFRESH_TOKEN = "testRefreshToken";

    private static String username;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(redisTemplate);
        username = "username";
    }

    @Test
    void storeRefreshToken_ShouldStoreRefreshTokenInRedis() {
        //arrange
        long ttlMillis = 60000L;

        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        //act
        refreshTokenService.storeRefreshToken(username, REFRESH_TOKEN, ttlMillis);

        //assert
        Mockito.verify(redisTemplate.opsForValue()).set(
                REFRESH_PREFIX + username,
                REFRESH_TOKEN,
                Duration.ofMillis(ttlMillis)
        );
    }

    @Test
    void getRefreshToken_ShouldReturnRefreshTokenFromRedis() {
        //arrange
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Mockito.when(redisTemplate.opsForValue().get(REFRESH_PREFIX + username)).thenReturn(REFRESH_TOKEN);

        //act
        String result = refreshTokenService.getRefreshToken(username);

        //assert
        assertEquals(result, REFRESH_TOKEN);
    }

    @Test
    void getRefreshToken_ShouldReturnNull_WhenNotFound() {
        //arrange
        username = "nonExistsUsername";
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Mockito.when(redisTemplate.opsForValue().get(REFRESH_PREFIX + username)).thenReturn(null);

        //act
        String result = refreshTokenService.getRefreshToken(username);

        //assert
        assertNull(result);
    }

    @Test
    void deleteRefreshToken_ShouldDeleteRefreshTokenFromRedis() {
        refreshTokenService.deleteRefreshToken(username);

        Mockito.verify(redisTemplate).delete(REFRESH_PREFIX + username);
    }
}
