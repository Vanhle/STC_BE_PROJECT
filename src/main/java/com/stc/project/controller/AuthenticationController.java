package com.stc.project.controller;


import com.nimbusds.jose.JOSEException;
import com.stc.project.dto.request.AuthenticationRequest;
import com.stc.project.dto.request.LogoutRequest;
import com.stc.project.dto.response.ApiResponse;
import com.stc.project.dto.response.AuthenticationResponse;
import com.stc.project.service.serviceImpl.AuthenticationImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.ParseException;
import java.time.Instant;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationImpl authenticationImpl;
    @PostMapping("/login")
    public ResponseEntity login(@RequestBody @Valid AuthenticationRequest rq) {
        AuthenticationResponse response = authenticationImpl.checkLogin(rq);
        ApiResponse<AuthenticationResponse> apiResponse = ApiResponse.<AuthenticationResponse>builder()
                .timestamp(Instant.now())
                .status(HttpStatus.OK.value())
                .message("Login successfully")
                .data(response)
                .build();

        return ResponseEntity.ok(apiResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity logout(@RequestBody @Valid LogoutRequest rq) throws ParseException, JOSEException {
        authenticationImpl.logout(rq);
        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .timestamp(Instant.now())
                .status(HttpStatus.OK.value())
                .message("Logout successfully")
                .build();
        return ResponseEntity.ok(apiResponse);
    }
}
