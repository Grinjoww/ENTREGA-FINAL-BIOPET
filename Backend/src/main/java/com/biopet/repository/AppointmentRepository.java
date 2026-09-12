package com.biopet.repository;

import com.biopet.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    /**
     * Lists active appointments with pagination.
     *
     * @param pageable the requested page
     * @return the page of active appointments
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Page<Appointment> findAllByActivoTrue(Pageable pageable);
    /**
     * Lists the active appointments of every pet owned by one user, with pagination.
     *
     * @param duenioId identifier of the owning user
     * @param pageable the requested page
     * @return the page of that owner's active appointments
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Page<Appointment> findAllByMascota_Duenio_IdAndActivoTrue(Long duenioId, Pageable pageable);
    /**
     * Finds an active appointment by identifier.
     *
     * @param id the appointment identifier
     * @return the active appointment, or empty when none matches
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Optional<Appointment> findByIdAndActivoTrue(Long id);
}
