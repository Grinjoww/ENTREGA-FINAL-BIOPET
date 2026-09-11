package com.biopet.repository;

import com.biopet.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndActivoTrue(String email);
    boolean existsByEmail(String email);
    Page<User> findAllByActivoTrue(Pageable pageable);
    Optional<User> findByIdAndActivoTrue(Long id);
}
