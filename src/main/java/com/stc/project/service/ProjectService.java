package com.stc.project.service;

import com.stc.project.constants.Constants;
import com.stc.project.core.CrudService;
import com.stc.project.model.Apartment;
import com.stc.project.model.Building;
import com.stc.project.model.Project;
import com.stc.project.repository.ApartmentRepository;
import com.stc.project.repository.BuildingRepository;
import com.stc.project.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProjectService extends CrudService<Project, Long> {

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private ApartmentRepository apartmentRepository;

    private ProjectRepository projectRepository;

    public ProjectService(ProjectRepository repository) {
        this.repository = this.projectRepository = repository;
    }

    public static void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to the expected completion date!");
        }
    }

    @Override
    public void beforeCreate(Project newProject) {
        try {
            super.beforeCreate(newProject);
            validateDateRange(newProject.getConstructionStartDateFrom(), newProject.getExpectedCompletionDate());
        } catch (Exception e) {
            throw new RuntimeException("Error during project creation: " + e.getMessage(), e);
        }
    }


    // Nếu thay đổi code của Project thì cũng thay đổi luôn code của Building và Apartment
    @Override
    public void beforeUpdate(Project newProject) {
        try {
            super.beforeUpdate(newProject);
            validateDateRange(newProject.getConstructionStartDateFrom(), newProject.getExpectedCompletionDate());

            Project oldProject = get(newProject.getId());
            String oldCode = oldProject.getCode();
            String newCode = newProject.getCode();

            boolean codeChanged = !oldCode.equals(newCode);

            if (codeChanged) {
                List<Building> buildings = buildingRepository.findByProject_Id(newProject.getId());
                for (Building building : buildings) {
                    String buildingCode = building.getCode();
                    if (buildingCode != null && buildingCode.contains(oldCode)) {
                        building.setCode(buildingCode.replaceFirst(Pattern.quote(oldCode), newCode));
                    }

                    List<Apartment> apartments = apartmentRepository.findByBuilding_Id(building.getId());
                    for (Apartment apartment : apartments) {
                        String aptCode = apartment.getCode();
                        if (aptCode != null && aptCode.contains(oldCode)) {
                            apartment.setCode(aptCode.replaceFirst(Pattern.quote(oldCode), newCode));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error during project update: " + e.getMessage(), e);
        }
    }



    // khi deactive dự án đồng thời cx phải deactive tòa nhà thuộc nó và apt thuộc tòa nhà đó
    @Override
    public void deactivate(Long id) {
        try {
            super.deactivate(id);
            List<Building> buildings = buildingRepository.findByProject_Id(id);
            for (Building building : buildings) {
                if (building.getDeletedAt() == null) {
                    building.setActive(Constants.EntityStatus.DEACTIVATED);
                    building.setDeactivatedAt(LocalDateTime.now());

                    List<Apartment> apartments = apartmentRepository.findByBuilding_Id(building.getId());
                    for (Apartment apartment : apartments) {
                        if (apartment.getDeletedAt() == null) {
                            apartment.setActive(Constants.EntityStatus.DEACTIVATED);
                            apartment.setDeactivatedAt(LocalDateTime.now());
                            apartmentRepository.save(apartment);
                        }
                    }
                    buildingRepository.save(building);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while deactivating project and its related entities: " + e.getMessage(), e);
        }
    }


    // khi Xóa mềm dự án đồng thời cx phải xóa mềm toàn bộ tòa nhà thuộc nó và apt thuộc tòa nhà đó
    @Override
    public void moveToTrash(Long id) {
        try {
            super.moveToTrash(id);
            List<Building> buildings = buildingRepository.findByProject_Id(id);
            for (Building building : buildings) {
                if (building.getDeletedAt() == null) {
                    building.setActive(Constants.EntityStatus.IN_ACTIVE);
                    building.setDeletedAt(LocalDateTime.now());
                    building.setDeactivatedAt(null);

                    List<Apartment> apartments = apartmentRepository.findByBuilding_Id(building.getId());
                    for (Apartment apartment : apartments) {
                        if (apartment.getDeletedAt() == null) {
                            apartment.setActive(Constants.EntityStatus.IN_ACTIVE);
                            apartment.setDeletedAt(LocalDateTime.now());
                            apartment.setDeactivatedAt(null);
                            apartmentRepository.save(apartment);
                        }
                    }
                    buildingRepository.save(building);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while soft-deleting project and related entities: " + e.getMessage(), e);
        }
    }

    // khi Xóa mềm toàn bộ dự án đang bị vô hiệu hóa đồng thời cx phải xóa mềm toàn bộ tòa nhà thuộc nó và apt thuộc tòa nhà đó đang bị vô hiệu hóa
    @Override
    public void moveDeactivateToTrashAll() {
        try {
            super.moveDeactivateToTrashAll();

            // Tìm các Project vừa bị xóa mềm
            List<Project> trashedProjects = projectRepository.findAll().stream()
                    .filter(p -> p.getDeletedAt() != null)
                    .collect(Collectors.toList());

            for (Project project : trashedProjects) {
                List<Building> buildings = buildingRepository.findByProject_Id(project.getId());

                for (Building building : buildings) {
                    if (building.getDeletedAt() == null && building.getDeactivatedAt() != null) {
                        building.setDeletedAt(LocalDateTime.now());
                        building.setActive(Constants.EntityStatus.IN_ACTIVE);
                        building.setDeactivatedAt(null);
                        buildingRepository.save(building);
                    }

                    // Xử lý tiếp Apartment thuộc Building
                    List<Apartment> apartments = apartmentRepository.findByBuilding_Id(building.getId());
                    for (Apartment apartment : apartments) {
                        if (apartment.getDeletedAt() == null && apartment.getDeactivatedAt() != null) {
                            apartment.setActive(Constants.EntityStatus.IN_ACTIVE);
                            apartment.setDeletedAt(LocalDateTime.now());
                            apartment.setDeactivatedAt(null);
                            apartmentRepository.save(apartment);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while soft-deleting all deactivated projects and related entities: " + e.getMessage(), e);
        }
    }


    // hàm này sd trong trang thống kê
    public long countActiveProjects() {
        return projectRepository.countActiveProjects();
    }

    // hàm này sd trong trang thống kê
    public long countDistinctDistricts() {
        return projectRepository.countDistinctDistricts();
    }

    // hàm này sd trong trang thống kê
    public List<Map<String, Object>> countProjectByDistrict() {
        List<Object[]> rawResults = projectRepository.countProjectByDistrict();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] row : rawResults) {
            Map<String, Object> item = new HashMap<>();
            item.put("district", row[0]);
            item.put("count", ((Number) row[1]).longValue());
            result.add(item);
        }

        return result;
    }

    // hàm này sd trong trang thống kê
    public List<Map<String, Object>> countProjectByYear() {
        List<Object[]> results = projectRepository.countProjectByYear();
        List<Map<String, Object>> response = new ArrayList<>();

        for (Object[] row : results) {
            Integer year = (Integer) row[0];
            Long count = ((Number) row[1]).longValue();
            Map<String, Object> item = new HashMap<>();
            item.put("year", year);
            item.put("count", count);
            response.add(item);
        }

        return response;
    }




}
