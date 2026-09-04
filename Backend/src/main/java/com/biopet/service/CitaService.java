package com.biopet.service;

import com.biopet.dto.CitaRequest;
import com.biopet.dto.CitaResponse;
import com.biopet.entity.Cita;
import com.biopet.entity.EstadoCita;
import com.biopet.entity.Mascota;
import com.biopet.entity.Rol;
import com.biopet.entity.Usuario;
import com.biopet.exception.RecursoNoEncontradoException;
import com.biopet.repository.CitaRepository;
import com.biopet.repository.MascotaRepository;
import com.biopet.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de citas (agendamiento previo de atención veterinaria). No reemplaza ni
 * duplica el futuro módulo de "Consulta" (registro clínico posterior), que
 * pertenece a otro integrante del equipo.
 * <p>
 * Reglas de acceso (aplicadas aquí porque dependen de datos, no solo del rol;
 * el control por rol "puro" ya vive en {@code CitaController} vía @PreAuthorize):
 * <ul>
 *   <li>DUENO: solo lee citas de sus propias mascotas (igual que MascotaService).</li>
 *   <li>VETERINARIO: lee todas, pero solo puede actualizar las citas donde él
 *       es el veterinario asignado.</li>
 *   <li>ADMIN/AUXILIAR: sin restricciones adicionales de datos.</li>
 * </ul>
 */
@Service
public class CitaService {
    private final CitaRepository citaRepository;
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;

    public CitaService(CitaRepository citaRepository, MascotaRepository mascotaRepository, UsuarioRepository usuarioRepository) {
        this.citaRepository = citaRepository;
        this.mascotaRepository = mascotaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Lists active appointments, scoped to the caller's own pets for
     * DUENO and unrestricted for other roles.
     *
     * @param pageable pagination and sorting parameters
     * @param email authenticated user's email
     * @return page of appointments
     * @throws com.biopet.exception.RecursoNoEncontradoException if the authenticated user cannot be resolved
     */
    @Transactional(readOnly = true)
    public Page<CitaResponse> listar(Pageable pageable, String email) {
        Usuario usuario = usuarioActual(email);
        if (usuario.getRol() == Rol.ROLE_DUENO) {
            return citaRepository.findAllByMascota_Duenio_IdAndActivoTrue(usuario.getId(), pageable).map(this::toResponse);
        }
        return citaRepository.findAllByActivoTrue(pageable).map(this::toResponse);
    }

    /**
     * Retrieves a single appointment by id, enforcing that a DUENO may
     * only access appointments for their own pets.
     *
     * @param id appointment identifier
     * @param email authenticated user's email
     * @return the requested appointment
     * @throws com.biopet.exception.RecursoNoEncontradoException if no active appointment exists with the given id
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access to this appointment
     */
    @Transactional(readOnly = true)
    public CitaResponse buscar(Long id, String email) {
        Usuario usuario = usuarioActual(email);
        Cita cita = citaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada: " + id));
        verificarAccesoLectura(usuario, cita);
        return toResponse(cita);
    }

    /**
     * Schedules a new appointment for the given pet and veterinarian.
     * Always created in {@code PROGRAMADA} status.
     *
     * @param request appointment data to create
     * @return the created appointment
     * @throws com.biopet.exception.RecursoNoEncontradoException if the referenced pet or veterinarian does not exist
     * @throws IllegalArgumentException if the referenced veterinarian does not have role ROLE_VETERINARIO
     */
    @Transactional
    public CitaResponse crear(CitaRequest request) {
        Mascota mascota = resolverMascota(request.mascotaId());
        Usuario veterinario = resolverVeterinario(request.veterinarioId());

        Cita cita = Cita.builder()
                .mascota(mascota)
                .veterinario(veterinario)
                .fechaHora(request.fechaHora())
                .estado(EstadoCita.PROGRAMADA)
                .motivo(request.motivo())
                .activo(true)
                .build();
        return toResponse(citaRepository.save(cita));
    }

    /**
     * Updates an existing appointment. A VETERINARIO may only update
     * appointments where they are the assigned veterinarian.
     *
     * @param id appointment identifier
     * @param request updated appointment data
     * @param email authenticated user's email
     * @return the updated appointment
     * @throws com.biopet.exception.RecursoNoEncontradoException if the appointment, pet or veterinarian does not exist
     * @throws org.springframework.security.access.AccessDeniedException if a VETERINARIO attempts to modify an appointment not assigned to them
     * @throws IllegalArgumentException if the referenced veterinarian does not have role ROLE_VETERINARIO
     */
    @Transactional
    public CitaResponse actualizar(Long id, CitaRequest request, String email) {
        Usuario usuario = usuarioActual(email);
        Cita cita = citaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada: " + id));
        verificarPermisoEscritura(usuario, cita);

        Mascota mascota = resolverMascota(request.mascotaId());
        Usuario veterinario = resolverVeterinario(request.veterinarioId());

        cita.setMascota(mascota);
        cita.setVeterinario(veterinario);
        cita.setFechaHora(request.fechaHora());
        cita.setEstado(request.estado());
        cita.setMotivo(request.motivo());
        return toResponse(citaRepository.save(cita));
    }

    /**
     * Soft-deletes an appointment (marks it inactive; does not remove
     * the row).
     *
     * @param id appointment identifier
     * @throws com.biopet.exception.RecursoNoEncontradoException if no active appointment exists with the given id
     */
    @Transactional
    public void eliminar(Long id) {
        Cita cita = citaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cita no encontrada: " + id));
        cita.setActivo(false);
        citaRepository.save(cita);
    }

    private Usuario usuarioActual(String email) {
        return usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + email));
    }

    private Mascota resolverMascota(Long mascotaId) {
        return mascotaRepository.findByIdAndActivoTrue(mascotaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada: " + mascotaId));
    }

    private Usuario resolverVeterinario(Long veterinarioId) {
        Usuario veterinario = usuarioRepository.findById(veterinarioId)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Veterinario no encontrado: " + veterinarioId));
        if (veterinario.getRol() != Rol.ROLE_VETERINARIO) {
            throw new IllegalArgumentException(
                    "El usuario asignado como veterinario debe tener rol ROLE_VETERINARIO: " + veterinarioId);
        }
        return veterinario;
    }

    private boolean tieneAccesoGlobal(Rol rol) {
        return rol == Rol.ROLE_ADMIN || rol == Rol.ROLE_VETERINARIO || rol == Rol.ROLE_AUXILIAR;
    }

    private void verificarAccesoLectura(Usuario usuario, Cita cita) {
        if (!tieneAccesoGlobal(usuario.getRol()) && !cita.getMascota().getDuenio().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("No tiene permisos para acceder a esta cita.");
        }
    }

    private void verificarPermisoEscritura(Usuario usuario, Cita cita) {
        if (usuario.getRol() == Rol.ROLE_VETERINARIO && !cita.getVeterinario().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("Solo puede modificar las citas asignadas a usted.");
        }
    }

    private CitaResponse toResponse(Cita cita) {
        return new CitaResponse(
                cita.getId(),
                cita.getMascota().getId(),
                cita.getMascota().getNombre(),
                cita.getVeterinario().getId(),
                cita.getVeterinario().getNombre(),
                cita.getFechaHora(),
                cita.getEstado(),
                cita.getMotivo(),
                cita.isActivo(),
                cita.getCreadoEn(),
                cita.getActualizadoEn()
        );
    }
}
