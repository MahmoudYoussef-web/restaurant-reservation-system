package com.mahmoud.reservation.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleBadCredentials(
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                401,
                "Invalid email or password",
                "INVALID_CREDENTIALS",
                request.getRequestURI()
        );

        return ResponseEntity.status(401).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                403,
                "Access denied",
                "ACCESS_DENIED",
                request.getRequestURI()
        );

        return ResponseEntity.status(403).body(response);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthException(
            HttpServletRequest request
    ) {
        ApiErrorResponse response = new ApiErrorResponse(
                401,
                "Authentication failed",
                "AUTHENTICATION_FAILED",
                request.getRequestURI()
        );

        return ResponseEntity.status(401).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request
    ) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);

        ApiErrorResponse response = new ApiErrorResponse(
                500,
                "Internal server error",
                "INTERNAL_ERROR",
                request.getRequestURI()
        );

        return ResponseEntity.internalServerError().body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request
    ) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                400, ex.getMessage() != null ? ex.getMessage() : "Invalid request",
                "BAD_REQUEST", request.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        log.warn("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(409).body(new ApiErrorResponse(
                409, "Resource already exists or is in use",
                "DATA_CONFLICT", request.getRequestURI()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                400, "Validation failed: " + ex.getMessage(),
                "VALIDATION_ERROR", request.getRequestURI()));
    }

    @ExceptionHandler({MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class})
    public ResponseEntity<ApiErrorResponse> handleBadInput(
            Exception ex,
            HttpServletRequest request
    ) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(
                400, "Invalid request parameters or body",
                "BAD_REQUEST", request.getRequestURI()));
    }
}