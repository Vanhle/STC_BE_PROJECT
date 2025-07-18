package com.stc.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class RefreshTokenRequest {
    @NotBlank(message = "TOKEN_REQUIRED")
    String refreshToken;
}
