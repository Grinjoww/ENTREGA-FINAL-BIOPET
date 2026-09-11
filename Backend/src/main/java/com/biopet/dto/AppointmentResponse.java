package com.biopet.dto;

import com.biopet.entity.AppointmentStatus;

import java.io.Serializable;
import java.time.Instant;

/**
 * Read model of an appointment returned by the API.
 *
 * @param id appointment identifier
 * @param mascotaId identifier of the pet
 * @param mascotaNombre name of the pet the appointment belongs to
 * @param veterinarioId identifier of the assigned veterinarian
 * @param veterinarioNombre name of the assigned veterinarian
 * @param fechaHora scheduled date and time
 * @param estado current appointment status
 * @param motivo reason for the visit
 * @param activo whether the appointment is active (no logical deletion applied)
 * @param creadoEn creation instant
 * @param actualizadoEn last update instant
 */
public record AppointmentResponse(
        Long id,
        Long mascotaId,
        String mascotaNombre,
        Long veterinarioId,
        String veterinarioNombre,
        Instant fechaHora,
        AppointmentStatus estado,
        String motivo,
        boolean activo,
        Instant creadoEn,
        Instant actualizadoEn
) implements Serializable {}
