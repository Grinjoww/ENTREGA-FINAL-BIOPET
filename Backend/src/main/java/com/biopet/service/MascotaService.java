package com.biopet.service;

import com.biopet.dto.MascotaRequest;
import com.biopet.dto.MascotaResponse;
import com.biopet.dto.ResumenEspecieResponse;
import com.biopet.entity.Mascota;
import com.biopet.entity.Rol;
import com.biopet.entity.Usuario;
import com.biopet.exception.RecursoNoEncontradoException;
import com.biopet.repository.MascotaRepository;
import com.biopet.repository.ProcedimientoBiopetRepository;
import com.biopet.repository.UsuarioRepository;
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
 * The {@code listar}/{@code crear}/{@code actualizar}/{@code eliminar}
 * results are cached in the {@code mascotas} Redis cache and evicted on
 * any write.
 */
@Service
public class MascotaService {
    private final MascotaRepository mascotaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProcedimientoBiopetRepository procedimientoBiopetRepository;

    public MascotaService(MascotaRepository mascotaRepository,
                          UsuarioRepository usuarioRepository,
                          ProcedimientoBiopetRepository procedimientoBiopetRepository) {
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
     * @throws com.biopet.exception.RecursoNoEncontradoException if the authenticated user cannot be resolved
     */
    @Cacheable(value = "mascotas", key = "#email + '-' + #pageable.pageNumber + '-' + #pageable.pageSize + '-' + #pageable.sort.toString()")
    @Transactional(readOnly = true)
    public Page<MascotaResponse> listar(Pageable pageable, String email) {
        Usuario usuario = usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));

        if (usuario.getRol() == Rol.ROLE_DUENO) {
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
     * @throws com.biopet.exception.RecursoNoEncontradoException if no active pet exists with the given id
     * @throws org.springframework.security.access.AccessDeniedException if the user does not own this pet
     */
    @Transactional(readOnly = true)
    public MascotaResponse buscar(Long id, String email) {
        Usuario usuario = usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        Mascota mascota = mascotaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada: " + id));
        verificarPropiedad(usuario, mascota);
        return toResponse(mascota);
    }

    /**
     * Registers a new pet under the given owner, evicting the pets cache.
     *
     * @param request pet data to create
     * @return the created pet
     * @throws com.biopet.exception.RecursoNoEncontradoException if the referenced owner does not exist
     * @throws IllegalArgumentException if the referenced owner does not have role ROLE_DUENO
     */
    @CacheEvict(value = "mascotas", allEntries = true)
    @Transactional
    public MascotaResponse crear(MascotaRequest request) {
        Usuario duenio = resolverDuenio(request.duenioId());
        Mascota mascota = Mascota.builder()
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
     * @throws com.biopet.exception.RecursoNoEncontradoException if the pet or the new owner does not exist
     * @throws org.springframework.security.access.AccessDeniedException if the user does not own this pet
     * @throws IllegalArgumentException if the referenced owner does not have role ROLE_DUENO
     */
    @CacheEvict(value = "mascotas", allEntries = true)
    @Transactional
    public MascotaResponse actualizar(Long id, MascotaRequest request, String email) {
        Usuario usuario = usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        Mascota mascota = mascotaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada: " + id));
        verificarPropiedad(usuario, mascota);
        Usuario duenio = resolverDuenio(request.duenioId());
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
     * @throws com.biopet.exception.RecursoNoEncontradoException if no active pet exists with the given id
     * @throws org.springframework.security.access.AccessDeniedException if the user does not own this pet
     */
    @CacheEvict(value = "mascotas", allEntries = true)
    @Transactional
    public void eliminar(Long id, String email) {
        Usuario usuario = usuarioRepository.findByEmailAndActivoTrue(email)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        Mascota mascota = mascotaRepository.findByIdAndActivoTrue(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Mascota no encontrada: " + id));
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
     * @throws com.biopet.exception.RecursoNoEncontradoException if the authenticated user cannot be resolved
     */
    @Transactional(readOnly = true)
    public List<ResumenEspecieResponse> resumenPorEspecie(Long duenioIdSolicitado, String emailAutenticado) {
        Usuario usuarioAutenticado = usuarioRepository.findByEmailAndActivoTrue(emailAutenticado)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado: " + emailAutenticado));

        Long duenioIdEfectivo = (usuarioAutenticado.getRol() == Rol.ROLE_ADMIN)
                ? duenioIdSolicitado
                : usuarioAutenticado.getId();

        return procedimientoBiopetRepository.resumenPorEspecie(duenioIdEfectivo).stream()
                .map(r -> new ResumenEspecieResponse(r.getEspecie(), r.getTotal()))
                .toList();
    }

    private boolean tieneAccesoGlobal(Rol rol) {
        return rol == Rol.ROLE_ADMIN || rol == Rol.ROLE_VETERINARIO || rol == Rol.ROLE_AUXILIAR;
    }

    private void verificarPropiedad(Usuario usuario, Mascota mascota) {
        if (!tieneAccesoGlobal(usuario.getRol()) && !mascota.getDuenio().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("No tiene permisos para acceder a esta mascota.");
        }
    }

    private Usuario resolverDuenio(Long duenioId) {
        Usuario duenio = usuarioRepository.findById(duenioId)
                .filter(Usuario::isActivo)
                .orElseThrow(() -> new RecursoNoEncontradoException("Dueño no encontrado: " + duenioId));
        if (duenio.getRol() != Rol.ROLE_DUENO) {
            throw new IllegalArgumentException("El usuario asignado como dueño debe tener rol ROLE_DUENO: " + duenioId);
        }
        return duenio;
    }

    private MascotaResponse toResponse(Mascota mascota) {
        return new MascotaResponse(
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