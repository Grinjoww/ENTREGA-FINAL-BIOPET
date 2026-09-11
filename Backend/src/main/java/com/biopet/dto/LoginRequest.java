package com.biopet.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credentials submitted at login.
 *
 * @param email registered user email
 * @param password account password
 */
public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
) {}
