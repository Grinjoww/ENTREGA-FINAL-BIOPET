package com.biopet.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class JwtCookieService {

    private static final String ACCESS_PATH = "/";
    private static final String REFRESH_PATH = "/api/auth";

    private final String accessCookieName;
    private final String refreshCookieName;
    private final boolean secure;
    private final String sameSite;
    private final Duration accessMaxAge;
    private final Duration refreshMaxAge;

    /**
     * Creates the cookie service from the configured cookie and token settings.
     *
     * @param accessCookieName name of the access token cookie
     * @param refreshCookieName name of the refresh token cookie
     * @param secure whether cookies carry the {@code Secure} attribute
     * @param sameSite value of the {@code SameSite} cookie attribute
     * @param expirationMs lifetime of the access cookie in milliseconds
     * @param refreshExpirationMs lifetime of the refresh cookie in milliseconds
     */
    public JwtCookieService(
            @Value("${security.cookie.access-name}") String accessCookieName,
            @Value("${security.cookie.refresh-name}") String refreshCookieName,
            @Value("${security.cookie.secure}") boolean secure,
            @Value("${security.cookie.same-site}") String sameSite,
            @Value("${security.jwt.expiration-ms}") long expirationMs,
            @Value("${security.jwt.refresh-expiration-ms}") long refreshExpirationMs
    ) {
        this.accessCookieName = accessCookieName;
        this.refreshCookieName = refreshCookieName;
        this.secure = secure;
        this.sameSite = sameSite;
        this.accessMaxAge = Duration.ofMillis(expirationMs);
        this.refreshMaxAge = Duration.ofMillis(refreshExpirationMs);
    }

    /**
     * Writes the access token cookie on the response.
     *
     * @param response the HTTP response receiving the cookie
     * @param token the access JWT to store in the cookie
     */
    public void addAccessCookie(HttpServletResponse response, String token) {
        writeCookie(response, accessCookieName, token, ACCESS_PATH, accessMaxAge);
    }

    /**
     * Writes the refresh token cookie on the response, scoped to the authentication paths.
     *
     * @param response the HTTP response receiving the cookie
     * @param token the refresh JWT to store in the cookie
     */
    public void addRefreshCookie(HttpServletResponse response, String token) {
        writeCookie(response, refreshCookieName, token, REFRESH_PATH, refreshMaxAge);
    }

    /**
     * Clears the access token cookie by overwriting it with an expired value.
     *
     * @param response the HTTP response receiving the expired cookie
     */
    public void clearAccessCookie(HttpServletResponse response) {
        writeCookie(response, accessCookieName, "", ACCESS_PATH, Duration.ZERO);
    }

    /**
     * Clears the refresh token cookie by overwriting it with an expired value.
     *
     * @param response the HTTP response receiving the expired cookie
     */
    public void clearRefreshCookie(HttpServletResponse response) {
        writeCookie(response, refreshCookieName, "", REFRESH_PATH, Duration.ZERO);
    }

    /**
     * Reads the access token from the request cookies.
     *
     * @param request the incoming HTTP request
     * @return the access token, or empty when the cookie is absent or blank
     */
    public Optional<String> readAccessToken(HttpServletRequest request) {
        return readCookie(request, accessCookieName);
    }

    /**
     * Reads the refresh token from the request cookies.
     *
     * @param request the incoming HTTP request
     * @return the refresh token, or empty when the cookie is absent or blank
     */
    public Optional<String> readRefreshToken(HttpServletRequest request) {
        return readCookie(request, refreshCookieName);
    }

    private void writeCookie(HttpServletResponse response, String name, String value, String path, Duration maxAge) {
        if (response.isCommitted()) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName())) {
                String value = cookie.getValue();
                if (value == null || value.isEmpty()) {
                    return Optional.empty();
                }
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
