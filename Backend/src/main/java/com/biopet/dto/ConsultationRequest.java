package com.biopet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Input to register a clinical consultation.
 *
 * @param mascotaId identifier of the examined pet
 * @param veterinarioId identifier of the veterinarian performing it
 * @param fechaConsulta date and time of the consultation, not in the future
 * @param motivo reason for the visit, up to 200 characters
 * @param diagnostico diagnosis, up to 500 characters
 * @param tratamiento prescribed treatment, up to 500 characters
 * @param observaciones additional notes, up to 500 characters
 */
public record ConsultationRequest(
        @NotNull Long mascotaId,
        @NotNull Long veterinarioId,
        @NotNull @PastOrPresent Instant fechaConsulta,
        @NotBlank @Size(max = 200) String motivo,
        @Size(max = 500) String diagnostico,
        @Size(max = 500) String tratamiento,
        @Size(max = 500) String observaciones
) {}