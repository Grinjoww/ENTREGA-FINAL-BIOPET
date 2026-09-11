package com.biopet.dto;

import java.io.Serializable;
import java.time.Instant;

/**
 * Species information served to clients, from cache or the external API.
 *
 * @param especieConsultada species name originally requested
 * @param nombreCientifico scientific name, when provided by the source
 * @param habitat habitat, when provided by the source
 * @param dieta diet, when provided by the source
 * @param origen data source, {@code cache} or {@code api-ninjas}
 * @param consultadoEn instant the response was produced
 */
public record ExternalApiResponse(
        String especieConsultada,
        String nombreCientifico,
        String habitat,
        String dieta,
        String origen,        // "cache" o "api-ninjas"
        Instant consultadoEn
) implements Serializable {}