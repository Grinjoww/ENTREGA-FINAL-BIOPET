package com.biopet.dto;

/**
 * Aggregated pet count for one species.
 *
 * @param especie species name
 * @param total number of active pets of the species
 */
public record SpeciesSummaryResponse(String especie, Long total) {
}