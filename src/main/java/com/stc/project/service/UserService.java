package com.stc.project.service;


import com.stc.project.core.CrudService;
import com.stc.project.dto.request.UserCreateRequest;
import com.stc.project.dto.request.UserUpdateRequest;
import com.stc.project.exception.AppException;
import com.stc.project.exception.ErrorCode;
import com.stc.project.model.User;
import com.stc.project.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;

import static com.stc.project.utils.SecurityUtil.getCurrentUserLogin;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // CREATE User
    public User createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_EXISTED); // Or a more specific email existed error
        }

        User newUser = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .hashedPassword(passwordEncoder.encode(request.getHashedPassword()))
                .isActive(true) // Set isActive to true
                .isVerified(true) // Set isVerified to true
                .roles(new HashSet<>()) // Assign roles as needed, e.g., default USER role
                .build();

        // Call the super.create method to handle createdBy and active status
//        return super.create(newUser);
        // Assuming getCurrentUserLogin() is available
        return userRepository.save(newUser);
    }

    // UPDATE User (only isActive)
    public User updateUserStatus(Long id, UserUpdateRequest request) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User with ID = " + id + " not found."));

        existingUser.setIsActive(request.getIsActive());

        return userRepository.save(existingUser);
    }

    // DELETE User
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User with ID = " + id + " not found."));
        userRepository.delete(user);
    }

    // READ Users with search by email or isActive

    //    public Page<User> searchUsers(String query, Pageable pageable) {
//        return searchUser(query, pageable);
//    }
    public Page<User> searchUsers (String query, Pageable pageable) {
        if (query == null || query.isEmpty()) {
            return userRepository.findAll(pageable);
        }
        // tách query thành các phần và tìm kiếm theo email hoặc isActive
        String[] parts = query.split(";");
        String email = null;
        Boolean isActive = null;

        for (String part : parts) {
            if (part.startsWith("email=")) {
                email = part.substring(6);
            } else if (part.startsWith("isActive==")) {
                isActive = Boolean.parseBoolean(part.substring(9));
            }
        }
        if (email != null && isActive != null) {
            return userRepository.findByEmailContainingIgnoreCase(email, isActive, pageable);
        } else if (email != null) {
            return userRepository.findByEmail(email, pageable);
        } else if (isActive != null) {
            return userRepository.findByIsActive(isActive, pageable);
        } else {
            return userRepository.findAll(pageable);
        }
    }

    public User get(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User with ID = " + id + " not found."));
        return user;
    }
}
