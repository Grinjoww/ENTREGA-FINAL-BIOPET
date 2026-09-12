package com.biopet.repository;

import com.biopet.entity.Vaccine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VaccineRepository extends JpaRepository<Vaccine, Long> {
    /**
     * Lists active vaccine records with pagination.
     *
     * @param pageable the requested page
     * @return the page of active vaccine records
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Page<Vaccine> findAllByActivoTrue(Pageable pageable);

    /**
     * Lists the active vaccine records of one pet with pagination.
     *
     * @param mascotaId identifier of the pet
     * @param pageable the requested page
     * @return the page of that pet's active vaccine records
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Page<Vaccine> findAllByMascotaIdAndActivoTrue(Long mascotaId, Pageable pageable);

    /**
     * Lists the active vaccine records of every pet owned by one user, with
     * pagination. For the owner role this covers all of their pets at once.
     *
     * @param duenioId identifier of the owning user
     * @param pageable the requested page
     * @return the page of that owner's active vaccine records
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Page<Vaccine> findAllByMascota_Duenio_IdAndActivoTrue(Long duenioId, Pageable pageable);

    /**
     * Finds an active vaccine record by identifier.
     *
     * @param id the vaccine record identifier
     * @return the active vaccine record, or empty when none matches
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Optional<Vaccine> findByIdAndActivoTrue(Long id);
}
