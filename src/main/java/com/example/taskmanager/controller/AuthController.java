package com.example.taskmanager.controller;

import com.example.taskmanager.dto.*;
import com.example.taskmanager.security.JwtService;
import com.example.taskmanager.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Контроллер аутентификации", description = "Вход, выход и обновление токена")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsServiceImpl;
    private final CookieService cookieService;

    @Operation(
            summary = "Логин"
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthenticationRequest authenticationRequest,
                                   HttpServletResponse response) {
        logger.info("Попытка входа пользователя: {}", authenticationRequest.getUsername());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authenticationRequest.getUsername(),
                        authenticationRequest.getPassword())
        );
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        refreshTokenService.storeRefreshToken(userDetails.getUsername(), refreshToken, 604800000L);

        cookieService.addRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.ok(new AuthenticationResponse(accessToken, null));
    }

    @Operation(
            summary = "Выход"
    )
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader,
                                    HttpServletResponse response) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            logger.info("Выход пользователя с токеном: {}", token);

            tokenBlacklistService.blacklistToken(token, 15 * 60 * 1000);

            String username = jwtService.extractUsername(token);

            refreshTokenService.deleteRefreshToken(username);

            cookieService.removeRefreshTokenCookie(response);
        }
        return ResponseEntity.ok("Logout successful");
    }

    @Operation(
            summary = "Обновить jwt токен"
    )
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.refreshToken();
        String username = jwtService.extractUsername(refreshToken);

        String storedToken = refreshTokenService.getRefreshToken(username);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (!jwtService.isTokenValid(refreshToken, username)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(username);
        String newAccessToken = jwtService.generateAccessToken(userDetails);
        String newRefreshToken = jwtService.generateRefreshToken(userDetails);

        refreshTokenService.deleteRefreshToken(username);

        refreshTokenService.storeRefreshToken(username, newRefreshToken, 604800000L);

        logger.debug("Обновление токена для пользователя: {}", username);

        return ResponseEntity.ok(new AuthenticationResponse(newAccessToken, newRefreshToken));

    }
}
