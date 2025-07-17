package com.stc.project.repository;


import com.stc.project.core.CustomJpaRepository;
import com.stc.project.model.Building;

import java.util.List;

public interface BuildingRepository extends CustomJpaRepository<Building,Long> {

    List<Building> findByCreatedBy(String username);

    List<Building> findByProject_Id(Long projectId);

}



