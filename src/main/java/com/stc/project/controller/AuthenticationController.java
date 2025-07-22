package com.stc.project.controller;

import com.nimbusds.jose.JOSEException;
import com.stc.project.dto.request.*;
import com.stc.project.dto.response.ApiResponse;
import com.stc.project.dto.response.AuthenticationResponse;
import com.stc.project.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.time.Instant;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthenticationRequest rq) {
        AuthenticationResponse response = authenticationService.checkLogin(rq);
        ApiResponse<AuthenticationResponse> apiResponse = ApiResponse.<AuthenticationResponse>builder()
                .timestamp(Instant.now())
                .status(HttpStatus.OK.value())
                .message("Login successfully")
                .data(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authorizationHeader) throws ParseException, JOSEException {
        // Lấy token từ header dạng: Bearer <token>
        String token = authorizationHeader.replace("Bearer ", "");
        authenticationService.logout(token);
        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .timestamp(Instant.now())
                .status(HttpStatus.OK.value())
                .message("Logout successfully")
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody @Valid RefreshTokenRequest refreshToken) throws ParseException, JOSEException {
        // Lấy token từ header dạng: Bearer <token>
        AuthenticationResponse response = authenticationService.refreshToken(refreshToken.getRefreshToken());
        ApiResponse<AuthenticationResponse> apiResponse = ApiResponse.<AuthenticationResponse>builder()
                .timestamp(Instant.now())
                .status(HttpStatus.OK.value())
                .message("Refresh token successfully")
                .data(response)
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest rq) {
        authenticationService.register(rq);
        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .timestamp(Instant.now())
                .status(HttpStatus.OK.value())
                .message("Register successfully")
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/verifyotp")
    public ResponseEntity<?> verifyOtp(@RequestBody @Valid VerifyOtpRequest rq) {
        boolean check = authenticationService.verifyOtp(rq.getEmail(), rq.getOtp());
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .timestamp(Instant.now())
                .status(check ? HttpStatus.OK.value() : HttpStatus.BAD_REQUEST.value())
                .message(check ? "Verify otp successfully" : "False to verify OTP")
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/resendotp")
    public ResponseEntity<?> resendOtp(@RequestBody @Valid ResendOtpRequest rq) {
        authenticationService.refreshOtp(rq.getEmail(), rq.getIsReset());
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .timestamp(Instant.now())
                .status(HttpStatus.OK.value())
                .message("Resend otp successfully")
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/forgotpassword")
    public ResponseEntity<?> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        authenticationService.forgotPassword(request);
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .timestamp(Instant.now())
                .status(HttpStatus.OK.value())
                .message("Mã OTP đã được gửi đến email của bạn")
                .build();
        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/resetpassword")
    public ResponseEntity<?> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authenticationService.resetPassword(request);
        ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
                .timestamp(Instant.now())
                .status(HttpStatus.OK.value())
                .message("Đặt lại mật khẩu thành công")
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}
