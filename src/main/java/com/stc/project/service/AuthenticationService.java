package com.stc.project.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.SignedJWT;
import com.stc.project.dto.request.AuthenticationRequest;
import com.stc.project.dto.request.ForgotPasswordRequest;
import com.stc.project.dto.request.RegisterRequest;
import com.stc.project.dto.request.ResetPasswordRequest;
import com.stc.project.dto.response.AuthenticationResponse;

import java.text.ParseException;

public interface AuthenticationService {
    
    /**
     * Xác thực token
     */
    SignedJWT verifyToken(String token) throws JOSEException, ParseException;
    
    /**
     * Đăng nhập
     */
    AuthenticationResponse checkLogin(AuthenticationRequest request);
    
    /**
     * Đăng xuất
     */
    void logout(String token) throws ParseException, JOSEException;
    
    /**
     * Làm mới token
     */
    AuthenticationResponse refreshToken(String refreshTokenValue);
    
    /**
     * Đăng ký tài khoản
     */
    void register(RegisterRequest request);
    
    /**
     * Xác thực OTP
     */
    boolean verifyOtp(String username, String otp);
    
    /**
     * Gửi lại OTP
     */
    void refreshOtp(String username, boolean isReset);
    
    /**
     * Quên mật khẩu - gửi OTP
     */
    void forgotPassword(ForgotPasswordRequest request);
    
    /**
     * Đặt lại mật khẩu bằng OTP
     */
    void resetPassword(ResetPasswordRequest request);
}
