package com.clearing.netting.adapter.in.web;

import com.clearing.netting.adapter.in.web.dto.ErrorResponse;
import com.clearing.netting.domain.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Set<String> FORBIDDEN_CODES = Set.of(
            "FORBIDDEN", "ROLE_DENIED", "OPERATOR_REQUIRED");

    private static final Set<String> UNAUTHORIZED_CODES = Set.of(
            "UNAUTHORIZED", "AUTH_FAILED");

    private static final Set<String> NOT_FOUND_CODES = Set.of(
            "MEMBER_NOT_FOUND", "RUN_NOT_FOUND");

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException ex) {
        HttpStatus status = mapStatus(ex.getCode());
        return ResponseEntity.status(status).body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());
        return ResponseEntity.badRequest().body(ErrorResponse.of("VALIDATION_ERROR", "validation failed", details));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleOther(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", ex.getMessage() == null ? "unexpected error" : ex.getMessage()));
    }

    private HttpStatus mapStatus(String code) {
        if (FORBIDDEN_CODES.contains(code)) {
            return HttpStatus.FORBIDDEN;
        }
        if (UNAUTHORIZED_CODES.contains(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (NOT_FOUND_CODES.contains(code)) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.BAD_REQUEST;
    }

}
