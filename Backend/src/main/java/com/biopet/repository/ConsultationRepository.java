package com.biopet.repository;

import com.biopet.entity.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    /**
     * Lists active consultations with pagination.
     *
     * @param pageable the requested page
     * @return the page of active consultations
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Page<Consultation> findAllByActivoTrue(Pageable pageable);
    /**
     * Lists the active consultations of one pet with pagination.
     *
     * @param mascotaId identifier of the pet
     * @param pageable the requested page
     * @return the page of that pet's active consultations
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Page<Consultation> findAllByMascotaIdAndActivoTrue(Long mascotaId, Pageable pageable);
    /**
     * Finds an active consultation by identifier.
     *
     * @param id the consultation identifier
     * @return the active consultation, or empty when none matches
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Optional<Consultation> findByIdAndActivoTrue(Long id);
}