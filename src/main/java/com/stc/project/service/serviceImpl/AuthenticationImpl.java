package com.stc.project.service.serviceImpl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.stc.project.dto.request.AuthenticationRequest;
import com.stc.project.dto.request.LogoutRequest;
import com.stc.project.dto.response.AuthenticationResponse;
import com.stc.project.exception.AppException;
import com.stc.project.exception.ErrorCode;
import com.stc.project.model.InvalidatedToken;
import com.stc.project.model.RefreshToken;
import com.stc.project.model.User;
import com.stc.project.repository.InvalidatedTokenRepository;
import com.stc.project.repository.RefreshTokenRepository;
import com.stc.project.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationImpl {
    @NonFinal
    @Value("${jwt.signed_key}")
    protected String SIGNED_KEY;

    InvalidatedTokenRepository invalidatedTokenRepository;
    UserRepository userRepository;
    RefreshTokenRepository refreshTokenRepository;


    /**
     * Author: @Vanhle
     * Hàm này để veirify token mỗi request
    **/
    public SignedJWT verifyToken(String token) throws JOSEException, ParseException {
        JWSVerifier verifier = new MACVerifier(SIGNED_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);
        Date exp = signedJWT.getJWTClaimsSet().getExpirationTime();
        var verified = signedJWT.verify(verifier);

        // 1. Check signature và format
        if (!verified) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }

        // 2. Check invalidated trước - QUAN TRỌNG!
        String jwtId = signedJWT.getJWTClaimsSet().getJWTID();
        boolean isInvalidated = invalidatedTokenRepository.existsById(jwtId);
        if (isInvalidated) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        // 3. Check expiration sau
        if (exp.before(new Date())) {
            throw new AppException(ErrorCode.TOKEN_EXPIRED);
        }
        return signedJWT;
    }

    public AuthenticationResponse checkLogin(AuthenticationRequest request) {
        // Tìm user bằng username hoặc email
        User user = findUserByUsernameOrEmail(request.getUsername());

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        boolean check = passwordEncoder.matches(request.getPassword(), user.getHashedPassword());
        if (!check) {
            throw new AppException(ErrorCode.PASSWORD_OR_EMAIL_USERNAME_INCORRECT);
        }

        // Kiểm tra trạng thái user
        if (!user.getIsActive()) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }

        // Generate access token (15 minutes)
        String accessToken = generateAccessToken(user);

        // Generate refresh token (7 days)
        String refreshToken = generateRefreshToken(user);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(15 * 60L)// 15 minutes in seconds
                .build();

    }

    public void logout(String token) throws ParseException, JOSEException {
        SignedJWT signedToken = verifyToken(token);
        String jid = signedToken.getJWTClaimsSet().getJWTID();
        Date expiredTime = signedToken.getJWTClaimsSet().getExpirationTime();
        InvalidatedToken invalidatedToken = InvalidatedToken.builder()
                .id(jid)
                .expiredTime(expiredTime)
                .build();
        invalidatedTokenRepository.save(invalidatedToken);

        // Revoke all refresh tokens for this user
        String username = signedToken.getJWTClaimsSet().getSubject();
        User user = userRepository.findByUsername(username).get();
        // Phải revoke refresh token trong database
        refreshTokenRepository.revokeAllByUserId(user.getId());
    }

    public AuthenticationResponse refreshToken(String refreshTokenValue) {
        // 1. Validate refresh token -> Nếu token bị revoke rồi thì throw exception
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndIsRevokedFalse(refreshTokenValue)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));

        // 2. Check expiration -> expired phải sau ngay hien tai
        if (refreshToken.getExpiresAt().before(new Date())) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 3. Get user
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REFRESH_TOKEN));

        // 4. Generate new access token
        String newAccessToken = generateAccessToken(user);

        // 5. Rotate refresh token
        String newRefreshToken = rotateRefreshToken(refreshToken, user);

        return AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(15 * 60L) // 15 minutes
                .build();
    }

    private String rotateRefreshToken(RefreshToken oldToken, User user) {
        // Revoke old token
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);

        // Generate new refresh token
        return generateRefreshToken(user);
    }

    /**
     * Tìm user bằng username hoặc email
     * @param usernameOrEmail có thể là username hoặc email
     * @return Users entity
     * @throws AppException nếu không tìm thấy user
     */
    private User findUserByUsernameOrEmail(String usernameOrEmail) {
        // Thử tìm bằng username trước
        var user = userRepository.findByUsername(usernameOrEmail);
        if (user.isPresent()) {
            return user.get();
        }

        // Nếu không tìm thấy, thử tìm bằng email
        user = userRepository.findByEmail(usernameOrEmail);
        if (user.isPresent()) {
            return user.get();
        }

        // Nếu không tìm thấy cả hai
        throw new AppException(ErrorCode.PASSWORD_OR_EMAIL_USERNAME_INCORRECT);
    }

    private String generateAccessToken(User users) {
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(users.getUsername())
                .issuer("stc.project.com")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 15 * 60 * 1000)) // 15 minutes
                .claim("scope", buildScope(users))
                .jwtID(UUID.randomUUID().toString())
                .build();
        Payload payload = new Payload(claims.toJSONObject());

        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(SIGNED_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }

    }

    private String generateRefreshToken(User user) {
        // Generate secure random string
        String refreshTokenValue = UUID.randomUUID().toString() + "_" + System.currentTimeMillis();

        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID().toString())
                .userId(user.getId())
                .token(refreshTokenValue)
                .expiresAt(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L)) // 7 days
                .createdAt(new Date())
                .isRevoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
        return refreshTokenValue;
    }

    private String buildScope(User users) {
        StringJoiner stringJoiner = new StringJoiner(" ");
        if (!CollectionUtils.isEmpty(users.getRoles())) {
            users.getRoles().forEach(role -> {
                stringJoiner.add("ROLE_" + role.getName());
                if (!CollectionUtils.isEmpty(role.getPermissions())) {
                    role.getPermissions().forEach(permission -> {
                        stringJoiner.add(permission.getName());
                    });
                }
            });
        }
        return stringJoiner.toString();
    }


}
