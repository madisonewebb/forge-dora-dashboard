package com.liatrio.dora.metrics;

import com.liatrio.dora.dto.DoraPerformanceBand;
import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.WeekDataPoint;
import com.liatrio.dora.model.GithubDeployment;
import com.liatrio.dora.model.GithubIssue;
import com.liatrio.dora.model.GithubPullRequest;
import com.liatrio.dora.model.GithubWorkflowRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class ChangeFailureRateCalculator {

    private static final Logger log = LoggerFactory.getLogger(ChangeFailureRateCalculator.class);

    private static final Set<String> FAILURE_PR_LABELS = Set.of("hotfix", "revert", "bug");
    private static final Set<String> INCIDENT_ISSUE_LABELS = Set.of("incident", "outage");
    private static final Pattern ROLLBACK_PATTERN = Pattern.compile("(?i)^(rollback|revert).*");
    private static final Duration INCIDENT_WINDOW = Duration.ofHours(24);

    public MetricResult calculate(List<GithubDeployment> deployments,
                                  List<GithubPullRequest> pullRequests,
                                  List<GithubIssue> issues,
                                  List<GithubWorkflowRun> workflowRuns,
                                  int windowDays) {
        if (deployments.isEmpty()) {
            return calculateFromWorkflowRuns(workflowRuns, pullRequests, windowDays);
        }

        // Use a Set of deployment githubIds to deduplicate across signals
        Set<Long> failedDeploymentIds = new HashSet<>();

        for (GithubDeployment deploy : deployments) {
            // Signal 1: PR with failure label merged in the 24h before deployment
            boolean labeledPr = pullRequests.stream()
                    .filter(pr -> pr.getMergedAt() != null)
                    .filter(pr -> {
                        Duration gap = Duration.between(pr.getMergedAt(), deploy.getCreatedAt());
                        return !gap.isNegative() && gap.compareTo(INCIDENT_WINDOW) <= 0;
                    })
                    .anyMatch(pr -> hasAnyLabel(pr.getLabels(), FAILURE_PR_LABELS));

            if (labeledPr) failedDeploymentIds.add(deploy.getGithubId());

            // Signal 2: Issue with incident label opened within 24h after deployment
            boolean incidentIssue = issues.stream()
                    .filter(issue -> {
                        Duration gap = Duration.between(deploy.getCreatedAt(), issue.getCreatedAt());
                        return !gap.isNegative() && gap.compareTo(INCIDENT_WINDOW) <= 0;
                    })
                    .anyMatch(issue -> hasAnyLabel(issue.getLabels(), INCIDENT_ISSUE_LABELS));

            if (incidentIssue) failedDeploymentIds.add(deploy.getGithubId());

            // Signal 3: Rollback/revert workflow run within 24h after deployment
            boolean rollbackRun = workflowRuns.stream()
                    .filter(run -> run.getName() != null)
                    .filter(run -> {
                        Duration gap = Duration.between(deploy.getCreatedAt(), run.getCreatedAt());
                        return !gap.isNegative() && gap.compareTo(INCIDENT_WINDOW) <= 0;
                    })
                    .anyMatch(run -> ROLLBACK_PATTERN.matcher(run.getName()).matches());

            if (rollbackRun) failedDeploymentIds.add(deploy.getGithubId());
        }

        double cfr = (double) failedDeploymentIds.size() / deployments.size() * 100.0;
        DoraPerformanceBand band = DoraPerformanceBand.forChangeFailureRate(cfr);
        List<WeekDataPoint> timeSeries = buildWeeklyBuckets(deployments, failedDeploymentIds, windowDays);

        log.debug("Change failure rate: {}% ({})", cfr, band);
        return new MetricResult(cfr, "%", band, true, timeSeries, null);
    }

    private MetricResult calculateFromWorkflowRuns(List<GithubWorkflowRun> workflowRuns,
                                                    List<GithubPullRequest> pullRequests,
                                                    int windowDays) {
        List<GithubWorkflowRun> completed = workflowRuns.stream()
                .filter(r -> "completed".equals(r.getStatus()))
                .filter(r -> "success".equals(r.getConclusion()) || "failure".equals(r.getConclusion()))
                .toList();

        // Also count revert/hotfix/bug PRs as a failure signal
        long revertPrs = pullRequests.stream()
                .filter(pr -> pr.getMergedAt() != null)
                .filter(pr -> hasAnyLabel(pr.getLabels(), FAILURE_PR_LABELS))
                .count();

        long total = completed.size() + revertPrs;
        if (total == 0) {
            return MetricResult.notAvailable("No deployment data found.");
        }

        long failedRuns = completed.stream()
                .filter(r -> "failure".equals(r.getConclusion()))
                .count();
        long effectiveFailures = Math.max(failedRuns, revertPrs);

        double cfr = (double) effectiveFailures / total * 100.0;
        DoraPerformanceBand band = DoraPerformanceBand.forChangeFailureRate(cfr);
        List<WeekDataPoint> timeSeries = buildWorkflowWeeklyBuckets(completed, windowDays);

        log.debug("Change failure rate (CI proxy): {}% ({})", cfr, band);
        return new MetricResult(cfr, "%", band, true, timeSeries,
                "Based on CI workflow failures on main (no GitHub Deployments configured)");
    }

    private List<WeekDataPoint> buildWorkflowWeeklyBuckets(List<GithubWorkflowRun> runs, int windowDays) {
        Instant windowStart = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        LocalDate startDate = windowStart.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate weekCursor = startDate.with(WeekFields.ISO.dayOfWeek(), 1);
        if (weekCursor.isAfter(startDate)) weekCursor = weekCursor.minusWeeks(1);

        int bucketCount = (int) Math.ceil(windowDays / 7.0);
        List<WeekDataPoint> buckets = new ArrayList<>();

        for (int i = 0; i < bucketCount; i++) {
            final LocalDate weekStart = weekCursor;
            final LocalDate weekEnd = weekStart.plusWeeks(1);

            List<GithubWorkflowRun> weekRuns = runs.stream()
                    .filter(r -> {
                        LocalDate d = r.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
                        return !d.isBefore(weekStart) && d.isBefore(weekEnd);
                    })
                    .toList();

            double weekCfr = 0.0;
            if (!weekRuns.isEmpty()) {
                long failures = weekRuns.stream().filter(r -> "failure".equals(r.getConclusion())).count();
                weekCfr = (double) failures / weekRuns.size() * 100.0;
            }

            buckets.add(new WeekDataPoint(weekStart, weekCfr));
            weekCursor = weekCursor.plusWeeks(1);
        }

        return buckets;
    }

    private boolean hasAnyLabel(String labelsField, Set<String> targets) {
        if (labelsField == null || labelsField.isBlank()) return false;
        for (String label : labelsField.split(",")) {
            if (targets.contains(label.trim().toLowerCase())) return true;
        }
        return false;
    }

    private List<WeekDataPoint> buildWeeklyBuckets(List<GithubDeployment> deployments,
                                                    Set<Long> failedIds,
                                                    int windowDays) {
        Instant windowStart = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        LocalDate startDate = windowStart.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate weekCursor = startDate.with(WeekFields.ISO.dayOfWeek(), 1);
        if (weekCursor.isAfter(startDate)) {
            weekCursor = weekCursor.minusWeeks(1);
        }

        int bucketCount = (int) Math.ceil(windowDays / 7.0);
        List<WeekDataPoint> buckets = new ArrayList<>();

        for (int i = 0; i < bucketCount; i++) {
            final LocalDate weekStart = weekCursor;
            final LocalDate weekEnd = weekStart.plusWeeks(1);

            List<GithubDeployment> weekDeploys = deployments.stream()
                    .filter(d -> {
                        LocalDate day = d.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
                        return !day.isBefore(weekStart) && day.isBefore(weekEnd);
                    })
                    .toList();

            double weekCfr = 0.0;
            if (!weekDeploys.isEmpty()) {
                long failures = weekDeploys.stream()
                        .filter(d -> failedIds.contains(d.getGithubId()))
                        .count();
                weekCfr = (double) failures / weekDeploys.size() * 100.0;
            }

            buckets.add(new WeekDataPoint(weekStart, weekCfr));
            weekCursor = weekCursor.plusWeeks(1);
        }

        return buckets;
    }
}
