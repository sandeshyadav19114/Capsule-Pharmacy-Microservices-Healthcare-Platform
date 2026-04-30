package com.payment.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice @Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handle(RuntimeException ex) {
        HttpStatus s = ex.getMessage().contains("not found") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(s).body(Map.of("status", s.value(), "message", ex.getMessage(), "timestamp", LocalDateTime.now()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {
        log.error("Unexpected: {}", ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(Map.of("status", 500, "message", "Something went wrong", "timestamp", LocalDateTime.now()));
    }
}
