package com.biopet.repository;

import com.biopet.entity.Consultation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsultationRepository extends JpaRepository<Consultation, Long> {
    Page<Consultation> findAllByActivoTrue(Pageable pageable);
    Page<Consultation> findAllByMascotaIdAndActivoTrue(Long mascotaId, Pageable pageable);
    Optional<Consultation> findByIdAndActivoTrue(Long id);
}