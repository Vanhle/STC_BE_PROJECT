package com.stc.project.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Date;

@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RefreshToken {
    @Id
    String id;
    
    @Column(name = "user_id")
    Long userId;
    
    @Column(name = "token", length = 500)
    String token;
    
    @Column(name = "expires_at")
    Date expiresAt;
    
    @Column(name = "created_at")
    Date createdAt;
    
    @Column(name = "is_revoked")
    boolean isRevoked = false;
} 