package com.biopet.controller;

import com.biopet.dto.ExternalApiResponse;
import com.biopet.integration.ExternalApiService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoint that proxies species information from an external animal
 * data API (api-ninjas), with a Redis-backed cache in
 * {@link ExternalApiService}.
 */
@RestController
@RequestMapping("/api/externa/especies")
public class ExternalApiController {

    private final ExternalApiService externalApiService;

    public ExternalApiController(ExternalApiService externalApiService) {
        this.externalApiService = externalApiService;
    }

    /**
     * Looks up biological/reference information for a species name,
     * serving a cached value when available.
     *
     * @param especie species name to look up
     * @return species information (taxonomy, habitat, diet)
     * @throws com.biopet.exception.ExternalApiException if the external API call fails or returns no results
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR','DUENO')")
    public ExternalApiResponse infoEspecie(@RequestParam String especie) {
        return externalApiService.obtenerInfoEspecie(especie);
    }
}