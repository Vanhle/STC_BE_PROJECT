package com.stc.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)

public class VerifyOtpRequest {
    @NotBlank(message = "email required")
    String email;
    @NotBlank(message = "OTP required")
    String otp;
}
