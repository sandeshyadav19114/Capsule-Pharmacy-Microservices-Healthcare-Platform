package com.notification.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice @Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
            "status", 404, "message", ex.getMessage(), "timestamp", LocalDateTime.now()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {
        log.error("Unexpected: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(Map.of(
            "status", 500, "message", "Something went wrong", "timestamp", LocalDateTime.now()));
    }
}
