package com.biopet.repository;

import com.biopet.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {
    /**
     * Lists active pets with pagination.
     *
     * @param pageable the requested page
     * @return the page of active pets
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Page<Pet> findAllByActivoTrue(Pageable pageable);
    /**
     * Lists the active pets of one owner with pagination.
     *
     * @param duenioId identifier of the owning user
     * @param pageable the requested page
     * @return the page of that owner's active pets
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Page<Pet> findAllByDuenioIdAndActivoTrue(Long duenioId, Pageable pageable);
    /**
     * Finds an active pet by identifier.
     *
     * @param id the pet identifier
     * @return the active pet, or empty when none matches
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Optional<Pet> findByIdAndActivoTrue(Long id);
}
