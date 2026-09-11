package com.biopet.service;

import com.biopet.dto.PetRequest;
import com.biopet.dto.PetResponse;
import com.biopet.dto.SpeciesSummaryResponse;
import com.biopet.entity.Pet;
import com.biopet.entity.Role;
import com.biopet.entity.User;
import com.biopet.exception.ResourceNotFoundException;
import com.biopet.repository.PetRepository;
import com.biopet.repository.BiopetProcedureRepository;
import com.biopet.repository.UserRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD for pet records, plus a species-summary report backed by a
 * PostgreSQL stored procedure. Access rules that depend on data (not just
 * role) live here:
 * <ul>
 *   <li>DUENO: only reads/writes their own pets.</li>
 *   <li>ADMIN/VETERINARIO/AUXILIAR: no additional data restrictions.</li>
 * </ul>
 * The {@code listAll}/{@code crear}/{@code actualizar}/{@code eliminar}
 * results are cached in the {@code mascotas} Redis cache and evicted on
 * any write.
 */
@Service
public class PetService {
    private final PetRepository mascotaRepository;
    private final UserRepository usuarioRepository;
    private final BiopetProcedureRepository procedimientoBiopetRepository;

    public PetService(PetRepository mascotaRepository,
                          UserRepository usuarioRepository,
                          BiopetProcedureRepository procedimientoBiopetRepository) {
        this.mascotaRepository = mascotaRepository;
        this.usuarioRepository = usuarioRepository;
        this.procedimientoBiopetRepository = procedimientoBiopetRepository;
    }

