package com.pharmacy.exception;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private Map<String, String> fieldErrors; // populated on @Valid failures
    private LocalDateTime timestamp;
}
