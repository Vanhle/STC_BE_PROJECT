package com.stc.project.service;

import com.stc.project.constants.Constants;
import com.stc.project.core.CrudService;
import com.stc.project.model.Apartment;
import com.stc.project.model.Building;
import com.stc.project.model.Project;
import com.stc.project.repository.ApartmentRepository;
import com.stc.project.repository.BuildingRepository;
import com.stc.project.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class BuildingService extends CrudService<Building, Long> {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    private BuildingRepository buildingRepository;

    public BuildingService(BuildingRepository repository) {
        this.repository = this.buildingRepository = repository;
    }


    @Override
    protected void beforeCreate(Building building) {
        try {
            super.beforeCreate(building);
            assignAndValidateProject(building);
        } catch (Exception e) {
            throw new RuntimeException("Error while creating building: " + e.getMessage(), e);
        }
    }

    @Override
    protected void beforeUpdate(Building building) {
        try {
            super.beforeUpdate(building);
            assignAndValidateProject(building);
        } catch (Exception e) {
            throw new RuntimeException("Error while updating building: " + e.getMessage(), e);
        }
    }


    // Chỉ thêm và sửa Building nếu
    // KHÓA PHỤ ProjectId ko được null, có tồn tại ko, có đang active ko
    // Project phải đang hoạt động thì mới thêm được Building
    // code và name của Building là UNIQUE
    // code của Building phải phù hợp với code của Project (toàn bộ code của Project phải nằm trong code và name của Building)
    private void assignAndValidateProject(Building building) {
        Long projectId = building.getProjectId();

        if (projectId == null) {
            throw new IllegalArgumentException("projectId must not be null.");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project with id = " + projectId + " not found."));

        if (project.getActive() == null || project.getActive() != 1) {
            throw new IllegalArgumentException("Project must be active to add a building.");
        }

        String projectCode = project.getCode() != null ? project.getCode().toLowerCase() : "";
        String buildingCode = building.getCode() != null ? building.getCode().toLowerCase() : "";

        if (!buildingCode.contains(projectCode)) {
            throw new RuntimeException("The code of the building does not match the code of the project. Please check again.");
        }

        building.setProject(project);
    }

    // khi deactive tòa nhà đồng thời cx phải deactive căn hộ thuộc nó
    @Override
    public void deactivate(Long id) {
        try {
            super.deactivate(id);

            List<Apartment> apartments = apartmentRepository.findByBuilding_Id(id);
            for (Apartment apartment : apartments) {
                if (apartment.getDeletedAt() == null) {
                    apartment.setActive(Constants.EntityStatus.DEACTIVATED);
                    apartment.setDeactivatedAt(LocalDateTime.now());
                    apartmentRepository.save(apartment);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while deactivating building and its apartments: " + e.getMessage(), e);
        }
    }

    // khi xóa mềm tòa nhà đồng thời cx phải xóa mềm căn hộ thuộc nó
    @Override
    public void moveToTrash(Long id) {
        try {
            super.moveToTrash(id);

            List<Apartment> apartments = apartmentRepository.findByBuilding_Id(id);
            for (Apartment apartment : apartments) {
                if (apartment.getDeletedAt() == null) {
                    apartment.setActive(Constants.EntityStatus.IN_ACTIVE);
                    apartment.setDeletedAt(LocalDateTime.now());
                    apartment.setDeactivatedAt(null);
                    apartmentRepository.save(apartment);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while soft-deleting building and its apartments: " + e.getMessage(), e);
        }
    }

    // khi Xóa mềm building đồng thời cx phải xóa mềm toàn bộ apt thuộc tòa nhà đó
    @Override
    public void moveDeactivateToTrashAll() {
        try {
            super.moveDeactivateToTrashAll();

            List<Building> trashedBuildings = buildingRepository.findAll().stream()
                    .filter(b -> b.getDeletedAt() != null)
                    .collect(Collectors.toList());

            for (Building building : trashedBuildings) {
                List<Apartment> apartments = apartmentRepository.findByBuilding_Id(building.getId());
                for (Apartment apartment : apartments) {
                    if (apartment.getDeletedAt() == null) {
                        apartment.setActive(Constants.EntityStatus.IN_ACTIVE);
                        apartment.setDeletedAt(LocalDateTime.now());
                        apartment.setDeactivatedAt(null);
                        apartmentRepository.save(apartment);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while soft-deleting deactivated buildings and related apartments: " + e.getMessage(), e);
        }
    }

    // check thằng cha còn active hay ko thì mới restore thằng con được
    @Override
    protected void beforeRestore(Building building) {
        Project project = building.getProject();
        if (project == null || project.getActive() != Constants.EntityStatus.ACTIVE) {
            throw new IllegalStateException("Cannot restore Building with ID = " + building.getId()
                    + " because its Project is not active or does not exist.");
        }
    }

    // hàm này sd trong trang thống kê
    public long countActiveBuildings() {
        return buildingRepository.countActiveBuildings();
    }


}
