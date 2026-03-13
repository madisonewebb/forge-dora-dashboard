package com.liatrio.dora.controller;

import com.liatrio.dora.service.InsightsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Set;

@Tag(name = "Insights", description = "AI-powered DORA insights via Server-Sent Events")
@RestController
@RequestMapping("/api/insights")
public class InsightsController {

    private static final Logger log = LoggerFactory.getLogger(InsightsController.class);
    private static final long SSE_TIMEOUT_MS = 60_000L;
    private static final Set<Integer> VALID_DAYS = Set.of(30, 90, 180);

    private final InsightsService insightsService;

    public InsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @Operation(summary = "Stream AI insights", description = "Streams AI-generated insights for the given repository as Server-Sent Events")
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getInsights(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "30") int days,
            HttpServletResponse response) {

        if (!VALID_DAYS.contains(days)) {
            throw new IllegalArgumentException("days must be 30, 90, or 180");
        }

        response.setHeader("Cache-Control", "no-store");
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        log.info("Streaming insights for {}/{} over {} days", owner, repo, days);

        // May throw InsightsUnavailableException synchronously → handled by GlobalExceptionHandler → 503
        Flux<String> tokenStream = insightsService.streamInsights(owner, repo, token, days);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        tokenStream.subscribe(
                chunk -> {
                    try {
                        emitter.send(chunk);
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                },
                emitter::completeWithError,
                emitter::complete
        );

        return emitter;
    }
}
