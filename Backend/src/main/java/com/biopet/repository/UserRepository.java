package com.biopet.repository;

import com.biopet.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Finds a user by email address.
     *
     * @param email the email to search for
     * @return the user, or empty when no account uses that email
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Optional<User> findByEmail(String email);
    /**
     * Finds an active user by email address.
     *
     * @param email the email to search for
     * @return the active user, or empty when none matches
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Optional<User> findByEmailAndActivoTrue(String email);
    /**
     * Checks whether an email address is already registered.
     *
     * @param email the email to check
     * @return true when an account already uses that email
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    boolean existsByEmail(String email);
    /**
     * Lists active users with pagination.
     *
     * @param pageable the requested page
     * @return the page of active users
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Page<User> findAllByActivoTrue(Pageable pageable);
    /**
     * Finds an active user by identifier.
     *
     * @param id the user identifier
     * @return the active user, or empty when none matches
     * @throws org.springframework.dao.DataAccessException if the query fails
     */
    Optional<User> findByIdAndActivoTrue(Long id);
}
