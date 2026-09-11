package com.biopet.repository;

import com.biopet.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {
    Page<Pet> findAllByActivoTrue(Pageable pageable);
    Page<Pet> findAllByDuenioIdAndActivoTrue(Long duenioId, Pageable pageable);
    Optional<Pet> findByIdAndActivoTrue(Long id);
}
