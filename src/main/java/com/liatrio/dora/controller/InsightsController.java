package com.liatrio.dora.controller;

import com.liatrio.dora.service.InsightsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/api/insights")
public class InsightsController {

    private static final Logger log = LoggerFactory.getLogger(InsightsController.class);
    private static final long SSE_TIMEOUT_MS = 60_000L;

    private final InsightsService insightsService;

    public InsightsController(InsightsService insightsService) {
        this.insightsService = insightsService;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getInsights(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam String token,
            @RequestParam(defaultValue = "30") int days) {

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
