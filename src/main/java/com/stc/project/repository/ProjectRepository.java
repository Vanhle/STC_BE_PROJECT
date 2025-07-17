package com.stc.project.repository;


import com.stc.project.core.CustomJpaRepository;
import com.stc.project.model.Project;

import java.util.List;

public interface ProjectRepository extends CustomJpaRepository<Project, Long> {

    List<Project> findByCreatedBy(String username);

}
