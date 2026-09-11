package com.biopet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Input to register or update a pet.
 *
 * @param duenioId identifier of the owning user
 * @param nombre pet name, up to 50 characters
 * @param especie species, up to 30 characters
 * @param raza breed, up to 50 characters
 * @param fechaNacimiento birth date, not in the future
 */
public record PetRequest(
        @NotNull Long duenioId,
        @NotBlank @Size(max = 50) String nombre,
        @NotBlank @Size(max = 30) String especie,
        @NotBlank @Size(max = 50) String raza,
        @NotNull @PastOrPresent LocalDate fechaNacimiento
) {}
