package com.biopet.dto;

/**
 * Session lifetime returned to the client.
 *
 * @param expiresIn lifetime of the access token in seconds
 */
public record AuthSessionResponse(
        long expiresIn
) {}
