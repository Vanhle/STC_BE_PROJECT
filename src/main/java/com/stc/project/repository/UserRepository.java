package com.stc.project.repository;

import com.stc.project.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findByEmailContainingIgnoreCase(String email, Boolean isActive, Pageable pageable);
    Page<User> findByIsActive(Boolean isActive, Pageable pageable);
    Page<User> findByEmailContainingIgnoreCaseAndIsActive(String email, Boolean isActive, Pageable pageable);
    Page<User> findByEmail(String email, Pageable pageable);
}

