package com.biopet.repository;

import com.biopet.entity.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    Page<Appointment> findAllByActivoTrue(Pageable pageable);
    Page<Appointment> findAllByMascota_Duenio_IdAndActivoTrue(Long duenioId, Pageable pageable);
    Optional<Appointment> findByIdAndActivoTrue(Long id);
}
