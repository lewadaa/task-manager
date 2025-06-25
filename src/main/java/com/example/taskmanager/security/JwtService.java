package com.example.taskmanager.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expirationAccessToken}")
    private long accessTokenExpirationTime;

    @Value("${jwt.expirationRefreshToken}")
    private long refreshTokenExpirationTime;

    private SecretKey key;

    public SecretKey getKey() {
        if (key == null) {
            try {
                byte[] keyBytes = Decoders.BASE64.decode(secret);
                key = Keys.hmacShaKeyFor(keyBytes);
                logger.info("JWT ключ успешно инициализирован");
            } catch (IllegalArgumentException e) {
                logger.error("Ошибка при инициализации JWT ключа", e);
                throw new BadCredentialsException("Invalid key");
            }
        }
        return key;
    }

    public String generateAccessToken(UserDetails userDetails) {
        logger.debug("Генерация access токена для пользователя {}", userDetails.getUsername());

        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpirationTime);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("roles", userDetails.getAuthorities())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(UserDetails userDetails) {
        logger.debug("Генерация refresh токена для пользователя {}", userDetails.getUsername());
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpirationTime);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getKey())
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token, String username) {
        logger.debug("Проверка валидности токена для пользователя {}", username);
        String extractedUsername = extractUsername(token);
        return extractedUsername.equals(username) && !isTokenExpired(token);
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            logger.warn("Токен просрочен: {}", e.getMessage());
            return true;
        }

    }
}
