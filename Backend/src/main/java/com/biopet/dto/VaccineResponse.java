package com.biopet.dto;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Read model of a vaccine application returned by the API.
 *
 * @param id vaccine record identifier
 * @param mascotaId identifier of the vaccinated pet
 * @param mascotaNombre name of the vaccinated pet
 * @param veterinarioId identifier of the veterinarian, when recorded
 * @param veterinarioNombre name of the veterinarian, when recorded
 * @param tipo vaccine type
 * @param fechaAplicacion date on which the vaccine was applied
 * @param proximaFecha scheduled next dose date, when known
 * @param observaciones additional notes
 * @param activo whether the record is active
 * @param creadoEn creation instant
 * @param actualizadoEn last update instant
 */
public record VaccineResponse(
        Long id,
        Long mascotaId,
        String mascotaNombre,
        Long veterinarioId,
        String veterinarioNombre,
        String tipo,
        LocalDate fechaAplicacion,
        LocalDate proximaFecha,
        String observaciones,
        boolean activo,
        Instant creadoEn,
        Instant actualizadoEn
) implements Serializable {}
