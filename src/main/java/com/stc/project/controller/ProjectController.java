package com.stc.project.controller;

import com.stc.project.core.CrudController;
import com.stc.project.model.Project;
import com.stc.project.repository.ProjectRepository;
import com.stc.project.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/projects")
public class ProjectController extends CrudController<Project, Long> {
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    private ProjectService projectService;
    private ProjectRepository projectRepository;

    @Autowired
    public ProjectController(ProjectService service, ProjectRepository repository) {
        super(service);
        this.projectService = service;
        this.projectRepository = repository;
        this.baseUrl = "/api/projects";
    }

    //  endpoint này được gọi trong trang thống kê
    @GetMapping("/project-count")
    public long countActiveProjects() {
        return projectService.countActiveProjects();
    }


    //  endpoint này được gọi trong trang thống kê
    @GetMapping("/district-count")
    public long countDistinctDistricts() {
        return projectService.countDistinctDistricts();
    }


    //  endpoint này được gọi trong trang thống kê
    @GetMapping("/statistics/district")
    public ResponseEntity<List<Map<String, Object>>> getProjectCountByDistrict() {
        List<Map<String, Object>> data = projectService.countProjectByDistrict();
        return ResponseEntity.ok(data);
    }


    //  endpoint này được gọi trong trang thống kê
    @GetMapping("/statistics/year")
    public List<Map<String, Object>> countProjectByYear() {
        return projectService.countProjectByYear();
    }
}
