package com.biopet.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Token submitted to obtain a new access token.
 *
 * @param refreshToken the refresh JWT previously issued to the client
 */
public record RefreshRequest(@NotBlank String refreshToken) {}
