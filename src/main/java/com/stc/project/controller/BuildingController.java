package com.stc.project.controller;

import com.stc.project.core.CrudController;
import com.stc.project.model.Building;
import com.stc.project.repository.BuildingRepository;
import com.stc.project.service.BuildingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/buildings")
public class BuildingController extends CrudController<Building, Long> {
    private static Logger logger = LoggerFactory.getLogger(BuildingController.class);

    private BuildingService buildingService;
    private BuildingRepository buildingRepository;
    @Autowired
    public BuildingController(BuildingService service, BuildingRepository repository) {
        super(service);
        this.buildingService = service;
        this.buildingRepository = repository;
        this.baseUrl = "/api/buildings";
    }

    //get data created by user đang login
    @GetMapping("/detail")
    public ResponseEntity<?> getData() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return ResponseEntity.ok(buildingRepository.findByCreatedBy(username));
    }
}



