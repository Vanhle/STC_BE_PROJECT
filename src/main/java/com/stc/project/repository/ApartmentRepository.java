package com.stc.project.repository;


import com.stc.project.core.CustomJpaRepository;
import com.stc.project.model.Apartment;

import java.util.List;

public interface ApartmentRepository extends CustomJpaRepository<Apartment, Long> {

    List<Apartment> findByBuilding_Id(Long buildingId);

}
