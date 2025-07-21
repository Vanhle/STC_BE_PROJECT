package com.stc.project.service.serviceImpl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.stc.project.dto.request.AuthenticationRequest;
import com.stc.project.dto.request.ForgotPasswordRequest;
import com.stc.project.dto.request.LogoutRequest;
import com.stc.project.dto.request.RegisterRequest;
import com.stc.project.dto.request.ResetPasswordRequest;
import com.stc.project.dto.response.AuthenticationResponse;
import com.stc.project.exception.AppException;
import com.stc.project.exception.ErrorCode;
import com.stc.project.model.InvalidatedToken;
import com.stc.project.model.RefreshToken;
import com.stc.project.model.Role;
import com.stc.project.model.User;
import com.stc.project.repository.InvalidatedTokenRepository;
import com.stc.project.repository.RefreshTokenRepository;
import com.stc.project.repository.RoleRepository;
import com.stc.project.repository.UserRepository;
import com.stc.project.service.AuthenticationService;
import com.stc.project.service.EmailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationImpl implements AuthenticationService {
    @NonFinal
    @Value("${jwt.signed_key}")
    protected String SIGNED_KEY;

    InvalidatedTokenRepository invalidatedTokenRepository;
    UserRepository userRepository;
    RefreshTokenRepository refreshTokenRepository;
    RoleRepository roleRepository;
    EmailService emailService;


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

        // Kiểm tra user đã verify chưa
        if (!user.getIsVerified()) {
            if(user.getOtpExpiryTime() == null || user.getOtpExpiryTime().isBefore(LocalDateTime.now())){
                String otp = generateOtp();
                emailService.sendSimpleEmail(user.getEmail(), "OTP", otp);
                user.setOtp(otp);
                user.setOtpAttemptCount(0);
                user.setOtpLockedUntil(null);
                user.setOtpUsed(false);
                user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
                userRepository.save(user);
                throw new AppException(ErrorCode.NEED_TO_VERIFY);
            } else {
                throw new AppException(ErrorCode.NEED_TO_VERIFY);
            }
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

    public void register(RegisterRequest rq) {
        // Kiểm tra password và confirm password
        if (!rq.getPassword().equals(rq.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_AND_CONFIRM_PASSWORD_NOT_MATCH); // Có thể tạo ErrorCode riêng cho lỗi này nếu muốn
        }
        // Kiểm tra email đã tồn tại
        Optional<User> userByEmail = userRepository.findByEmail(rq.getEmail());
        if (userByEmail.isPresent()) {
            throw new AppException(ErrorCode.EMAIL_EXISTED); // Có thể tạo ErrorCode riêng cho lỗi này nếu muốn
        }
        // Kiểm tra username đã tồn tại
        Optional<User> userByUsername = userRepository.findByUsername(rq.getUsername());
        if (userByUsername.isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED); // Có thể tạo ErrorCode riêng cho lỗi này nếu muốn
        }

        String otp = generateOtp();
        emailService.sendSimpleEmail(rq.getEmail(), "OTP", otp);
        // Mã hoá password
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String hashedPassword = passwordEncoder.encode(rq.getPassword());
        // Tạo user mới
        User user = User.builder()
                .email(rq.getEmail())
                .username(rq.getUsername())
                .hashedPassword(hashedPassword)
                .isActive(true)
                .isVerified(false)
                .otp(otp)
                .otpUsed(false)
                .otpAttemptCount(0)
                .otpExpiryTime(LocalDateTime.now().plusMinutes(5))
                .otpLockedUntil(null)
                .build();
        // Gán role MANAGER cho user mới
        Set<Role> managerRole = roleRepository.findByName("MANAGER");
        user.setRoles(managerRole);
        userRepository.save(user);
    }


    public boolean verifyOtp(String username, String otp) {
        User user = findUserByUsernameOrEmail(username);
        if (user.getIsVerified()) {
            throw new AppException(ErrorCode.USER_ALREADY_VERIFIED);
        }

        // Kiểm tra OTP bị khóa
        if (user.getOtpLockedUntil() != null && user.getOtpLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.OTP_LOCKED);
        }

        // Kiểm tra OTP đúng
        if (user.getOtp() != null && user.getOtp().equals(otp)
                && user.getOtpExpiryTime() != null && user.getOtpExpiryTime().isAfter(LocalDateTime.now())
                && !Boolean.TRUE.equals(user.getOtpUsed())
                && user.getOtpAttemptCount() < 3) {
            user.setOtpUsed(true);
            user.setOtpAttemptCount(0);
            user.setOtpExpiryTime(null);
            user.setOtp(null);
            user.setIsVerified(true);
            userRepository.save(user);
            return true;
        }

        // OTP sai
        user.setOtpAttemptCount(user.getOtpAttemptCount() + 1);
        if (user.getOtpAttemptCount() >= 3) {
            user.setOtpLockedUntil(LocalDateTime.now().plusMinutes(5));
        }
        userRepository.save(user);
        return false;
    }

    public void refreshOtp(String username) {
        User user = findUserByUsernameOrEmail(username);
        if (user.getOtpLockedUntil() != null && user.getOtpLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.OTP_LOCKED);
        }
        if (user.getIsVerified()) {
            throw new AppException(ErrorCode.USER_ALREADY_VERIFIED);
        }
        String otp = generateOtp();
        emailService.sendSimpleEmail(user.getEmail(), "OTP", otp);
        user.setOtp(otp);
        user.setOtpAttemptCount(0);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5));
        user.setOtpLockedUntil(null);
        user.setOtpUsed(false);
        userRepository.save(user);
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
     *
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
        throw new AppException(ErrorCode.EMAIL_OR_USERNAME_INCORRECT);
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

    private String generateOtp() {
        //Generate 6 ditgit OTP
        return String.format("%06d", new Random().nextInt(999999));
    }

    /**
     * Gửi OTP để reset mật khẩu
     *
     * @param request chứa email của user
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        // Tìm user bằng email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_OR_USERNAME_INCORRECT));

        // Kiểm tra tài khoản có bị khóa không
        if (!user.getIsActive()) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }

        // Kiểm tra OTP có bị khóa không
        if (user.getOtpLockedUntil() != null && user.getOtpLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.OTP_LOCKED);
        }

        // Tạo OTP mới
        String otp = generateOtp();

        // Gửi email OTP
        String subject = "Mã OTP đặt lại mật khẩu";
        String content = String.format(
                "Xin chào %s,\n\n" +
                        "Bạn đã yêu cầu đặt lại mật khẩu. Mã OTP của bạn là: %s\n\n" +
                        "Mã OTP này sẽ hết hạn sau 5 phút.\n\n" +
                        "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
                        "Trân trọng,\nĐội ngũ STC Project",
                user.getUsername(), otp
        );

        emailService.sendSimpleEmail(user.getEmail(), subject, content);

        // Cập nhật thông tin OTP trong database
        user.setOtp(otp);
        user.setOtpUsed(false);
        user.setOtpAttemptCount(0);
        user.setOtpExpiryTime(LocalDateTime.now().plusMinutes(5)); // OTP hết hạn sau 5 phút
        user.setOtpLockedUntil(null);

        userRepository.save(user);

        log.info("Đã gửi OTP reset password cho email: {}", request.getEmail());
    }

    /**
     * Đặt lại mật khẩu bằng OTP
     *
     * @param request chứa email, OTP và mật khẩu mới
     */
    public void resetPassword(ResetPasswordRequest request) {
        // Kiểm tra mật khẩu và xác nhận mật khẩu
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_AND_CONFIRM_PASSWORD_NOT_MATCH);
        }

        // Tìm user bằng email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_OR_USERNAME_INCORRECT));

        // Kiểm tra tài khoản có bị khóa không
        if (!user.getIsActive()) {
            throw new AppException(ErrorCode.ACCOUNT_LOCKED);
        }

        // Kiểm tra OTP có bị khóa không
        if (user.getOtpLockedUntil() != null && user.getOtpLockedUntil().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.OTP_LOCKED);
        }

        // Kiểm tra OTP
        if (user.getOtp() == null || !user.getOtp().equals(request.getOtp())) {
            // OTP sai, tăng số lần thử
            user.setOtpAttemptCount(user.getOtpAttemptCount() + 1);
            if (user.getOtpAttemptCount() >= 3) {
                user.setOtpLockedUntil(LocalDateTime.now().plusMinutes(5));
                userRepository.save(user);
                throw new AppException(ErrorCode.OTP_LOCKED);
            }
            userRepository.save(user);
            throw new AppException(ErrorCode.INVALID_OTP);
        }

        // Kiểm tra OTP đã hết hạn chưa
        if (user.getOtpExpiryTime() == null || user.getOtpExpiryTime().isBefore(LocalDateTime.now())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        // Kiểm tra OTP đã được sử dụng chưa
        if (Boolean.TRUE.equals(user.getOtpUsed())) {
            throw new AppException(ErrorCode.OTP_ALREADY_USED);
        }

        // Kiểm tra số lần thử OTP
        if (user.getOtpAttemptCount() >= 3) {
            user.setOtpLockedUntil(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);
            throw new AppException(ErrorCode.OTP_LOCKED);
        }

        // OTP hợp lệ, đặt lại mật khẩu
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String hashedPassword = passwordEncoder.encode(request.getNewPassword());

        user.setHashedPassword(hashedPassword);
        user.setOtp(null);
        user.setOtpUsed(true);
        user.setOtpAttemptCount(0);
        user.setOtpExpiryTime(null);
        user.setOtpLockedUntil(null);

        userRepository.save(user);

        // Revoke tất cả refresh token của user để buộc đăng nhập lại
        refreshTokenRepository.revokeAllByUserId(user.getId());

        log.info("Đã đặt lại mật khẩu thành công cho email: {}", request.getEmail());
    }

}
