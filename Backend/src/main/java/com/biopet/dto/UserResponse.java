package com.biopet.dto;

import com.biopet.entity.Role;

public record UserResponse(
        Long id,
        String nombre,
        String email,
        Role rol,
        boolean activo
) {}
