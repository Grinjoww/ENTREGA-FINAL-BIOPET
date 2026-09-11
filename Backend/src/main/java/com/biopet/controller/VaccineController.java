package com.biopet.controller;

import com.biopet.dto.VaccineRequest;
import com.biopet.dto.VaccineResponse;
import com.biopet.service.VaccineService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for vaccination ({@code Vaccine}) records. Role-based
 * access is enforced here via {@code @PreAuthorize}; data-level
 * ownership rules are enforced in {@link VaccineService}.
 */
@RestController
@RequestMapping("/api/vacunas")
public class VaccineController {
    private final VaccineService vacunaService;

    public VaccineController(VaccineService vacunaService) {
        this.vacunaService = vacunaService;
    }

    /**
     * Lists vaccination records visible to the authenticated user.
     *
     * @param pageable pagination and sorting parameters
     * @param userDetails authenticated user extracted from the security context
     * @return page of vaccination records the user is allowed to see
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR','DUENO')")
    public Page<VaccineResponse> listAll(Pageable pageable, @AuthenticationPrincipal UserDetails userDetails) {
        return vacunaService.listAll(pageable, userDetails.getUsername());
    }

    /**
     * Lists vaccination records for a specific pet, if the authenticated
     * user has access to that pet.
     *
     * @param mascotaId pet identifier
     * @param pageable pagination and sorting parameters
     * @param userDetails authenticated user extracted from the security context
     * @return page of vaccination records for the given pet
     * @throws com.biopet.exception.ResourceNotFoundException if no active pet exists with the given id
     */
    @GetMapping("/mascota/{mascotaId:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR','DUENO')")
    public Page<VaccineResponse> listByPet(@PathVariable Long mascotaId, Pageable pageable,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        return vacunaService.listByPet(mascotaId, pageable, userDetails.getUsername());
    }

    /**
     * Retrieves a single vaccination record by id, if the authenticated
     * user has access to it.
     *
     * @param id vaccination record identifier
     * @param userDetails authenticated user extracted from the security context
     * @return the requested vaccination record
     * @throws com.biopet.exception.ResourceNotFoundException if no active vaccination record exists with the given id
     */
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR','DUENO')")
    public VaccineResponse findById(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        return vacunaService.findById(id, userDetails.getUsername());
    }

    /**
     * Registers a new vaccination record for a given pet.
     *
     * @param request vaccination data to create
     * @return the created vaccination record, with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR')")
    public ResponseEntity<VaccineResponse> crear(@Valid @RequestBody VaccineRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vacunaService.crear(request));
    }

    /**
     * Updates an existing vaccination record.
     *
     * @param id vaccination record identifier
     * @param request updated vaccination data
     * @param userDetails authenticated user extracted from the security context
     * @return the updated vaccination record
     * @throws com.biopet.exception.ResourceNotFoundException if no active vaccination record exists with the given id
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR')")
    public VaccineResponse actualizar(@PathVariable Long id, @Valid @RequestBody VaccineRequest request,
                                      @AuthenticationPrincipal UserDetails userDetails) {
        return vacunaService.actualizar(id, request, userDetails.getUsername());
    }

    /**
     * Soft-deletes a vaccination record (marks it inactive; does not
     * remove the row).
     *
     * @param id vaccination record identifier
     * @param userDetails authenticated user extracted from the security context
     * @return empty response with HTTP 204 status
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        vacunaService.eliminar(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
