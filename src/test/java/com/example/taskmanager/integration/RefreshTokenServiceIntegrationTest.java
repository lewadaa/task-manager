package com.example.taskmanager.integration;

import com.example.taskmanager.service.RefreshTokenService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RefreshTokenServiceIntegrationTest {

    @Container
    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:8-alpine")
            .withExposedPorts(6379);

    @Container
    private static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testDb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));

        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", postgresContainer::getDriverClassName);
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
