package com.finflow.authservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<java.util.Map<String, String>> handleInvalidRefreshToken(
            InvalidRefreshTokenException exception) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(java.util.Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<java.util.Map<String, String>> handleValidationException(
            org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String errorMsg = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(java.util.Map.of("message", errorMsg));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<java.util.Map<String, String>> handleRuntimeException(
            RuntimeException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "An error occurred";
        if (msg.contains("Invalid email or password")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(java.util.Map.of("message", msg));
        }
        if (msg.contains("Email already exists")) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(java.util.Map.of("message", msg));
        }
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(java.util.Map.of("message", msg));
    }
}