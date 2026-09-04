package com.biopet.controller;

import com.biopet.dto.CitaRequest;
import com.biopet.dto.CitaResponse;
import com.biopet.service.CitaService;
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
 * REST endpoints for veterinary appointment ({@code Cita}) scheduling and
 * management. Role-based access is enforced here via {@code @PreAuthorize};
 * data-level ownership rules (e.g. a DUENO seeing only their own pets'
 * appointments) are enforced in {@link CitaService}.
 */
@RestController
@RequestMapping("/api/citas")
public class CitaController {
    private final CitaService citaService;

    public CitaController(CitaService citaService) {
        this.citaService = citaService;
    }

    /**
     * Lists appointments visible to the authenticated user; a DUENO sees
     * only appointments for their own pets, other roles see all.
     *
     * @param pageable pagination and sorting parameters
     * @param userDetails authenticated user extracted from the security context
     * @return page of appointments the user is allowed to see
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR','DUENO')")
    public Page<CitaResponse> listar(Pageable pageable, @AuthenticationPrincipal UserDetails userDetails) {
        return citaService.listar(pageable, userDetails.getUsername());
    }

    /**
     * Retrieves a single appointment by id, if the authenticated user has
     * access to it.
     *
     * @param id appointment identifier
     * @param userDetails authenticated user extracted from the security context
     * @return the requested appointment
     * @throws com.biopet.exception.RecursoNoEncontradoException if no active appointment exists with the given id
     */
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR','DUENO')")
    public CitaResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        return citaService.buscar(id, userDetails.getUsername());
    }

    /**
     * Creates a new appointment for a given pet and veterinarian.
     *
     * @param request appointment data to create
     * @return the created appointment, with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','AUXILIAR')")
    public ResponseEntity<CitaResponse> crear(@Valid @RequestBody CitaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.crear(request));
    }

    /**
     * Updates an existing appointment. A VETERINARIO may only update
     * appointments where they are the assigned veterinarian.
     *
     * @param id appointment identifier
     * @param request updated appointment data
     * @param userDetails authenticated user extracted from the security context
     * @return the updated appointment
     * @throws com.biopet.exception.RecursoNoEncontradoException if no active appointment exists with the given id
     */
    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','AUXILIAR','VETERINARIO')")
    public CitaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CitaRequest request,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        return citaService.actualizar(id, request, userDetails.getUsername());
    }

    /**
     * Soft-deletes an appointment (marks it inactive; does not remove the row).
     *
     * @param id appointment identifier
     * @return empty response with HTTP 204 status
     */
    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        citaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
