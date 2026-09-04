package com.biopet.controller;

import com.biopet.dto.MascotaRequest;
import com.biopet.dto.MascotaResponse;
import com.biopet.dto.ResumenEspecieResponse;
import com.biopet.service.MascotaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for pet ({@code Mascota}) records. Role-based access is
 * enforced here via {@code @PreAuthorize}; data-level ownership rules
 * (a DUENO only sees their own pets) are enforced in {@link MascotaService}.
 */
@RestController
@RequestMapping("/api/mascotas")
public class MascotaController {
    private final MascotaService mascotaService;

    public MascotaController(MascotaService mascotaService) {
        this.mascotaService = mascotaService;
    }

    /**
     * Lists pets visible to the authenticated user; a DUENO sees only
     * their own pets, other roles see all.
     *
     * @param pageable pagination and sorting parameters
     * @param userDetails authenticated user extracted from the security context
     * @return page of pets the user is allowed to see
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR','DUENO')")
    public Page<MascotaResponse> listar(Pageable pageable, @AuthenticationPrincipal UserDetails userDetails) {
        return mascotaService.listar(pageable, userDetails.getUsername());
    }

    /**
     * Retrieves a single pet by id, if the authenticated user owns it or
     * has global access.
     *
     * @param id pet identifier
     * @param userDetails authenticated user extracted from the security context
     * @return the requested pet
     * @throws com.biopet.exception.RecursoNoEncontradoException if no active pet exists with the given id
     */
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR','DUENO')")
    public MascotaResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        return mascotaService.buscar(id, userDetails.getUsername());
    }

    /**
     * Registers a new pet under a given owner.
     *
     * @param request pet data to create
     * @return the created pet, with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR')")
    public ResponseEntity<MascotaResponse> crear(@Valid @RequestBody MascotaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(mascotaService.crear(request));
    }

    /**
     * Updates an existing pet's data.
     *
     * @param id pet identifier
     * @param request updated pet data
     * @param userDetails authenticated user extracted from the security context
     * @return the updated pet
     * @throws com.biopet.exception.RecursoNoEncontradoException if no active pet exists with the given id
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR')")
    public MascotaResponse actualizar(@PathVariable Long id, @Valid @RequestBody MascotaRequest request,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        return mascotaService.actualizar(id, request, userDetails.getUsername());
    }

    /**
     * Soft-deletes a pet (marks it inactive; does not remove the row).
     *
     * @param id pet identifier
     * @param userDetails authenticated user extracted from the security context
     * @return empty response with HTTP 204 status
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        mascotaService.eliminar(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }

    /**
     * Summarizes pet counts grouped by species, via a stored procedure.
     * ADMIN may pass {@code duenioId} to scope the summary to a specific
     * owner; other roles are always scoped to themselves.
     *
     * @param duenioId owner id to scope the summary to (ADMIN only; ignored for other roles)
     * @param authentication authenticated user extracted from the security context
     * @return species and their pet counts
     */
    @GetMapping("/resumen-especies")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR','DUENO')")
    public List<ResumenEspecieResponse> resumenPorEspecies(
            @RequestParam(required = false) Long duenioId,
            Authentication authentication) {
        return mascotaService.resumenPorEspecie(duenioId, authentication.getName());
    }
}