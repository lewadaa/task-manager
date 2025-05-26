package com.example.taskmanager.integration;

import com.example.taskmanager.dto.AuthenticationRequest;
import com.example.taskmanager.dto.AuthenticationResponse;
import com.example.taskmanager.dto.RefreshTokenRequest;
import com.example.taskmanager.security.JwtService;
import com.example.taskmanager.service.TokenBlacklistService;
import com.example.taskmanager.service.UserDetailsServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.AuthResponse;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Container
    static PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("testDb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.datasource.driver-class-name", postgresContainer::getDriverClassName);
    }

    @Test
    @Order(1)
    void login_ShouldLoginSuccessfullyAndSetRefreshTokenCookie() throws Exception {
        var loginRequest = new AuthenticationRequest("john", "user123");

        mvc.perform(
                post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();
    }

    @Test
    @Order(2)
    void login_ShouldReturn401_WhenWrongPassword() throws Exception {
        var loginRequest = new AuthenticationRequest("john", "wrongPassword");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    void refresh_ShouldRefreshAccessToken_WhenRefreshTokenIsValid() throws Exception {
        var loginRequest = new AuthenticationRequest("john", "user123");

        var result = mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        var refreshToken = parseRefreshTokenFromSetCookie(setCookieHeader);

        var refreshTokenRequest = new RefreshTokenRequest(refreshToken);

        mvc.perform(
                post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    @Order(4)
    void refresh_ShouldReturn401_WhenWrongRefreshToken() throws Exception {
        var refreshTokenRequest = new RefreshTokenRequest("wrong.refresh.token");

        mvc.perform(
                post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(5)
    void refresh_ShouldReturn401_WhenRefreshTokenIsMismatched() throws Exception {
        var loginRequest = new AuthenticationRequest("john", "user123");

        mvc.perform(
                post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var fakeRefreshTokenRequest = new RefreshTokenRequest("wrong.refresh.token");

        mvc.perform(
                        post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(fakeRefreshTokenRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(6)
    void logout_ShouldLogoutSuccessfully() throws Exception {
        var loginRequest = new AuthenticationRequest("john", "user123");

        var result = mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var responseBody = result.getResponse().getContentAsString();
        var authResponse = objectMapper.readValue(responseBody, AuthenticationResponse.class);

        mvc.perform(
                post("/api/auth/logout")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authResponse.getAccessToken()))
                .andExpect(status().isOk());
    }

    @Test
    @Order(7)
    void refresh_ShouldReturn401_WhenUserTriesRefreshAccessTokenByOldRefreshTokenAfterLogout() throws Exception {
        var loginRequest = new AuthenticationRequest("john", "user123");

        var result = mvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        var responseBody = result.getResponse().getContentAsString();
        var authResponse = objectMapper.readValue(responseBody, AuthenticationResponse.class);

        var setCookieHeader = result.getResponse().getHeader("Set-Cookie");
        var refreshToken = parseRefreshTokenFromSetCookie(setCookieHeader);

        var refreshTokenRequest = new RefreshTokenRequest(refreshToken);

        mvc.perform(
                post("/api/auth/logout")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + authResponse.getAccessToken()))
                .andExpect(status().isOk());

        mvc.perform(
                post("/api/auth/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(refreshTokenRequest)))
                .andExpect(status().isUnauthorized());
    }

    private String parseRefreshTokenFromSetCookie(String header) {
        if (header == null) {
            return null;
        }
        for (String part : header.split(";")) {
            if (part.trim().startsWith("refreshToken=")) {
                return part.trim().substring("refreshToken=".length());
            }
        }
        return null;
    }
}
