package com.biopet.service;

import com.biopet.dto.UsuarioRequest;
import com.biopet.dto.UsuarioResponse;
import com.biopet.entity.Usuario;
import com.biopet.exception.EmailDuplicadoException;
import com.biopet.exception.RecursoNoEncontradoException;
import com.biopet.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD administrativo de usuarios (POST/PUT/DELETE /api/usuarios), restringido a
 * ROLE_ADMIN a nivel de {@code UsuarioController} (@PreAuthorize). No reemplaza ni
 * duplica {@code AuthService.registrar()}: aquella es el autoregistro público
 * (siempre ROLE_DUENO); este servicio permite a un administrador crear cuentas con
 * cualquier rol y gestionar cuentas existentes.
 */
@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Lists active user accounts.
     *
     * @param pageable pagination and sorting parameters
     * @return page of user accounts
     */
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return usuarioRepository.findAllByActivoTrue(pageable).map(this::toResponse);
    }

    /**
     * Retrieves a single user account by id.
     *
     * @param id user identifier
     * @return the requested user account
     * @throws com.biopet.exception.RecursoNoEncontradoException if no active user exists with the given id
     */
    @Transactional(readOnly = true)
    public UsuarioResponse buscar(Long id) {
        Usuario usuario = usuarioRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
        return toResponse(usuario);
    }

    /**
     * Creates a new user account with an administrator-chosen role. This
     * is distinct from public self-registration, which always creates a
     * ROLE_DUENO account.
     *
     * @param request user data to create, including email, password and role
     * @return the created user account
     * @throws com.biopet.exception.EmailDuplicadoException if the email is already registered
     * @throws IllegalArgumentException if no password is provided
     */
    @Transactional
    public UsuarioResponse crear(UsuarioRequest request) {
        String email = request.email().toLowerCase();
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailDuplicadoException(email);
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria al crear un usuario.");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.nombre())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .rol(request.rol())
                .activo(true)
                .build();
        return toResponse(usuarioRepository.save(usuario));
    }

    /**
     * Updates an existing user account. An administrator may not change
     * their own role through this method.
     *
     * @param id user identifier
     * @param request updated user data
     * @param emailAutenticado authenticated administrator's email
     * @return the updated user account
     * @throws com.biopet.exception.RecursoNoEncontradoException if the user or the authenticated administrator cannot be resolved
     * @throws org.springframework.security.access.AccessDeniedException if the administrator attempts to change their own role
     * @throws com.biopet.exception.EmailDuplicadoException if the new email is already used by another account
     */
    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioRequest request, String emailAutenticado) {
        Usuario usuario = usuarioRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));

        Usuario autenticado = usuarioRepository.findByEmailAndActivoTrue(emailAutenticado)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + emailAutenticado));
        if (autenticado.getId().equals(usuario.getId()) && request.rol() != usuario.getRol()) {
            throw new AccessDeniedException("No puede modificar su propio rol.");
        }

        String nuevoEmail = request.email().toLowerCase();
        usuarioRepository.findByEmail(nuevoEmail)
                .filter(otro -> !otro.getId().equals(usuario.getId()))
                .ifPresent(otro -> {
                    throw new EmailDuplicadoException(nuevoEmail);
                });

        usuario.setNombre(request.nombre());
        usuario.setEmail(nuevoEmail);
        usuario.setRol(request.rol());
        if (request.password() != null && !request.password().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return toResponse(usuarioRepository.save(usuario));
    }

    /**
     * Soft-deletes a user account (marks it inactive; does not remove
     * the row).
     *
     * @param id user identifier
     * @throws com.biopet.exception.RecursoNoEncontradoException if no active user exists with the given id
     */
    @Transactional
    public void eliminar(Long id) {
        Usuario usuario = usuarioRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + id));
        usuario.setActivo(false);
        usuarioRepository.save(usuario);
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNombre(), usuario.getEmail(), usuario.getRol(), usuario.isActivo());
    }
}
