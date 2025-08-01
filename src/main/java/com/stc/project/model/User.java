package com.stc.project.model;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "user")
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;
    @Column(name = "username", unique = true)
    String username;
    @Column(name = "hashed_password")
    String hashedPassword;
    @Column(name = "email", unique = true)
    String email;
    @Column(name = "otp")
    String otp;

    @Column(name = "otp_expiry_time")
    LocalDateTime otpExpiryTime;

    @Column(name = "otp_used")
    Boolean otpUsed = false;

    @Column(name = "otp_attempt_count")
    Integer otpAttemptCount = 0;

    @Column(name = "otp_locked_until")
    LocalDateTime otpLockedUntil;

    @Column(name = "is_verified", nullable = false, columnDefinition = "BIT(1) DEFAULT 0")
    Boolean isVerified = false;
    @Column(name = "is_active", nullable = false, columnDefinition = "BIT(1) DEFAULT 1")
    Boolean isActive = true;

    @ManyToMany
    Set<Role> roles;

}
