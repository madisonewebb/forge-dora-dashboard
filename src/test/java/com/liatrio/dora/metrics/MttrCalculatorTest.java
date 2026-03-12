package com.liatrio.dora.metrics;

import com.liatrio.dora.dto.DoraPerformanceBand;
import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.model.GithubDeployment;
import com.liatrio.dora.model.GithubIssue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MttrCalculatorTest {

    private MttrCalculator calculator;
    private static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        calculator = new MttrCalculator();
    }

    @Test
    void calculate_issueSignal_computesOpenToCloseTime() {
        // 3 incidents: resolved in 1h, 3h, 5h → median = 3h → HIGH band
        List<GithubIssue> issues = List.of(
                buildIssue(1L, NOW.minus(10, ChronoUnit.DAYS), NOW.minus(10, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS)),
                buildIssue(2L, NOW.minus(8, ChronoUnit.DAYS), NOW.minus(8, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS)),
                buildIssue(3L, NOW.minus(6, ChronoUnit.DAYS), NOW.minus(6, ChronoUnit.DAYS).plus(5, ChronoUnit.HOURS))
        );

        MetricResult result = calculator.calculate(issues, List.of(), 30);

        assertTrue(result.dataAvailable());
        assertEquals(3.0, result.value(), 0.1);
        assertEquals(DoraPerformanceBand.HIGH, result.band());
        assertEquals("hours", result.unit());
    }

    @Test
    void calculate_deploymentGapFallback_usedWhenNoIssues() {
        // No issues at all → skips signals 1–2, falls back to deployment gaps (signal 3)
        // 3 failure→success pairs with gaps of 2h, 4h, 6h → median = 4h
        Instant base = NOW.minus(20, ChronoUnit.DAYS);
        List<GithubDeployment> deployments = List.of(
                buildDeployment(1L, "failure", base),
                buildDeployment(2L, "success", base.plus(2, ChronoUnit.HOURS)),
                buildDeployment(3L, "failure", base.plus(5, ChronoUnit.DAYS)),
                buildDeployment(4L, "success", base.plus(5, ChronoUnit.DAYS).plus(4, ChronoUnit.HOURS)),
                buildDeployment(5L, "failure", base.plus(10, ChronoUnit.DAYS)),
                buildDeployment(6L, "success", base.plus(10, ChronoUnit.DAYS).plus(6, ChronoUnit.HOURS))
        );

        MetricResult result = calculator.calculate(List.of(), deployments, 30);

        assertTrue(result.dataAvailable());
        assertEquals(4.0, result.value(), 0.1);
    }

    @Test
    void calculate_noIssuesAndNoDeployments_returnsNotAvailable() {
        // No issues, no deployments — all signals exhausted → not available
        MetricResult result = calculator.calculate(List.of(), List.of(), 30);

        assertFalse(result.dataAvailable());
        assertNotNull(result.message());
    }

    @Test
    void calculate_elite_under1Hour() {
        // 3 incidents each resolved in 30 minutes → median = 0.5h → ELITE
        List<GithubIssue> issues = List.of(
                buildIssue(1L, NOW.minus(10, ChronoUnit.DAYS), NOW.minus(10, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES)),
                buildIssue(2L, NOW.minus(8, ChronoUnit.DAYS), NOW.minus(8, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES)),
                buildIssue(3L, NOW.minus(6, ChronoUnit.DAYS), NOW.minus(6, ChronoUnit.DAYS).plus(30, ChronoUnit.MINUTES))
        );

        MetricResult result = calculator.calculate(issues, List.of(), 30);

        assertTrue(result.dataAvailable());
        assertEquals(DoraPerformanceBand.ELITE, result.band());
    }

    @Test
    void calculate_weeklyTimeSeries_has5BucketsFor30Days() {
        List<GithubIssue> issues = List.of(
                buildIssue(1L, NOW.minus(10, ChronoUnit.DAYS), NOW.minus(10, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS)),
                buildIssue(2L, NOW.minus(8, ChronoUnit.DAYS), NOW.minus(8, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS)),
                buildIssue(3L, NOW.minus(6, ChronoUnit.DAYS), NOW.minus(6, ChronoUnit.DAYS).plus(3, ChronoUnit.HOURS))
        );

        MetricResult result = calculator.calculate(issues, List.of(), 30);

        assertTrue(result.dataAvailable());
        assertEquals(5, result.timeSeries().size());
    }

    @Test
    void calculate_noDataAtAll_returnsNotAvailable() {
        MetricResult result = calculator.calculate(List.of(), List.of(), 30);

        assertFalse(result.dataAvailable());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private GithubIssue buildIssue(Long id, Instant createdAt, Instant closedAt) {
        return GithubIssue.builder()
                .githubId(id)
                .repoId("owner/repo")
                .labels("incident")
                .state("closed")
                .createdAt(createdAt)
                .closedAt(closedAt)
                .build();
    }

    private GithubDeployment buildDeployment(Long id, String status, Instant createdAt) {
        return GithubDeployment.builder()
                .githubId(id)
                .repoId("owner/repo")
                .status(status)
                .createdAt(createdAt)
                .build();
    }
}