    /**
     * Lists active pets, scoped to the caller's own pets for DUENO and
     * unrestricted for other roles, cached by user email and page
     * parameters.
     *
     * @param pageable pagination and sorting parameters
     * @param email authenticated user's email
     * @return page of pets
     * @throws com.biopet.exception.ResourceNotFoundException if the authenticated user cannot be resolved
     */
    @Cacheable(value = "mascotas", key = "#email + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    @Transactional(readOnly = true)
    public Page<PetResponse> listAll(Pageable pageable, String email) {
        User usuario = usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado"));

        if (usuario.getRol() == Role.ROLE_DUENO) {
            return mascotaRepository.findAllByDuenioIdAndActivoTrue(usuario.getId(), pageable).map(this::toResponse);
        }
        return mascotaRepository.findAllByActivoTrue(pageable).map(this::toResponse);
    }

    /**
     * Retrieves a single pet by id, enforcing that a DUENO may only
     * access their own pets.
     *
     * @param id pet identifier
     * @param email authenticated user's email
     * @return the requested pet
     * @throws com.biopet.exception.ResourceNotFoundException if no active pet exists with the given id
     * @throws org.springframework.security.access.AccessDeniedException if the user does not own this pet
     */
    @Transactional(readOnly = true)
    public PetResponse findById(Long id, String email) {
        User usuario = usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado"));
        Pet mascota = mascotaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet no encontrada: " + id));
        verificarPropiedad(usuario, mascota);
        return toResponse(mascota);
    }

    /**
     * Registers a new pet under the given owner, evicting the pets cache.
     *
     * @param request pet data to create
     * @return the created pet
     * @throws com.biopet.exception.ResourceNotFoundException if the referenced owner does not exist
     * @throws IllegalArgumentException if the referenced owner does not have role ROLE_DUENO
     */
    @CacheEvict(value = "mascotas", allEntries = true)
    @Transactional
    public PetResponse crear(PetRequest request) {
        User duenio = resolveOwner(request.duenioId());
        Pet mascota = Pet.builder()
                .duenio(duenio)
                .nombre(request.nombre())
                .especie(request.especie())
                .raza(request.raza())
                .fechaNacimiento(request.fechaNacimiento())
                .activo(true)
                .build();
        return toResponse(mascotaRepository.save(mascota));
    }

    /**
     * Updates an existing pet's data, enforcing that a DUENO may only
     * modify their own pets, and evicting the pets cache.
     *
     * @param id pet identifier
     * @param request updated pet data
     * @param email authenticated user's email
     * @return the updated pet
     * @throws com.biopet.exception.ResourceNotFoundException if the pet or the new owner does not exist
     * @throws org.springframework.security.access.AccessDeniedException if the user does not own this pet
     * @throws IllegalArgumentException if the referenced owner does not have role ROLE_DUENO
     */
    @CacheEvict(value = "mascotas", allEntries = true)
    @Transactional
    public PetResponse actualizar(Long id, PetRequest request, String email) {
        User usuario = usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado"));
        Pet mascota = mascotaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet no encontrada: " + id));
        verificarPropiedad(usuario, mascota);
        User duenio = resolveOwner(request.duenioId());
        mascota.setDuenio(duenio);
        mascota.setNombre(request.nombre());
        mascota.setEspecie(request.especie());
        mascota.setRaza(request.raza());
        mascota.setFechaNacimiento(request.fechaNacimiento());
        return toResponse(mascotaRepository.save(mascota));
    }

    /**
     * Soft-deletes a pet (marks it inactive), enforcing that a DUENO may
     * only delete their own pets, and evicting the pets cache.
     *
     * @param id pet identifier
     * @param email authenticated user's email
     * @throws com.biopet.exception.ResourceNotFoundException if no active pet exists with the given id
     * @throws org.springframework.security.access.AccessDeniedException if the user does not own this pet
     */
    @CacheEvict(value = "mascotas", allEntries = true)
    @Transactional
    public void eliminar(Long id, String email) {
        User usuario = usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado"));
        Pet mascota = mascotaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet no encontrada: " + id));
        verificarPropiedad(usuario, mascota);
        mascota.setActivo(false);
        mascotaRepository.save(mascota);
    }

    /**
     * Summarizes active pet counts grouped by species, via the
     * {@code fn_resumen_mascotas_por_especie} stored procedure. ADMIN may
     * request the summary for any owner; other roles are always scoped
     * to themselves regardless of the requested id.
     *
     * @param duenioIdSolicitado owner id requested (honored only for ADMIN)
     * @param emailAutenticado authenticated user's email
     * @return species and their active pet counts
     * @throws com.biopet.exception.ResourceNotFoundException if the authenticated user cannot be resolved
     */
    @Transactional(readOnly = true)
    public List<SpeciesSummaryResponse> speciesSummary(Long duenioIdSolicitado, String emailAutenticado) {
        User usuarioAutenticado = usuarioRepository.findByEmailAndActivoTrue(emailAutenticado)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado: " + emailAutenticado));

        Long duenioIdEfectivo = (usuarioAutenticado.getRol() == Role.ROLE_ADMIN)
                ? duenioIdSolicitado
                : usuarioAutenticado.getId();

        return procedimientoBiopetRepository.speciesSummary(duenioIdEfectivo).stream()
                .map(r -> new SpeciesSummaryResponse(r.getEspecie(), r.getTotal()))
                .toList();
    }

    private boolean tieneAccesoGlobal(Role rol) {
        return rol == Role.ROLE_ADMIN || rol == Role.ROLE_VETERINARIO || rol == Role.ROLE_AUXILIAR;
    }

    private void verificarPropiedad(User usuario, Pet mascota) {
        if (!tieneAccesoGlobal(usuario.getRol()) && !mascota.getDuenio().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("No tiene permisos para acceder a esta mascota.");
        }
    }

    private User resolveOwner(Long duenioId) {
        User duenio = usuarioRepository.findById(duenioId)
                .filter(User::isActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Dueño no encontrado: " + duenioId));
        if (duenio.getRol() != Role.ROLE_DUENO) {
            throw new IllegalArgumentException("El usuario asignado como dueño debe tener rol ROLE_DUENO: " + duenioId);
        }
        return duenio;
    }

    private PetResponse toResponse(Pet mascota) {
        return new PetResponse(
                mascota.getId(),
                mascota.getDuenio().getId(),
                mascota.getDuenio().getNombre(),
                mascota.getNombre(),
                mascota.getEspecie(),
                mascota.getRaza(),
                mascota.getFechaNacimiento(),
                mascota.isActivo(),
                mascota.getCreadoEn(),
                mascota.getActualizadoEn()
        );
    }
}