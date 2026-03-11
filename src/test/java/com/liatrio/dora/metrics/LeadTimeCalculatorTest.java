package com.liatrio.dora.metrics;

import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.model.GithubDeployment;
import com.liatrio.dora.model.GithubPullRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LeadTimeCalculatorTest {

    private LeadTimeCalculator calculator;
    private static final Instant NOW = Instant.now();

    @BeforeEach
    void setUp() {
        calculator = new LeadTimeCalculator();
    }

    @Test
    void calculate_computesMedianNotAverage() {
        // Three PRs with lead times of 1h, 3h, 10h → median = 3h (not avg 4.67h)
        // Deploy at NOW, PRs have firstCommitAt = deploy time minus lead time
        Instant deploy = NOW.minus(1, ChronoUnit.DAYS);

        GithubPullRequest pr1 = buildPr(1L, deploy.minus(1, ChronoUnit.HOURS), deploy.minus(30, ChronoUnit.MINUTES));
        GithubPullRequest pr2 = buildPr(2L, deploy.minus(3, ChronoUnit.HOURS), deploy.minus(30, ChronoUnit.MINUTES));
        GithubPullRequest pr3 = buildPr(3L, deploy.minus(10, ChronoUnit.HOURS), deploy.minus(30, ChronoUnit.MINUTES));

        GithubDeployment d1 = buildDeployment(1L, deploy);

        MetricResult result = calculator.calculate(List.of(pr1, pr2, pr3), List.of(d1), 30);

        assertTrue(result.dataAvailable());
        assertEquals(3.0, result.value(), 0.1);
    }

    @Test
    void calculate_excludesOutlierPrs() {
        // Outlier PR merged 10 days before the deployment — should NOT be correlated
        Instant deploy = NOW.minus(1, ChronoUnit.DAYS);
        GithubPullRequest outlier = buildPr(99L,
                deploy.minus(11, ChronoUnit.DAYS),    // firstCommitAt
                deploy.minus(10, ChronoUnit.DAYS));    // mergedAt — too far before deploy

        GithubDeployment d1 = buildDeployment(1L, deploy);

        MetricResult result = calculator.calculate(List.of(outlier), List.of(d1), 30);

        assertFalse(result.dataAvailable());
    }

    @Test
    void calculate_noData_returnsNotAvailable() {
        MetricResult result = calculator.calculate(List.of(), List.of(), 30);

        assertFalse(result.dataAvailable());
        assertNotNull(result.message());
    }

    @Test
    void calculate_weeklyTimeSeries_correctBucketCount_90Days() {
        // ceil(90/7) = 13 buckets
        Instant deploy = NOW.minus(1, ChronoUnit.DAYS);
        GithubPullRequest pr = buildPr(1L, deploy.minus(2, ChronoUnit.HOURS), deploy.minus(30, ChronoUnit.MINUTES));
        GithubDeployment d = buildDeployment(1L, deploy);

        MetricResult result = calculator.calculate(List.of(pr), List.of(d), 90);

        assertTrue(result.dataAvailable());
        assertEquals(13, result.timeSeries().size());
    }

    @Test
    void calculate_evenCountList_usesLowerMedian() {
        // Four lead times: 1h, 2h, 4h, 8h → sorted, lower-middle = 2h
        Instant deploy = NOW.minus(1, ChronoUnit.DAYS);
        GithubPullRequest pr1 = buildPr(1L, deploy.minus(1, ChronoUnit.HOURS), deploy.minus(10, ChronoUnit.MINUTES));
        GithubPullRequest pr2 = buildPr(2L, deploy.minus(2, ChronoUnit.HOURS), deploy.minus(10, ChronoUnit.MINUTES));
        GithubPullRequest pr3 = buildPr(3L, deploy.minus(4, ChronoUnit.HOURS), deploy.minus(10, ChronoUnit.MINUTES));
        GithubPullRequest pr4 = buildPr(4L, deploy.minus(8, ChronoUnit.HOURS), deploy.minus(10, ChronoUnit.MINUTES));

        GithubDeployment d = buildDeployment(1L, deploy);
        MetricResult result = calculator.calculate(List.of(pr1, pr2, pr3, pr4), List.of(d), 30);

        assertTrue(result.dataAvailable());
        assertEquals(2.0, result.value(), 0.1);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private GithubPullRequest buildPr(Long id, Instant firstCommitAt, Instant mergedAt) {
        return GithubPullRequest.builder()
                .githubId(id)
                .repoId("owner/repo")
                .firstCommitAt(firstCommitAt)
                .mergedAt(mergedAt)
                .createdAt(firstCommitAt)
                .build();
    }

    private GithubDeployment buildDeployment(Long id, Instant createdAt) {
        return GithubDeployment.builder()
                .githubId(id)
                .repoId("owner/repo")
                .status("success")
                .createdAt(createdAt)
                .build();
    }
}
