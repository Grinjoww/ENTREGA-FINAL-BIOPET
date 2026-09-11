package com.biopet.dto;

import com.biopet.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para el CRUD administrativo de usuarios (POST/PUT /api/usuarios).
 * No confundir con {@code RegistrationRequest}: ese es el autoregistro público
 * (siempre ROLE_DUENO); este es la administración de cuentas por parte de un ADMIN.
 * <p>
 * El campo {@code password} no lleva {@code @NotBlank} a propósito: es obligatorio
 * al crear (validado explícitamente en {@code UserService.crear}) pero opcional
 * al actualizar (vacío/null conserva la contraseña actual).
 *
 * @param nombre full name, up to 100 characters
 * @param email account email, unique in the system
 * @param password account password between 8 and 80 characters, required on creation
 * @param rol account role
 */
public record UserRequest(
        @NotBlank @Size(max = 100) String nombre,
        @Email @NotBlank @Size(max = 255) String email,
        @Size(min = 8, max = 80) String password,
        @NotNull Role rol
) {}
