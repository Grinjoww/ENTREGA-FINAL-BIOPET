package com.biopet.repository;

import com.biopet.entity.Vaccine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VaccineRepository extends JpaRepository<Vaccine, Long> {
    Page<Vaccine> findAllByActivoTrue(Pageable pageable);

    Page<Vaccine> findAllByMascotaIdAndActivoTrue(Long mascotaId, Pageable pageable);

    /** Para ROLE_DUENO: todas las vacunas de todas SUS mascotas, sin importar cuál. */
    Page<Vaccine> findAllByMascota_Duenio_IdAndActivoTrue(Long duenioId, Pageable pageable);

    Optional<Vaccine> findByIdAndActivoTrue(Long id);
}
