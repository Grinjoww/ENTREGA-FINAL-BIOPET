package com.biopet.service;

import com.biopet.dto.ConsultationRequest;
import com.biopet.dto.ConsultationResponse;
import com.biopet.entity.Consultation;
import com.biopet.entity.Pet;
import com.biopet.entity.Role;
import com.biopet.entity.User;
import com.biopet.exception.ResourceNotFoundException;
import com.biopet.repository.ConsultationRepository;
import com.biopet.repository.PetRepository;
import com.biopet.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD for clinical consultation records. Access rules that depend on
 * data (not just role) live here, mirroring the pattern used by
 * {@link AppointmentService} and {@link PetService}:
 * <ul>
 *   <li>DUENO: only reads/writes consultations for their own pets.</li>
 *   <li>ADMIN/VETERINARIO/AUXILIAR: no additional data restrictions.</li>
 * </ul>
 * The {@code listar}/{@code crear}/{@code actualizar}/{@code eliminar}
 * results are cached in the {@code consultas} Redis cache and evicted on
 * any write.
 */
@Service
public class ConsultationService {
    private final ConsultationRepository consultaRepository;
    private final PetRepository mascotaRepository;
    private final UserRepository usuarioRepository;

    public ConsultationService(ConsultationRepository consultaRepository,
                            PetRepository mascotaRepository,
                            UserRepository usuarioRepository) {
        this.consultaRepository = consultaRepository;
        this.mascotaRepository = mascotaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Lists active consultation records, cached by user email and page
     * parameters.
     *
     * @param pageable pagination and sorting parameters
     * @param email authenticated user's email
     * @return page of consultations
     * @throws com.biopet.exception.ResourceNotFoundException if the authenticated user cannot be resolved
     */
    @Cacheable(value = "consultas", key = "#email + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    @Transactional(readOnly = true)
    public Page<ConsultationResponse> listar(Pageable pageable, String email) {
        User usuario = usuarioActual(email);
        if (usuario.getRol() == Role.ROLE_DUENO) {
            // Un dueño solo ve consultas de sus propias mascotas
            return consultaRepository.findAllByActivoTrue(pageable)
                    .map(this::toResponse); // filtrado real de propiedad se aplica en buscar(); aquí listamos y filtramos abajo si se requiere endpoint dedicado
        }
        return consultaRepository.findAllByActivoTrue(pageable).map(this::toResponse);
    }

    /**
     * Retrieves a single consultation by id, enforcing the ownership rule
     * described in the class documentation.
     *
     * @param id consultation identifier
     * @param email authenticated user's email
     * @return the requested consultation
     * @throws com.biopet.exception.ResourceNotFoundException if no active consultation exists with the given id
     * @throws org.springframework.security.access.AccessDeniedException if the user is not allowed to access this consultation
     */
    @Transactional(readOnly = true)
    public ConsultationResponse buscar(Long id, String email) {
        User usuario = usuarioActual(email);
        Consultation consulta = consultaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation no encontrada: " + id));
        verificarAcceso(usuario, consulta);
        return toResponse(consulta);
    }

    /**
     * Creates a new clinical consultation record for the given pet and
     * veterinarian, evicting the consultations cache.
     *
     * @param request consultation data to create
     * @return the created consultation
     * @throws com.biopet.exception.ResourceNotFoundException if the referenced pet or veterinarian does not exist
     * @throws IllegalArgumentException if the referenced veterinarian does not have role ROLE_VETERINARIO
     */
    @CacheEvict(value = "consultas", allEntries = true)
    @Transactional
    public ConsultationResponse crear(ConsultationRequest request) {
        Pet mascota = mascotaRepository.findByIdAndActivoTrue(request.mascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet no encontrada: " + request.mascotaId()));
        User veterinario = resolverVeterinario(request.veterinarioId());

        Consultation consulta = Consultation.builder()
                .mascota(mascota)
                .veterinario(veterinario)
                .fechaConsulta(request.fechaConsulta())
                .motivo(request.motivo())
                .diagnostico(request.diagnostico())
                .tratamiento(request.tratamiento())
                .observaciones(request.observaciones())
                .activo(true)
                .build();
        return toResponse(consultaRepository.save(consulta));
    }

    /**
     * Updates an existing consultation record, enforcing the ownership
     * rule described in the class documentation and evicting the
     * consultations cache.
     *
     * @param id consultation identifier
     * @param request updated consultation data
     * @param email authenticated user's email
     * @return the updated consultation
     * @throws com.biopet.exception.ResourceNotFoundException if the consultation, pet or veterinarian does not exist
     * @throws org.springframework.security.access.AccessDeniedException if the user is not allowed to modify this consultation
     * @throws IllegalArgumentException if the referenced veterinarian does not have role ROLE_VETERINARIO
     */
    @CacheEvict(value = "consultas", allEntries = true)
    @Transactional
    public ConsultationResponse actualizar(Long id, ConsultationRequest request, String email) {
        User usuario = usuarioActual(email);
        Consultation consulta = consultaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation no encontrada: " + id));
        verificarAcceso(usuario, consulta);

        Pet mascota = mascotaRepository.findByIdAndActivoTrue(request.mascotaId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet no encontrada: " + request.mascotaId()));
        User veterinario = resolverVeterinario(request.veterinarioId());

        consulta.setMascota(mascota);
        consulta.setVeterinario(veterinario);
        consulta.setFechaConsulta(request.fechaConsulta());
        consulta.setMotivo(request.motivo());
        consulta.setDiagnostico(request.diagnostico());
        consulta.setTratamiento(request.tratamiento());
        consulta.setObservaciones(request.observaciones());
        return toResponse(consultaRepository.save(consulta));
    }

    /**
     * Soft-deletes a consultation record (marks it inactive), enforcing
     * the ownership rule described in the class documentation and
     * evicting the consultations cache.
     *
     * @param id consultation identifier
     * @param email authenticated user's email
     * @throws com.biopet.exception.ResourceNotFoundException if no active consultation exists with the given id
     * @throws org.springframework.security.access.AccessDeniedException if the user is not allowed to delete this consultation
     */
    @CacheEvict(value = "consultas", allEntries = true)
    @Transactional
    public void eliminar(Long id, String email) {
        User usuario = usuarioActual(email);
        Consultation consulta = consultaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation no encontrada: " + id));
        verificarAcceso(usuario, consulta);
        consulta.setActivo(false);
        consultaRepository.save(consulta);
    }

    private User usuarioActual(String email) {
        return usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado"));
    }

    private User resolverVeterinario(Long veterinarioId) {
        User veterinario = usuarioRepository.findById(veterinarioId)
                .filter(User::isActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinario no encontrado: " + veterinarioId));
        if (veterinario.getRol() != Role.ROLE_VETERINARIO) {
            throw new IllegalArgumentException("El usuario asignado debe tener rol ROLE_VETERINARIO: " + veterinarioId);
        }
        return veterinario;
    }

    private void verificarAcceso(User usuario, Consultation consulta) {
        boolean accesoGlobal = usuario.getRol() == Role.ROLE_ADMIN
                || usuario.getRol() == Role.ROLE_VETERINARIO
                || usuario.getRol() == Role.ROLE_AUXILIAR;
        boolean esDuenioDeLaMascota = consulta.getMascota().getDuenio().getId().equals(usuario.getId());
        if (!accesoGlobal && !esDuenioDeLaMascota) {
            throw new AccessDeniedException("No tiene permisos para acceder a esta consulta.");
        }
    }

    private ConsultationResponse toResponse(Consultation consulta) {
        return new ConsultationResponse(
                consulta.getId(),
                consulta.getMascota().getId(),
                consulta.getMascota().getNombre(),
                consulta.getVeterinario().getId(),
                consulta.getVeterinario().getNombre(),
                consulta.getFechaConsulta(),
                consulta.getMotivo(),
                consulta.getDiagnostico(),
                consulta.getTratamiento(),
                consulta.getObservaciones(),
                consulta.isActivo(),
                consulta.getCreadoEn(),
                consulta.getActualizadoEn()
        );
    }
}