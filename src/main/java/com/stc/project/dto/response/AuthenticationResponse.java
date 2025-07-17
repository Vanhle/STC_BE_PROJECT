package com.stc.project.dto.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.stc.project.model.User;
import lombok.*;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationResponse {
    String accessToken;
    String refreshToken;
    Long expiresIn;
    User user;
}
