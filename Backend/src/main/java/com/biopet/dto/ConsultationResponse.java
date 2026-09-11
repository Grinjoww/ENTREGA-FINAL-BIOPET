package com.biopet.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * Read model of a clinical consultation returned by the API.
 *
 * @param id consultation identifier
 * @param mascotaId identifier of the examined pet
 * @param mascotaNombre name of the examined pet
 * @param veterinarioId identifier of the veterinarian
 * @param veterinarioNombre name of the veterinarian
 * @param fechaConsulta date and time of the consultation
 * @param motivo reason for the visit
 * @param diagnostico recorded diagnosis
 * @param tratamiento prescribed treatment
 * @param observaciones additional notes
 * @param activo whether the record is active
 * @param creadoEn creation instant
 * @param actualizadoEn last update instant
 */
public record ConsultationResponse(
        Long id,
        Long mascotaId,
        String mascotaNombre,
        Long veterinarioId,
        String veterinarioNombre,
        Instant fechaConsulta,
        String motivo,
        String diagnostico,
        String tratamiento,
        String observaciones,
        boolean activo,
        Instant creadoEn,
        Instant actualizadoEn
) implements Serializable {}