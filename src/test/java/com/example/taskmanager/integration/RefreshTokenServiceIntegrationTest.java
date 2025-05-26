package com.example.taskmanager.integration;

import com.example.taskmanager.service.RefreshTokenService;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RefreshTokenServiceIntegrationTest {

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7.2.4")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final String username = "username";
    private final String refreshToken = "testRefreshToken";

    @Test
    @Order(1)
    void storeRefreshToken_ShouldStoreRefreshTokenInRedis() {
        refreshTokenService.storeRefreshToken(username, refreshToken, 10000L);

        var result = stringRedisTemplate.opsForValue().get("refresh:" + username);

        assertEquals(refreshToken, result, "Token should be saved in redis");
    }

    @Test
    @Order(2)
    void getRefreshToken_ShouldReturnStoredRefreshTokenFromRedis() {
        var result = refreshTokenService.getRefreshToken(username);

         assertEquals(refreshToken, result, "Token should be returned from redis");
    }

    @Test
    @Order(3)
    void deleteRefreshToken_ShouldDeleteRefreshTokenFromRedis() {
        refreshTokenService.deleteRefreshToken(username);

        var result = stringRedisTemplate.opsForValue().get("refresh:" + username);

        assertNull(result, "Token should be deleted from redis");
    }
}
