package com.biopet.controller;

import com.biopet.dto.ConsultaRequest;
import com.biopet.dto.ConsultaResponse;
import com.biopet.service.ConsultaService;
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
 * REST endpoints for clinical consultation ({@code Consulta}) records.
 * Role-based access is enforced here via {@code @PreAuthorize};
 * data-level ownership rules are enforced in {@link ConsultaService}.
 */
@RestController
@RequestMapping("/api/consultas")
public class ConsultaController {
    private final ConsultaService consultaService;

    public ConsultaController(ConsultaService consultaService) {
        this.consultaService = consultaService;
    }

    /**
     * Lists consultations visible to the authenticated user.
     *
     * @param pageable pagination and sorting parameters
     * @param userDetails authenticated user extracted from the security context
     * @return page of consultations the user is allowed to see
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR','DUENO')")
    public Page<ConsultaResponse> listar(Pageable pageable, @AuthenticationPrincipal UserDetails userDetails) {
        return consultaService.listar(pageable, userDetails.getUsername());
    }

    /**
     * Retrieves a single consultation by id, if the authenticated user has
     * access to it.
     *
     * @param id consultation identifier
     * @param userDetails authenticated user extracted from the security context
     * @return the requested consultation
     * @throws com.biopet.exception.RecursoNoEncontradoException if no active consultation exists with the given id
     */
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR','DUENO')")
    public ConsultaResponse buscar(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        return consultaService.buscar(id, userDetails.getUsername());
    }

    /**
     * Creates a new clinical consultation record for a given pet and
     * veterinarian.
     *
     * @param request consultation data to create
     * @return the created consultation, with HTTP 201 status
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR')")
    public ResponseEntity<ConsultaResponse> crear(@Valid @RequestBody ConsultaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(consultaService.crear(request));
    }

    /**
     * Updates an existing consultation record.
     *
     * @param id consultation identifier
     * @param request updated consultation data
     * @param userDetails authenticated user extracted from the security context
     * @return the updated consultation
     * @throws com.biopet.exception.RecursoNoEncontradoException if no active consultation exists with the given id
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR')")
    public ConsultaResponse actualizar(@PathVariable Long id, @Valid @RequestBody ConsultaRequest request,
                                        @AuthenticationPrincipal UserDetails userDetails) {
        return consultaService.actualizar(id, request, userDetails.getUsername());
    }

    /**
     * Soft-deletes a consultation record (marks it inactive; does not
     * remove the row).
     *
     * @param id consultation identifier
     * @param userDetails authenticated user extracted from the security context
     * @return empty response with HTTP 204 status
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIO','AUXILIAR')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        consultaService.eliminar(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}