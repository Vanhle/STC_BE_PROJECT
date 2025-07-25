package com.stc.project.controller;


import com.stc.project.dto.request.UserCreateRequest;
import com.stc.project.dto.request.UserUpdateRequest;
import com.stc.project.model.User;
import com.stc.project.service.UserService;
import com.stc.project.utils.PaginationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('CREATE_USER')")
    public ResponseEntity<User> createUser(@RequestBody @Valid UserCreateRequest request) {
        User newUser = userService.createUser(request);
        return new ResponseEntity<>(newUser, HttpStatus.CREATED);
    }

    // UPDATE User (only isActive)
    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority('UPDATE_USER')")
    public ResponseEntity<User> updateUserStatus(@PathVariable Long id, @RequestBody @Valid UserUpdateRequest request) {
        User updatedUser = userService.updateUserStatus(id, request);
        return ResponseEntity.ok(updatedUser);
    }

    // DELETE User
    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority('DELETE_USER')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // search user
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SEARCH_USER')")
    public ResponseEntity<List<User>> searchUsers(
            @RequestParam("query") String query,
            @PageableDefault(size = Integer.MAX_VALUE) Pageable pageable) {
        Page<User> page = userService.searchUsers(query, pageable);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, "/api/users/search");
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }

    // READ Users with pagination, sort, and search
    @GetMapping
    @PreAuthorize("hasAuthority('READ_USER')")
    public ResponseEntity<List<User>> getAllUsers(
            @RequestParam(value = "query", required = false) String query,
            Pageable pageable) {
        Page<User> page;
        if (query != null && !query.isEmpty()) {
            page = userService.searchUsers(query, pageable);
        } else {
            page = userService.searchUsers( null, pageable);
        }
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(page, query);
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }


    // Override get method from CrudController if needed for specific authorization
    @GetMapping(value = "read/{id}")
    @PreAuthorize("hasAuthority('READ_USER')")
    public User get(@PathVariable(value = "id") Long id) {
        return userService.get(id);
    }

    // Additional methods can be added here for more specific user operations
    // For example, methods for user verification, password reset, etc.
    // Ensure that all methods are properly secured with @PreAuthorize annotations
    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Test endpoint accessed successfully!");
    }

}