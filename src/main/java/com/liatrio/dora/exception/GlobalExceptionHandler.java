package com.liatrio.dora.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(GitHubRateLimitException.class)
    public ResponseEntity<Map<String, String>> handleRateLimit(GitHubRateLimitException ex) {
        log.warn("GitHub rate limit exceeded. Resets at: {}", ex.getResetsAt());
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of(
                        "error", "GitHub rate limit exceeded",
                        "resetsAt", DateTimeFormatter.ISO_INSTANT.format(ex.getResetsAt())
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        log.debug("Bad request: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<Map<String, String>> handleGitHubApiError(WebClientResponseException ex) {
        if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            log.warn("GitHub API returned 401 — token missing or invalid");
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "GitHub token is invalid or missing"));
        }
        log.warn("GitHub API error {}: {}", ex.getStatusCode(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "GitHub API error: " + ex.getStatusCode()));
    }

    @ExceptionHandler(InsightsUnavailableException.class)
    public ResponseEntity<Map<String, String>> handleInsightsUnavailable(InsightsUnavailableException ex) {
        log.warn("AI insights unavailable: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("error", "AI insights unavailable", "reason", ex.getMessage()));
    }
}
