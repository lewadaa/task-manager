package com.example.taskmanager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest  {

    private JwtService jwtService;

    private final String secret = Base64.getEncoder()
            .encodeToString("my-super-secret-key-which-is-long-enough".getBytes());

    private final UserDetails user = new User(
            "testuser@example.com",
            "password",
            Collections.emptyList()
    );

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationTime", 3600000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationTime", 7200000L );

        jwtService.init();
    }

    @Test
    void generateAccessToken_ShouldContainCorrectUsername() {
        String token = jwtService.generateAccessToken(user);

        String username = jwtService.extractUsername(token);

        assertEquals(user.getUsername(), username);
    }

    @Test
    void generateRefreshToken_ShouldContainCorrectUsername() {
        String refreshToken = jwtService.generateRefreshToken(user);

        String username = jwtService.extractUsername(refreshToken);

        assertNotNull(refreshToken);
        assertEquals(user.getUsername(), username);
    }

    @Test
    void accessToken_ShouldContainRolesClaim() {
        String token = jwtService.generateAccessToken(user);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertTrue(claims.containsKey("roles"));
    }

    @Test
    void isTokenValid_ShouldReturnTrue_WhenTokenIsValid() {
        String token = jwtService.generateAccessToken(user);

        assertTrue(jwtService.isTokenValid(token, jwtService.extractUsername(token)));
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenUsernameNotValid() {
        String token = jwtService.generateAccessToken(user);

        assertFalse( jwtService.isTokenValid(token, "fakeUser"));
    }

    @Test
    void isTokenExpired_ShouldReturnTrue_WhenTokenIsExpired() {
        Date now = new Date();
        Date expiredDate = new Date(now.getTime() - 1000);

        byte[] keyBytes = Decoders.BASE64.decode(secret);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        String expiredToken = Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(now)
                .expiration(expiredDate)
                .signWith(key)
                .compact();

        assertTrue(jwtService.isTokenExpired(expiredToken));
    }

    @Test
    void isTokenExpired_ShouldReturnFalse_WhenTokenIsStillValid() {
        String token = jwtService.generateAccessToken(user);

        assertFalse(jwtService.isTokenExpired(token));
    }
}
