package com.stc.project.exception;

import com.stc.project.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException ex) {
        log.info("AppException: {}", ex.getErrorCode().name());
        return ResponseEntity.status(ex.getStatus()).body(
                ApiResponse.<Void>builder()
                        .timestamp(Instant.now())
                        .status(ex.getStatus().value())
                        .message(ex.getErrorCode().getErrorMessage()) // Trả về errorMessage thay vì errorCode.name()
                        .data(null)
                        .build()
        );
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> handleValidationErrors(MethodArgumentNotValidException ex) {
        log.info("MethodArgumentNotValidException: {}", ex.getBindingResult().getFieldErrors());
        List<Map<String, String>> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.badRequest().body(
                ApiResponse.<List<Map<String, String>>>builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message("VALIDATION_ERROR")
                        .data(errors)
                        .build()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleJsonParseError(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(
                ApiResponse.<String>builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message("INVALID_JSON")
                        .data(null)
                        .build()
        );
    }

//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<ApiResponse<List<String>>> handleConstraintViolation(ConstraintViolationException ex) {
//        List<String> errors = ex.getConstraintViolations().stream()
//                .map(ConstraintViolation::getMessage)
//                .collect(Collectors.toList());
//
//        return ResponseEntity.badRequest().body(
//                ApiResponse.<List<String>>builder()
//                        .timestamp(Instant.now())
//                        .status(HttpStatus.BAD_REQUEST.value())
//                        .message("CONSTRAINT_VIOLATION")
//                        .data(errors)
//                        .build()
//        );
//    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<String>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(
                ApiResponse.<String>builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                        .message("METHOD_NOT_ALLOWED")
                        .data(null)
                        .build()
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                ApiResponse.<String>builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.FORBIDDEN.value())
                        .message("FORBIDDEN")
                        .data(null)
                        .build()
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<String>> handleGenericException(RuntimeException ex) {
        ex.printStackTrace(); // hoặc log.error(...)
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                ApiResponse.<String>builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .message("INTERNAL_ERROR")
                        .data(null)
                        .build()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                ApiResponse.<String>builder()
                        .timestamp(Instant.now())
                        .status(HttpStatus.NOT_FOUND.value())
                        .message("NOT_FOUND")
                        .data(null)
                        .build()
        );
    }
}
