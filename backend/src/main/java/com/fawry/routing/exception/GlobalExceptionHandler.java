package com.fawry.routing.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException ex, HttpServletRequest request) {
        log.warn("Domain exception [{}]: {}", ex.getCode(), ex.getMessage());
        return build(ex.getCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldViolation)
                .toList();
        return build(ApiErrorCode.INVALID_REQUEST, "Validation failed", request, violations);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex,
                                                         HttpServletRequest request) {
        return build(ApiErrorCode.INVALID_CREDENTIALS, "Invalid username or password", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                       HttpServletRequest request) {
        return build(ApiErrorCode.ACCESS_DENIED, "Access denied", request, null);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ApiError> handleMalformedRequest(Exception ex, HttpServletRequest request) {
        log.warn("Malformed request on {}: {}", request.getRequestURI(), ex.getMessage());
        return build(ApiErrorCode.INVALID_REQUEST, "Malformed request", request, null);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiError> handleUnexpected(RuntimeException ex, HttpServletRequest request) {
        log.error("Unhandled runtime exception on {}", request.getRequestURI(), ex);
        return build(ApiErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request, null);
    }

    private ApiError.FieldViolation toFieldViolation(FieldError error) {
        return new ApiError.FieldViolation(error.getField(), error.getDefaultMessage());
    }

    private ResponseEntity<ApiError> build(ApiErrorCode code, String message,
                                           HttpServletRequest request,
                                           List<ApiError.FieldViolation> violations) {
        HttpStatus status = code.status();
        ApiError body = ApiError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .code(code.name())
                .message(message)
                .path(request.getRequestURI())
                .violations(violations)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
