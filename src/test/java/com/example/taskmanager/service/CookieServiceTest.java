package com.example.taskmanager.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CookieServiceTest {

    @Mock
    private HttpServletResponse response;

    private CookieService cookieService;

    @BeforeEach
    void setUp() {
        cookieService = new CookieService();
    }

    @Test
    void addRefreshTokenCookie_ShouldSetCorrectCookie() {
        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);

        String refreshToken = "testRefreshToken";

        cookieService.addRefreshTokenCookie(response, refreshToken);

        Mockito.verify(response).addCookie(captor.capture());

        Cookie cookie = captor.getValue();

        assertEquals(cookie.getName(), "refreshToken");
        assertEquals(cookie.getValue(), refreshToken);
        assertEquals(cookie.getPath(), "/api/auth/refresh");
        assertEquals(cookie.getMaxAge(), 7 * 24 * 60 * 60);
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getSecure());
    }

    @Test
    void removeRefreshTokenCookie_ShouldRemoveCookie() {
        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);

        cookieService.removeRefreshTokenCookie(response);

        Mockito.verify(response).addCookie(captor.capture());

        Cookie cookie = captor.getValue();

        assertEquals(cookie.getName(), "refreshToken");
        assertNull(cookie.getValue());
        assertEquals(cookie.getPath(), "/api/auth/refresh");
        assertEquals(cookie.getMaxAge(), 0);
    }
}
