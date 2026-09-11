package com.biopet.dto;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Read model of a pet returned by the API.
 *
 * @param id pet identifier
 * @param duenioId identifier of the owning user
 * @param duenioNombre name of the owning user
 * @param nombre full name of the pet
 * @param especie species of the pet
 * @param raza breed of the pet
 * @param fechaNacimiento birth date of the pet
 * @param activo whether the pet is active
 * @param creadoEn creation instant
 * @param actualizadoEn last update instant
 */
public record PetResponse(
        Long id,
        Long duenioId,
        String duenioNombre,
        String nombre,
        String especie,
        String raza,
        LocalDate fechaNacimiento,
        boolean activo,
        Instant creadoEn,
        Instant actualizadoEn
) implements Serializable {}
