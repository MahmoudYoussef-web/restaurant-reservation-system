package com.mahmoud.reservation.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(
            ApiException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                ex.getStatus().value(),
                ex.getMessage(),
                ex.getErrorCode(),
                request.getRequestURI()
        );

        return ResponseEntity.status(ex.getStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, Object> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        field -> field.getField(),
                        field -> field.getDefaultMessage(),
                        (v1, v2) -> v1
                ));

        ApiErrorResponse response = new ApiErrorResponse(
                400,
                "Validation failed",
                "VALIDATION_ERROR",
                request.getRequestURI(),
                errors
        );

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        String path = request.getRequestURI();

        if (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui")) {
            throw new RuntimeException(ex);
        }

        ApiErrorResponse response = new ApiErrorResponse(
                500,
                "Internal server error",
                "INTERNAL_ERROR",
                path
        );

        return ResponseEntity.internalServerError().body(response);
    }
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            org.springframework.security.authentication.BadCredentialsException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                401,
                "Invalid email or password",
                "UNAUTHORIZED",
                request.getRequestURI()
        );

        return ResponseEntity.status(401).body(response);
    }
    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthException(
            org.springframework.security.core.AuthenticationException ex,
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                401,
                "Authentication failed",
                "UNAUTHORIZED",
                request.getRequestURI()
        );

        return ResponseEntity.status(401).body(response);
    }
}