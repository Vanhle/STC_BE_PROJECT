package com.stc.project.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordRequest {
    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    String email;
    
    @NotBlank(message = "OTP không được để trống")
    @Size(min = 6, max = 6, message = "OTP phải có 6 ký tự")
    String otp;
    
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    String newPassword;
    
    @NotBlank(message = "Xác nhận mật khẩu không được để trống")
    String confirmPassword;
}
