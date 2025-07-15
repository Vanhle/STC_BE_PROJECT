package com.stc.project.repository;

import com.stc.project.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissionRepository extends JpaRepository<Permission, String> {
    boolean existsByName(String name);
}
