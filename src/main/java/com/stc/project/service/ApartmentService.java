package com.stc.project.service;

import com.stc.project.constants.Constants;
import com.stc.project.core.CrudService;
import com.stc.project.model.Apartment;
import com.stc.project.model.Building;
import com.stc.project.repository.ApartmentRepository;
import com.stc.project.repository.BuildingRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Transactional
public class ApartmentService extends CrudService<Apartment, Long> {

    @Autowired
    private BuildingRepository buildingRepository;

    private final ApartmentRepository apartmentRepository;

    public ApartmentService(ApartmentRepository repository) {
        this.repository = this.apartmentRepository = repository;
    }

    @Override
    protected void beforeCreate(Apartment apartment) {
        try {
            super.beforeCreate(apartment);
            assignAndValidateBuilding(apartment);
        } catch (Exception e) {
            throw new RuntimeException("Error while creating apartment: " + e.getMessage(), e);
        }
    }

    @Override
    protected void beforeUpdate(Apartment apartment) {
        try {
            super.beforeUpdate(apartment);
            assignAndValidateBuilding(apartment);
        } catch (Exception e) {
            throw new RuntimeException("Error while updating apartment: " + e.getMessage(), e);
        }
    }


    // Chỉ thêm và sửa Apartment nếu
    // KHÓA PHỤ buildingId  ko được null, có tồn tại ko, có đang active ko
    // Building phải đang hoạt động thì mới thêm được Apartment
    // code và name của Apartment là UNIQUE (đã setting trong collumn sql)
    // code của Apartment phải phù hợp với code của Building (toàn bộ code của Building phải nằm trong code và name của Apartment)
    private void assignAndValidateBuilding(Apartment apartment) {
        Long buildingId = apartment.getBuildingId();

        if (buildingId == null) {
            throw new IllegalArgumentException("buildingId must not be null.");
        }

        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new EntityNotFoundException("Building with id = " + buildingId + " not found."));

        if (building.getActive() == null || building.getActive() != Constants.EntityStatus.ACTIVE) {
            throw new IllegalArgumentException("Building must be active to add an apartment.");
        }

        String buildingCode = building.getCode() != null ? building.getCode().toLowerCase() : "";
        String apartmentCode = apartment.getCode() != null ? apartment.getCode().toLowerCase() : "";

        if (!apartmentCode.contains(buildingCode)) {
            throw new RuntimeException("The code of the apartment does not match the code of the building. Please check again.");
        }

        apartment.setBuilding(building);
    }


    // check thằng cha còn active hay ko thì mới restore thằng con được
    @Override
    protected void beforeRestore(Apartment apartment) {
        Building building = apartment.getBuilding();
        if (building == null || building.getActive() != Constants.EntityStatus.ACTIVE) {
            throw new IllegalStateException("Cannot restore Apartment with ID = " + apartment.getId()
                    + " because its Building is not active or does not exist.");
        }
    }



}

