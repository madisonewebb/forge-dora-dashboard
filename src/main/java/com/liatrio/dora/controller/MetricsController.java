package com.liatrio.dora.controller;

import com.liatrio.dora.dto.MetricHistoryResponse;
import com.liatrio.dora.dto.MetricsResponse;
import com.liatrio.dora.model.MetricSnapshot;
import com.liatrio.dora.service.MetricSnapshotService;
import com.liatrio.dora.service.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private static final Logger log = LoggerFactory.getLogger(MetricsController.class);
    private static final Set<Integer> VALID_DAYS = Set.of(30, 90, 180);

    private final MetricsService metricsService;
    private final MetricSnapshotService snapshotService;

    public MetricsController(MetricsService metricsService, MetricSnapshotService snapshotService) {
        this.metricsService = metricsService;
        this.snapshotService = snapshotService;
    }

    @GetMapping
    public ResponseEntity<MetricsResponse> getMetrics(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "30") int days) {

        if (!VALID_DAYS.contains(days)) {
            throw new IllegalArgumentException("days must be 30, 90, or 180");
        }

        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7) : authHeader;
        log.info("Metrics requested for {}/{} over {} days", owner, repo, days);
        MetricsResponse response = metricsService.getMetrics(owner, repo, token, days);

        try {
            snapshotService.saveSnapshot(owner, repo, days, response);
        } catch (Exception e) {
            log.warn("Snapshot persistence failed for {}/{} — metrics response unaffected: {}", owner, repo, e.getMessage());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<MetricHistoryResponse> getHistory(
            @RequestParam String owner,
            @RequestParam String repo,
            @RequestParam String metric,
            @RequestParam(defaultValue = "365") int lookbackDays) {

        log.info("History requested for {}/{} metric={} lookback={}", owner, repo, metric, lookbackDays);
        List<MetricSnapshot> snapshots = snapshotService.getHistory(owner, repo, metric, lookbackDays);
        String repoId = owner + "/" + repo;
        return ResponseEntity.ok(new MetricHistoryResponse(metric, repoId, snapshots));
    }
}
