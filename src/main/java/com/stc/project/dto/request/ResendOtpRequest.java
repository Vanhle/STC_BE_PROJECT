package com.stc.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResendOtpRequest {
    @NotBlank(message = "Email required")
    String email;
    @NotNull(message = "Reset type required")
    Boolean isReset;
}
