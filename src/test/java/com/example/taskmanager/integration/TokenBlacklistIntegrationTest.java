package com.example.taskmanager.integration;

import com.example.taskmanager.service.TokenBlacklistService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TokenBlacklistIntegrationTest {

    @Container
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7.2.4")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", redis::getHost);
        registry.add("spring.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private final String token = "testToken";

    @BeforeEach
    void setUp() {
        stringRedisTemplate.delete("blacklist:" + token);
    }

    @Test
    void blacklistToken_ShouldAddTokenToBlacklist() {
        tokenBlacklistService.blacklistToken(token, 10000L);

        var result = stringRedisTemplate.opsForValue().get("blacklist:" + token);

        assertEquals("true", result, "Token should be blacklisted");
    }

    @Test
    void blacklistToken_ShouldSetExpiration() {
        tokenBlacklistService.blacklistToken(token, 10000L);

        var result = stringRedisTemplate.getExpire("blacklist:" + token, TimeUnit.MILLISECONDS);

        assertTrue(result > 0 && result < 10000, "Expiration time should be set correctly");
    }

    @Test
    void isTokenBlacklisted_ShouldReturnTrue_WhenTokenBlacklisted() {
        tokenBlacklistService.blacklistToken(token, 10000L);

        assertTrue(tokenBlacklistService.isTokenBlacklisted(token));
    }

    @Test
    void isTokenBlacklisted_ShouldReturnFalse_WhenTokenNotBlacklisted() {
        assertFalse(tokenBlacklistService.isTokenBlacklisted("notBlacklistedToken"),
                "Token should not be blacklisted"
        );
    }
}
