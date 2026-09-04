package com.biopet.controller;

import com.biopet.dto.UsuarioRequest;
import com.biopet.dto.UsuarioResponse;
import com.biopet.service.AuthService;
import com.biopet.service.UsuarioService;
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
 * REST endpoints for user account records. The self-service profile
 * endpoint ({@code /me}) is open to any authenticated user; the
 * administrative CRUD endpoints are restricted to ROLE_ADMIN.
 */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final AuthService authService;
    private final UsuarioService usuarioService;

    public UsuarioController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    /**
     * Returns the profile of the currently authenticated user.
     *
     * @param userDetails authenticated user extracted from the security context
     * @return the authenticated user's own profile
     */
    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal UserDetails userDetails) {
        return authService.perfil(userDetails.getUsername());
    }

    /**
     * Lists all user accounts (administrative operation).
     *
     * @param pageable pagination and sorting parameters
     * @return page of user accounts
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return usuarioService.listar(pageable);
    }

    /**
     * Retrieves a single user account by id (administrative operation).
     *
     * @param id user identifier
     * @return the requested user account
     * @throws com.biopet.exception.RecursoNoEncontradoException if no active user exists with the given id
     */
    @GetMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse buscar(@PathVariable Long id) {
        return usuarioService.buscar(id);
    }

    /**
     * Creates a new user account with the given role (administrative
     * operation; distinct from public self-registration).
     *
     * @param request user data to create, including email, password and role
     * @return the created user account, with HTTP 201 status
     * @throws com.biopet.exception.EmailDuplicadoException if the email is already registered
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    /**
     * Updates an existing user account. An administrator may not change
     * their own role through this endpoint.
     *
     * @param id user identifier
     * @param request updated user data
     * @param userDetails authenticated administrator extracted from the security context
     * @return the updated user account
     * @throws com.biopet.exception.RecursoNoEncontradoException if no active user exists with the given id
     * @throws com.biopet.exception.EmailDuplicadoException if the new email is already used by another account
     */
    @PutMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request,
                                       @AuthenticationPrincipal UserDetails userDetails) {
        return usuarioService.actualizar(id, request, userDetails.getUsername());
    }

    /**
     * Soft-deletes a user account (marks it inactive; does not remove the row).
     *
     * @param id user identifier
     * @return empty response with HTTP 204 status
     */
    @DeleteMapping("/{id:\\d+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
