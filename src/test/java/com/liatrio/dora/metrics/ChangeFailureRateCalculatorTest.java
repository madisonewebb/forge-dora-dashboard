package com.liatrio.dora.metrics;

import com.liatrio.dora.config.DoraLabelsProperties;
import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.model.GithubDeployment;
import com.liatrio.dora.model.GithubIssue;
import com.liatrio.dora.model.GithubPullRequest;
import com.liatrio.dora.model.GithubWorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChangeFailureRateCalculatorTest {

    private ChangeFailureRateCalculator calculator;
    private static final Instant NOW = Instant.now();
    private static final Instant DEPLOY_TIME = NOW.minus(5, ChronoUnit.DAYS);

    @BeforeEach
    void setUp() {
        calculator = new ChangeFailureRateCalculator(defaultLabels());
    }

    private static DoraLabelsProperties defaultLabels() {
        DoraLabelsProperties props = new DoraLabelsProperties();
        props.setIncident(List.of("incident", "outage"));
        props.setBug(List.of("bug", "defect"));
        props.setHotfix(List.of("hotfix", "hot-fix", "hotpatch"));
        props.setRevert(List.of("revert", "rollback"));
        return props;
    }

    @Test
    void calculate_labeledPrSignal_countsFailure() {
        GithubDeployment deploy = buildDeployment(1L, DEPLOY_TIME);
        // PR with "hotfix" label merged 2 hours before deploy
        GithubPullRequest pr = buildPr(1L, DEPLOY_TIME.minus(2, ChronoUnit.HOURS), "hotfix");

        MetricResult result = calculator.calculate(
                List.of(deploy), List.of(pr), List.of(), List.of(), 30);

        assertTrue(result.dataAvailable());
        assertTrue(result.value() > 0, "Expected failure rate > 0");
    }

    @Test
    void calculate_incidentIssueSignal_countsFailure() {
        GithubDeployment deploy = buildDeployment(1L, DEPLOY_TIME);
        // Issue labeled "incident" opened 1 hour after deploy
        GithubIssue issue = buildIssue(1L, DEPLOY_TIME.plus(1, ChronoUnit.HOURS), "incident");

        MetricResult result = calculator.calculate(
                List.of(deploy), List.of(), List.of(issue), List.of(), 30);

        assertTrue(result.dataAvailable());
        assertTrue(result.value() > 0, "Expected failure rate > 0");
    }

    @Test
    void calculate_rollbackWorkflowSignal_countsFailure() {
        GithubDeployment deploy = buildDeployment(1L, DEPLOY_TIME);
        // Workflow run named "rollback-production" created 30 minutes after deploy
        GithubWorkflowRun run = buildWorkflowRun(1L, "rollback-production",
                DEPLOY_TIME.plus(30, ChronoUnit.MINUTES));

        MetricResult result = calculator.calculate(
                List.of(deploy), List.of(), List.of(), List.of(run), 30);

        assertTrue(result.dataAvailable());
        assertTrue(result.value() > 0, "Expected failure rate > 0");
    }

    @Test
    void calculate_deduplication_multipleSignalsCounts1() {
        // All three signals point to the same deployment — should count as 1 failure
        GithubDeployment deploy = buildDeployment(1L, DEPLOY_TIME);
        GithubPullRequest pr = buildPr(1L, DEPLOY_TIME.minus(1, ChronoUnit.HOURS), "revert");
        GithubIssue issue = buildIssue(1L, DEPLOY_TIME.plus(1, ChronoUnit.HOURS), "outage");
        GithubWorkflowRun run = buildWorkflowRun(1L, "revert-deploy",
                DEPLOY_TIME.plus(30, ChronoUnit.MINUTES));

        MetricResult result = calculator.calculate(
                List.of(deploy), List.of(pr), List.of(issue), List.of(run), 30);

        assertTrue(result.dataAvailable());
        // 1 failure out of 1 deployment = 100%
        assertEquals(100.0, result.value(), 0.01);
    }

    @Test
    void calculate_noDeployments_returnsNotAvailable() {
        MetricResult result = calculator.calculate(
                List.of(), List.of(), List.of(), List.of(), 30);

        assertFalse(result.dataAvailable());
        assertNotNull(result.message());
    }

    @Test
    void calculate_noFailureSignals_returnsZeroPercent() {
        GithubDeployment deploy = buildDeployment(1L, DEPLOY_TIME);

        MetricResult result = calculator.calculate(
                List.of(deploy), List.of(), List.of(), List.of(), 30);

        assertTrue(result.dataAvailable());
        assertEquals(0.0, result.value(), 0.01);
    }

    @Test
    void calculate_weeklyTimeSeries_has5BucketsFor30Days() {
        GithubDeployment deploy = buildDeployment(1L, DEPLOY_TIME);

        MetricResult result = calculator.calculate(
                List.of(deploy), List.of(), List.of(), List.of(), 30);

        assertEquals(5, result.timeSeries().size());
    }

    @Test
    void calculate_customIncidentLabel_sev1_countsFailure() {
        // Configure "sev1" as an incident label (non-default)
        DoraLabelsProperties customProps = new DoraLabelsProperties();
        customProps.setIncident(List.of("sev1", "p0"));
        customProps.setBug(List.of("bug", "defect"));
        customProps.setHotfix(List.of("hotfix", "hot-fix", "hotpatch"));
        customProps.setRevert(List.of("revert", "rollback"));
        ChangeFailureRateCalculator customCalc = new ChangeFailureRateCalculator(customProps);

        GithubDeployment deploy = buildDeployment(1L, DEPLOY_TIME);
        // Issue labeled "sev1" opened 1 hour after deploy
        GithubIssue issue = buildIssue(1L, DEPLOY_TIME.plus(1, ChronoUnit.HOURS), "sev1");

        MetricResult result = customCalc.calculate(
                List.of(deploy), List.of(), List.of(issue), List.of(), 30);

        assertTrue(result.dataAvailable());
        assertTrue(result.value() > 0, "Expected failure rate > 0 with custom 'sev1' incident label");
    }

    @Test
    void calculate_customHotfixLabel_hotDash_countsFailure() {
        // Verify that hyphen/underscore equivalence works: "hot-fix" matches "hot_fix"
        DoraLabelsProperties customProps = new DoraLabelsProperties();
        customProps.setIncident(List.of("incident"));
        customProps.setBug(List.of("bug"));
        customProps.setHotfix(List.of("hot_fix"));
        customProps.setRevert(List.of("revert"));
        ChangeFailureRateCalculator customCalc = new ChangeFailureRateCalculator(customProps);

        GithubDeployment deploy = buildDeployment(1L, DEPLOY_TIME);
        // PR labeled "hot-fix" (hyphen) merged 2h before deploy
        GithubPullRequest pr = buildPr(1L, DEPLOY_TIME.minus(2, ChronoUnit.HOURS), "hot-fix");

        MetricResult result = customCalc.calculate(
                List.of(deploy), List.of(pr), List.of(), List.of(), 30);

        assertTrue(result.dataAvailable());
        assertTrue(result.value() > 0, "Expected failure rate > 0 with hyphen/underscore equivalent label");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private GithubDeployment buildDeployment(Long id, Instant createdAt) {
        return GithubDeployment.builder()
                .githubId(id)
                .repoId("owner/repo")
                .status("success")
                .createdAt(createdAt)
                .build();
    }

    private GithubPullRequest buildPr(Long id, Instant mergedAt, String label) {
        return GithubPullRequest.builder()
                .githubId(id)
                .repoId("owner/repo")
                .labels(label)
                .mergedAt(mergedAt)
                .createdAt(mergedAt.minus(1, ChronoUnit.HOURS))
                .build();
    }

    private GithubIssue buildIssue(Long id, Instant createdAt, String label) {
        return GithubIssue.builder()
                .githubId(id)
                .repoId("owner/repo")
                .labels(label)
                .state("open")
                .createdAt(createdAt)
                .build();
    }

    private GithubWorkflowRun buildWorkflowRun(Long id, String name, Instant createdAt) {
        return GithubWorkflowRun.builder()
                .githubId(id)
                .repoId("owner/repo")
                .name(name)
                .status("completed")
                .conclusion("success")
                .createdAt(createdAt)
                .build();
    }
}
