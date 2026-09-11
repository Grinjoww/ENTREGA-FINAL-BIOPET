package com.biopet.service;

import com.biopet.dto.VaccineRequest;
import com.biopet.dto.VaccineResponse;
import com.biopet.entity.Pet;
import com.biopet.entity.Rol;
import com.biopet.entity.Usuario;
import com.biopet.entity.Vaccine;
import com.biopet.exception.ResourceNotFoundException;
import com.biopet.repository.PetRepository;
import com.biopet.repository.UsuarioRepository;
import com.biopet.repository.VaccineRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for vaccination records. Access rules that depend on data (not
 * just role) mirror the pattern used by {@link AppointmentService} and
 * {@link PetService}:
 * <ul>
 *   <li>DUENO: only reads/writes vaccination records for their own pets.</li>
 *   <li>ADMIN/VETERINARIO/AUXILIAR: no additional data restrictions.</li>
 * </ul>
 */
@Service
public class VaccineService {
    private final VaccineRepository vacunaRepository;
    private final PetRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;

    public VaccineService(VaccineRepository vacunaRepository,
                          PetRepository mascotaRepository,
                          UsuarioRepository usuarioRepository) {
        this.vacunaRepository = vacunaRepository;
        this.mascotaRepository = mascotaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Lists active vaccination records, scoped to the caller's own pets
     * for DUENO and unrestricted for other roles.
     *
     * @param pageable pagination and sorting parameters
     * @param email authenticated user's email
     * @return page of vaccination records
     * @throws com.biopet.exception.ResourceNotFoundException if the authenticated user cannot be resolved
     */
    @Transactional(readOnly = true)
    public Page<VaccineResponse> listar(Pageable pageable, String email) {
        Usuario usuario = usuarioActivo(email);
        if (usuario.getRol() == Rol.ROLE_DUENO) {
            return vacunaRepository.findAllByMascota_Duenio_IdAndActivoTrue(usuario.getId(), pageable)
                    .map(this::toResponse);
        }
        return vacunaRepository.findAllByActivoTrue(pageable).map(this::toResponse);
    }

    /**
     * Lists active vaccination records for a specific pet, enforcing
     * that the caller has access to that pet.
     *
     * @param mascotaId pet identifier
     * @param pageable pagination and sorting parameters
     * @param email authenticated user's email
     * @return page of vaccination records for the given pet
     * @throws com.biopet.exception.ResourceNotFoundException if the authenticated user or the pet cannot be resolved
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access to this pet
     */
    @Transactional(readOnly = true)
    public Page<VaccineResponse> listarPorMascota(Long mascotaId, Pageable pageable, String email) {
        Usuario usuario = usuarioActivo(email);
        Pet mascota = mascotaActiva(mascotaId);
        verificarAcceso(usuario, mascota);
        return vacunaRepository.findAllByMascotaIdAndActivoTrue(mascotaId, pageable).map(this::toResponse);
    }

    /**
     * Retrieves a single vaccination record by id, enforcing that the
     * caller has access to the associated pet.
     *
     * @param id vaccination record identifier
     * @param email authenticated user's email
     * @return the requested vaccination record
     * @throws com.biopet.exception.ResourceNotFoundException if no active vaccination record exists with the given id
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access to the associated pet
     */
    @Transactional(readOnly = true)
    public VaccineResponse buscar(Long id, String email) {
        Usuario usuario = usuarioActivo(email);
        Vaccine vacuna = vacunaActiva(id);
        verificarAcceso(usuario, vacuna.getMascota());
        return toResponse(vacuna);
    }

    /**
     * Registers a new vaccination record for the given pet and
     * veterinarian.
     *
     * @param request vaccination data to create
     * @return the created vaccination record
     * @throws com.biopet.exception.ResourceNotFoundException if the referenced pet or veterinarian does not exist
     * @throws IllegalArgumentException if the referenced veterinarian does not have role ROLE_VETERINARIO
     */
    @Transactional
    public VaccineResponse crear(VaccineRequest request) {
        Pet mascota = mascotaActiva(request.mascotaId());
        Usuario veterinario = resolverVeterinario(request.veterinarioId());
        Vaccine vacuna = Vaccine.builder()
                .mascota(mascota)
                .veterinario(veterinario)
                .tipo(request.tipo())
                .fechaAplicacion(request.fechaAplicacion())
                .proximaFecha(request.proximaFecha())
                .observaciones(request.observaciones())
                .activo(true)
                .build();
        return toResponse(vacunaRepository.save(vacuna));
    }

    /**
     * Updates an existing vaccination record, enforcing that the caller
     * has access to the associated pet.
     *
     * @param id vaccination record identifier
     * @param request updated vaccination data
     * @param email authenticated user's email
     * @return the updated vaccination record
     * @throws com.biopet.exception.ResourceNotFoundException if the vaccination record, pet or veterinarian does not exist
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access to the associated pet
     * @throws IllegalArgumentException if the referenced veterinarian does not have role ROLE_VETERINARIO
     */
    @Transactional
    public VaccineResponse actualizar(Long id, VaccineRequest request, String email) {
        Usuario usuario = usuarioActivo(email);
        Vaccine vacuna = vacunaActiva(id);
        verificarAcceso(usuario, vacuna.getMascota());

        Pet mascota = mascotaActiva(request.mascotaId());
        Usuario veterinario = resolverVeterinario(request.veterinarioId());

        vacuna.setMascota(mascota);
        vacuna.setVeterinario(veterinario);
        vacuna.setTipo(request.tipo());
        vacuna.setFechaAplicacion(request.fechaAplicacion());
        vacuna.setProximaFecha(request.proximaFecha());
        vacuna.setObservaciones(request.observaciones());
        return toResponse(vacunaRepository.save(vacuna));
    }

    /**
     * Soft-deletes a vaccination record (marks it inactive), enforcing
     * that the caller has access to the associated pet.
     *
     * @param id vaccination record identifier
     * @param email authenticated user's email
     * @throws com.biopet.exception.ResourceNotFoundException if no active vaccination record exists with the given id
     * @throws org.springframework.security.access.AccessDeniedException if the user does not have access to the associated pet
     */
    @Transactional
    public void eliminar(Long id, String email) {
        Usuario usuario = usuarioActivo(email);
        Vaccine vacuna = vacunaActiva(id);
        verificarAcceso(usuario, vacuna.getMascota());
        vacuna.setActivo(false);
        vacunaRepository.save(vacuna);
    }

    // ---------- Helpers ----------

    private Usuario usuarioActivo(String email) {
        return usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private Pet mascotaActiva(Long mascotaId) {
        return mascotaRepository.findByIdAndActivoTrue(mascotaId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet no encontrada: " + mascotaId));
    }

    private Vaccine vacunaActiva(Long id) {
        return vacunaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vaccine no encontrada: " + id));
    }

    private Usuario resolverVeterinario(Long veterinarioId) {
        if (veterinarioId == null) return null;
        Usuario veterinario = usuarioRepository.findById(veterinarioId)
                .filter(Usuario::isActivo)
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

    private void verificarAcceso(Usuario usuario, Pet mascota) {
        if (!tieneAccesoGlobal(usuario.getRol()) && !mascota.getDuenio().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("No tiene permisos para acceder a esta vacuna.");
        }
    }

    private VaccineResponse toResponse(Vaccine vacuna) {
        Usuario veterinario = vacuna.getVeterinario();
        return new VaccineResponse(
                vacuna.getId(),
                vacuna.getMascota().getId(),
                vacuna.getMascota().getNombre(),
                veterinario != null ? veterinario.getId() : null,
                veterinario != null ? veterinario.getNombre() : null,
                vacuna.getTipo(),
                vacuna.getFechaAplicacion(),
                vacuna.getProximaFecha(),
                vacuna.getObservaciones(),
                vacuna.isActivo(),
                vacuna.getCreadoEn(),
                vacuna.getActualizadoEn()
        );
    }
}
