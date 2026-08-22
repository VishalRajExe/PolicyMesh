package com.policymesh.common.exception;

import com.policymesh.common.response.ProblemDetailResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Translates all exceptions into RFC 7807 problem+json bodies.
 * Never leaks stack traces to the client.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PolicyMeshException.class)
    public ResponseEntity<ProblemDetailResponse> handlePolicyMesh(PolicyMeshException ex, HttpServletRequest req) {
        log.warn("Domain exception: {}", ex.getMessage());
        ProblemDetailResponse body = ProblemDetailResponse.of(
                ex.getType(), ex.getStatus().getReasonPhrase(), ex.getStatus().value(), ex.getMessage(), req.getRequestURI());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetailResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        ProblemDetailResponse body = ProblemDetailResponse.withErrors(
                "https://policymesh/errors/validation", "Validation failed", HttpStatus.BAD_REQUEST.value(),
                "One or more fields are invalid", req.getRequestURI(), errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetailResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
        ProblemDetailResponse body = ProblemDetailResponse.of(
                "https://policymesh/errors/authentication-failed", "Authentication failed",
                HttpStatus.UNAUTHORIZED.value(), "Invalid email or password", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetailResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        ProblemDetailResponse body = ProblemDetailResponse.of(
                "https://policymesh/errors/authorization-failed", "Access denied",
                HttpStatus.FORBIDDEN.value(), "You do not have permission to perform this action", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetailResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.error("Data integrity violation", ex);
        ProblemDetailResponse body = ProblemDetailResponse.of(
                "https://policymesh/errors/data-integrity", "Data integrity violation",
                HttpStatus.CONFLICT.value(), "The request conflicts with existing data", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetailResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception", ex);
        ProblemDetailResponse body = ProblemDetailResponse.of(
                "https://policymesh/errors/internal", "Internal server error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(), "An unexpected error occurred", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
