package com.stc.project.controller;

import com.stc.project.dto.request.AuthenticationRequest;
import com.stc.project.dto.response.ApiResponse;
import com.stc.project.dto.response.AuthenticationResponse;
import com.stc.project.exception.AppException;
import com.stc.project.exception.ErrorCode;
import com.stc.project.service.serviceImpl.AuthenticationImpl;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class test {

    AuthenticationImpl authenticationImpl;

    @GetMapping("/{id}")
    public String test(@PathVariable Long id) {
        if (id == 1) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        return "test";
    }

    @PostMapping("/")
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

    @GetMapping("/aboutme")
    public String test() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
