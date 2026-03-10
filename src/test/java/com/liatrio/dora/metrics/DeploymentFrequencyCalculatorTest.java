package com.liatrio.dora.metrics;

import com.liatrio.dora.dto.DoraPerformanceBand;
import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.model.GithubDeployment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeploymentFrequencyCalculatorTest {

    private DeploymentFrequencyCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new DeploymentFrequencyCalculator();
    }

    @Test
    void calculate_elite_returnsCorrectRateAndBand() {
        // 60 successful deployments over 30 days = 2.0/day → ELITE
        List<GithubDeployment> deployments = buildDeployments(60, "success", 30);

        MetricResult result = calculator.calculate(deployments, 30);

        assertTrue(result.dataAvailable());
        assertEquals(DoraPerformanceBand.ELITE, result.band());
        assertEquals("deploys/day", result.unit());
        assertEquals(2.0, result.value(), 0.01);
    }

    @Test
    void calculate_noSuccessDeployments_returnsNotAvailable() {
        MetricResult result = calculator.calculate(List.of(), 30);

        assertFalse(result.dataAvailable());
        assertNotNull(result.message());
    }

    @Test
    void calculate_filtersNonSuccessDeployments() {
        // 5 failed + 3 successful = only 3 counted → 3/30 ≈ 0.1/day → HIGH
        List<GithubDeployment> deployments = new ArrayList<>();
        deployments.addAll(buildDeployments(5, "failure", 30));
        deployments.addAll(buildDeployments(3, "success", 30));

        MetricResult result = calculator.calculate(deployments, 30);

        assertTrue(result.dataAvailable());
        assertEquals(3.0 / 30, result.value(), 0.01);
    }

    @Test
    void calculate_weeklyTimeSeries_has5BucketsFor30Days() {
        // ceil(30/7) = 5 buckets
        List<GithubDeployment> deployments = buildDeployments(10, "success", 30);

        MetricResult result = calculator.calculate(deployments, 30);

        assertTrue(result.dataAvailable());
        assertEquals(5, result.timeSeries().size());
    }

    @Test
    void calculate_weeklyTimeSeries_has13BucketsFor90Days() {
        // ceil(90/7) = 13 buckets
        List<GithubDeployment> deployments = buildDeployments(30, "success", 90);

        MetricResult result = calculator.calculate(deployments, 90);

        assertTrue(result.dataAvailable());
        assertEquals(13, result.timeSeries().size());
    }

    @Test
    void calculate_low_singleDeploymentOver60Days() {
        // 1 deploy / 60 days ≈ 0.017/day → LOW
        List<GithubDeployment> deployments = buildDeployments(1, "success", 60);

        MetricResult result = calculator.calculate(deployments, 60);

        assertTrue(result.dataAvailable());
        assertEquals(DoraPerformanceBand.LOW, result.band());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private List<GithubDeployment> buildDeployments(int count, String status, int windowDays) {
        List<GithubDeployment> list = new ArrayList<>();
        Instant windowStart = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        for (int i = 0; i < count; i++) {
            long offsetSeconds = (long) ((windowDays * 86400L) / (count + 1.0)) * (i + 1);
            list.add(GithubDeployment.builder()
                    .githubId((long) i)
                    .repoId("owner/repo")
                    .status(status)
                    .environment("production")
                    .createdAt(windowStart.plusSeconds(offsetSeconds))
                    .build());
        }
        return list;
    }
}
