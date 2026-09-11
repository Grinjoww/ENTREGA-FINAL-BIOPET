package com.biopet.dto;

import com.biopet.entity.Role;

/**
 * Read model of a user account returned by the API.
 *
 * @param id user identifier
 * @param nombre full name
 * @param email account email
 * @param rol account role
 * @param activo whether the account is active
 */
public record UserResponse(
        Long id,
        String nombre,
        String email,
        Role rol,
        boolean activo
) {}
