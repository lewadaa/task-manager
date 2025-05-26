package com.example.taskmanager.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@RequiredArgsConstructor
public class RedisConnectionTest {
    private final StringRedisTemplate redisTemplate;

    @Bean
    public CommandLineRunner testRedisConnection() {
        return args -> {
            try {
                redisTemplate.opsForValue().set("redis:test", "connected");
                String value = redisTemplate.opsForValue().get("redis:test");

                if ("connected".equals(value)) {
                    System.out.println("✅Redis connected successfully");
                } else {
                    System.out.println("❌Redis connection failed");
                }
            } catch (Exception e) {
                System.out.println("Redis connection failed" + e.getMessage());
            }

        };
    }
}
