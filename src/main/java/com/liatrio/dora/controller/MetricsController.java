package com.liatrio.dora.controller;

import com.liatrio.dora.dto.MetricsResponse;
import com.liatrio.dora.service.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private static final Logger log = LoggerFactory.getLogger(MetricsController.class);
    private static final Set<Integer> VALID_DAYS = Set.of(30, 90, 180);

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    public ResponseEntity<MetricsResponse> getMetrics(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam String token,
            @RequestParam(defaultValue = "30") int days) {

        if (!VALID_DAYS.contains(days)) {
            throw new IllegalArgumentException("days must be 30, 90, or 180");
        }

        log.info("Metrics requested for {}/{} over {} days", owner, repo, days);
        MetricsResponse response = metricsService.getMetrics(owner, repo, token, days);
        return ResponseEntity.ok(response);
    }
}
