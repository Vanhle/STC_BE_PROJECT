package com.stc.project.configuration;

import com.stc.project.model.Permission;
import com.stc.project.model.Role;
import com.stc.project.model.User;
import com.stc.project.repository.PermissionRepository;
import com.stc.project.repository.RoleRepository;
import com.stc.project.repository.UserRepository;
import com.stc.project.utils.PermissionEnum;
import com.stc.project.utils.RoleEnum;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)

public class InitialConfig {

    PasswordEncoder passwordEncoder;
    UserRepository userRepository;
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;

    @Bean
    ApplicationRunner applicationRunner() {
        return args -> {
            initPermission();
            initRole();
            initUser();
        };
    }

    void initPermission() {
        Arrays.stream(PermissionEnum.values())
                .filter(permission -> !permissionRepository.existsByName(permission.name()))
                .map(permission -> Permission.builder()
                        .name(permission.name())
                        .description(permission.getDescription())
                        .build())
                .forEach(permissionRepository::save);
    }

    void initRole() {
        Arrays.stream(RoleEnum.values())
                .filter(role -> !roleRepository.existsByName(role.name()))
                .map(role -> Role.builder()
                        .name(role.name())
                        .description(role.getDescription())
                        .permissions(role.name().equals("ADMIN") ? new HashSet<>(permissionRepository.findAll()): new HashSet<>())
                        .build())
                .forEach(roleRepository::save);
    }

    void initUser() {
        if(userRepository.findByUsername("admin").isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findByName("ADMIN"));
            User user = User.builder()
                    .username("admin")
                    .email("admin@system.com")
                    .username("admin")
                    .hashedPassword(passwordEncoder.encode("admin"))
                    .isVerified(true)  // Admin user should be verified
                    .isActive(true)
                    .roles(roles)
                    .build();
            userRepository.save(user);
        }
        if(userRepository.findByUsername("manager").isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findByName("MANAGER"));
            User user = User.builder()
                    .username("manager")
                    .email("manager@system.com")
                    .username("manager")
                    .hashedPassword(passwordEncoder.encode("manager"))
                    .isVerified(true)  // Admin user should be verified
                    .isActive(true)
                    .roles(roles)
                    .build();
            userRepository.save(user);
        }
    }
}
