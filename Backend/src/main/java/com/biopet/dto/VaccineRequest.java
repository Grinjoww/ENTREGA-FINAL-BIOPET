package com.biopet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Input to register a vaccine application.
 *
 * @param mascotaId identifier of the vaccinated pet
 * @param veterinarioId identifier of the veterinarian, when recorded
 * @param tipo vaccine type, up to 60 characters
 * @param fechaAplicacion application date, not in the future
 * @param proximaFecha scheduled next dose date, when known
 * @param observaciones additional notes, up to 255 characters
 */
public record VaccineRequest(
        @NotNull Long mascotaId,
        Long veterinarioId,
        @NotBlank @Size(max = 60) String tipo,
        @NotNull @PastOrPresent LocalDate fechaAplicacion,
        LocalDate proximaFecha,
        @Size(max = 255) String observaciones
) {}
