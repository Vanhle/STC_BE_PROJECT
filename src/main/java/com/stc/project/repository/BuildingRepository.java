package com.stc.project.repository;


import com.stc.project.core.CustomJpaRepository;
import com.stc.project.model.Building;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BuildingRepository extends CustomJpaRepository<Building,Long> {

    List<Building> findByProject_Id(Long projectId);

    @Query(value = "SELECT COUNT(*) FROM building WHERE building.active = 1", nativeQuery = true)
    Long countActiveBuildings();
}



