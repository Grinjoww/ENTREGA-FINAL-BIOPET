package com.biopet.dto;

import com.biopet.entity.AppointmentStatus;

import java.io.Serializable;
import java.time.Instant;

public record CitaResponse(
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
