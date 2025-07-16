package com.stc.project.service.serviceImpl;

import com.stc.project.repository.InvalidatedTokenRepository;
import com.stc.project.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {
    
    private final InvalidatedTokenRepository invalidatedTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000) // Every 6 hours
    @Transactional
    public void cleanupExpiredTokens() {
        Date now = new Date();
        
        try {
            // 1. Remove expired invalidated tokens
            invalidatedTokenRepository.deleteByExpiredTimeBefore(now);
            log.info("Cleaned up expired invalidated tokens");
            
            // 2. Remove expired refresh tokens
            refreshTokenRepository.deleteByExpiresAtBefore(now);
            log.info("Cleaned up expired refresh tokens");
            
            // 3. Remove revoked refresh tokens
            refreshTokenRepository.deleteByIsRevokedTrue();
            log.info("Cleaned up revoked refresh tokens");
            
        } catch (Exception e) {
            log.error("Error during token cleanup: ", e);
        }
    }
} 