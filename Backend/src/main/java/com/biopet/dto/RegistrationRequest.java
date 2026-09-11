package com.biopet.dto;

import com.biopet.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data submitted for public self-registration; the role is always forced to owner.
 *
 * @param nombre full name, up to 100 characters
 * @param email account email, unique in the system
 * @param password account password between 8 and 80 characters
 * @param rol requested role, ignored and forced to owner on registration
 */
public record RegistrationRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Email @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 80) String password,
        @NotNull Role rol
) {}
