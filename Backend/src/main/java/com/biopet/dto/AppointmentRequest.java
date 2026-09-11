package com.biopet.dto;

import com.biopet.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * DTO de entrada para crear/actualizar una cita. El campo {@code estado} solo
 * tiene efecto en {@code PUT} (permite cancelar/completar); {@code POST} lo
 * ignora y fuerza siempre {@code PROGRAMADA}, igual que {@code RegistrationRequest.rol()}
 * es ignorado por {@code AuthService.register()} y se fuerza a ROLE_DUENO.
 *
 * @param mascotaId identifier of the pet the appointment is for
 * @param veterinarioId identifier of the assigned veterinarian
 * @param fechaHora scheduled date and time of the appointment
 * @param estado appointment status, only honored on update
 * @param motivo reason for the visit, up to 255 characters
 */
public record AppointmentRequest(
        @NotNull Long mascotaId,
        @NotNull Long veterinarioId,
        @NotNull Instant fechaHora,
        @NotNull AppointmentStatus estado,
        @Size(max = 255) String motivo
) {}
