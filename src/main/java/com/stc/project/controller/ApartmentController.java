package com.stc.project.controller;

import com.stc.project.core.CrudController;
import com.stc.project.model.Apartment;
import com.stc.project.repository.ApartmentRepository;
import com.stc.project.service.ApartmentService;
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
@RequestMapping("/api/apartments")
public class ApartmentController extends CrudController<Apartment, Long> {
    private static final Logger logger = LoggerFactory.getLogger(ApartmentController.class);

    private ApartmentService apartmentService;
    private ApartmentRepository apartmentRepository;

    @Autowired
    public ApartmentController(ApartmentService service, ApartmentRepository repository) {
        super(service);
        this.apartmentService = service;
        this.apartmentRepository = repository;
        this.baseUrl = "/api/apartments";
    }

    //get data created by user đang login
    @GetMapping("/detail")
    public ResponseEntity<?> getData() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return ResponseEntity.ok(apartmentRepository.findByCreatedBy(username));
    }
}
