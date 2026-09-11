package com.biopet.dto;

/**
 * Tokens returned after a successful login or refresh.
 *
 * @param accessToken short-lived JWT used to call protected endpoints
 * @param refreshToken long-lived JWT used to obtain new access tokens
 * @param expiresIn lifetime of the access token in seconds
 */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {}
