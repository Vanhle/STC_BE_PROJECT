package com.stc.project.repository;


import com.stc.project.core.CustomJpaRepository;
import com.stc.project.model.Project;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectRepository extends CustomJpaRepository<Project, Long> {

    @Query(value = "SELECT COUNT(*) FROM project WHERE project.active = 1", nativeQuery = true)
    long countActiveProjects();


    @Query(value = "SELECT COUNT(DISTINCT TRIM(REGEXP_SUBSTR(address, 'Q\\\\.\\s*[^,]+'))) " +
            "FROM project " +
            "WHERE address IS NOT NULL",
            nativeQuery = true)
    Long countDistinctDistricts();

    // Đếm project theo quận bằng cách tách chuỗi từ address
    @Query(value = "SELECT " +
            "TRIM(REGEXP_SUBSTR(address, 'Q\\\\.\\s*[^,]+')) AS district, " +
            "COUNT(*) AS project_count " +
            "FROM project " +
            "WHERE address IS NOT NULL " +
            "GROUP BY district " +
            "ORDER BY project_count DESC",
            nativeQuery = true)
    List<Object[]> countProjectByDistrict();


    // Đếm project theo năm bằng cách tách chuỗi từ expected_completion_date
    @Query(value = "SELECT " +
            "YEAR(expected_completion_date) AS year, " +
            "COUNT(*) AS count " +
            "FROM project " +
            "GROUP BY YEAR(expected_completion_date) " +
            "ORDER BY year",
            nativeQuery = true)
    List<Object[]> countProjectByYear();
}
