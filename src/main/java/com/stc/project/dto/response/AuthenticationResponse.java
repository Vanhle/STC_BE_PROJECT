package com.stc.project.dto.response;


import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class AuthenticationResponse {
    String accessToken;
    String refreshToken;
    Long expiresIn;
}
