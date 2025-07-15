package com.stc.project.repository;

import com.stc.project.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, String> {
    boolean existsByName(String name);

    Set<Role> findByName(String name);

}
