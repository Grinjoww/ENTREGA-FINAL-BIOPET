package com.biopet.service;

import com.biopet.dto.AppointmentRequest;
import com.biopet.dto.AppointmentResponse;
import com.biopet.entity.Appointment;
import com.biopet.entity.AppointmentStatus;
import com.biopet.entity.Pet;
import com.biopet.entity.Rol;
import com.biopet.entity.User;
import com.biopet.exception.ResourceNotFoundException;
import com.biopet.repository.AppointmentRepository;
import com.biopet.repository.PetRepository;
import com.biopet.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD de citas (agendamiento previo de atención veterinaria). No reemplaza ni
 * duplica el futuro módulo de "Consultation" (registro clínico posterior), que
 * pertenece a otro integrante del equipo.
 * <p>
 * Reglas de acceso (aplicadas aquí porque dependen de datos, no solo del rol;
 * el control por rol "puro" ya vive en {@code AppointmentController} vía @PreAuthorize):
 * <ul>
 *   <li>DUENO: solo lee citas de sus propias mascotas (igual que PetService).</li>
 *   <li>VETERINARIO: lee todas, pero solo puede actualizar las citas donde él
 *       es el veterinario asignado.</li>
 *   <li>ADMIN/AUXILIAR: sin restricciones adicionales de datos.</li>
 * </ul>
 */
@Service
public class AppointmentService {
    private final AppointmentRepository citaRepository;
    private final PetRepository mascotaRepository;
    private final UserRepository usuarioRepository;

    public AppointmentService(AppointmentRepository citaRepository, PetRepository mascotaRepository, UserRepository usuarioRepository) {
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
     * @throws com.biopet.exception.ResourceNotFoundException if the authenticated user cannot be resolved
     */
    @Transactional(readOnly = true)
    public Page<AppointmentResponse> listar(Pageable pageable, String email) {
        User usuario = usuarioActual(email);
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
     * @throws com.biopet.exception.ResourceNotFoundException if no active appointment exists with the given id
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access to this appointment
     */
    @Transactional(readOnly = true)
    public AppointmentResponse buscar(Long id, String email) {
        User usuario = usuarioActual(email);
        Appointment cita = citaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment no encontrada: " + id));
        verificarAccesoLectura(usuario, cita);
        return toResponse(cita);
    }

    /**
     * Schedules a new appointment for the given pet and veterinarian.
     * Always created in {@code PROGRAMADA} status.
     *
     * @param request appointment data to create
     * @return the created appointment
     * @throws com.biopet.exception.ResourceNotFoundException if the referenced pet or veterinarian does not exist
     * @throws IllegalArgumentException if the referenced veterinarian does not have role ROLE_VETERINARIO
     */
    @Transactional
    public AppointmentResponse crear(AppointmentRequest request) {
        Pet mascota = resolverMascota(request.mascotaId());
        User veterinario = resolverVeterinario(request.veterinarioId());

        Appointment cita = Appointment.builder()
                .mascota(mascota)
                .veterinario(veterinario)
                .fechaHora(request.fechaHora())
                .estado(AppointmentStatus.PROGRAMADA)
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
     * @throws com.biopet.exception.ResourceNotFoundException if the appointment, pet or veterinarian does not exist
     * @throws org.springframework.security.access.AccessDeniedException if a VETERINARIO attempts to modify an appointment not assigned to them
     * @throws IllegalArgumentException if the referenced veterinarian does not have role ROLE_VETERINARIO
     */
    @Transactional
    public AppointmentResponse actualizar(Long id, AppointmentRequest request, String email) {
        User usuario = usuarioActual(email);
        Appointment cita = citaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment no encontrada: " + id));
        verificarPermisoEscritura(usuario, cita);

        Pet mascota = resolverMascota(request.mascotaId());
        User veterinario = resolverVeterinario(request.veterinarioId());

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
     * @throws com.biopet.exception.ResourceNotFoundException if no active appointment exists with the given id
     */
    @Transactional
    public void eliminar(Long id) {
        Appointment cita = citaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment no encontrada: " + id));
        cita.setActivo(false);
        citaRepository.save(cita);
    }

    private User usuarioActual(String email) {
        return usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado: " + email));
    }

    private Pet resolverMascota(Long mascotaId) {
        return mascotaRepository.findByIdAndActivoTrue(mascotaId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet no encontrada: " + mascotaId));
    }

    private User resolverVeterinario(Long veterinarioId) {
        User veterinario = usuarioRepository.findById(veterinarioId)
                .filter(User::isActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario no encontrado: " + veterinarioId));
        if (veterinario.getRol() != Rol.ROLE_VETERINARIO) {
            throw new IllegalArgumentException(
                    "El usuario asignado como veterinario debe tener rol ROLE_VETERINARIO: " + veterinarioId);
        }
        return veterinario;
    }

    private boolean tieneAccesoGlobal(Rol rol) {
        return rol == Rol.ROLE_ADMIN || rol == Rol.ROLE_VETERINARIO || rol == Rol.ROLE_AUXILIAR;
    }

    private void verificarAccesoLectura(User usuario, Appointment cita) {
        if (!tieneAccesoGlobal(usuario.getRol()) && !cita.getMascota().getDuenio().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("No tiene permisos para acceder a esta cita.");
        }
    }

    private void verificarPermisoEscritura(User usuario, Appointment cita) {
        if (usuario.getRol() == Rol.ROLE_VETERINARIO && !cita.getVeterinario().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("Solo puede modificar las citas asignadas a usted.");
        }
    }

    private AppointmentResponse toResponse(Appointment cita) {
        return new AppointmentResponse(
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
