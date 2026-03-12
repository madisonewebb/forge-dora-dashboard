package com.liatrio.dora.metrics;

import com.liatrio.dora.dto.DoraPerformanceBand;
import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.WeekDataPoint;
import com.liatrio.dora.model.GithubDeployment;
import com.liatrio.dora.model.GithubRelease;
import com.liatrio.dora.model.GithubWorkflowRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeploymentFrequencyCalculator {

    private static final Logger log = LoggerFactory.getLogger(DeploymentFrequencyCalculator.class);

    public MetricResult calculate(List<GithubDeployment> deployments,
                                  List<GithubRelease> releases,
                                  List<GithubWorkflowRun> workflowRuns,
                                  int windowDays) {
        // Signal 1: GitHub Deployments API
        List<GithubDeployment> successful = deployments.stream()
                .filter(d -> "success".equals(d.getStatus()))
                .toList();

        if (!successful.isEmpty()) {
            double deploysPerDay = (double) successful.size() / windowDays;
            DoraPerformanceBand band = DoraPerformanceBand.forDeploymentFrequency(deploysPerDay);
            List<Instant> timestamps = successful.stream().map(GithubDeployment::getCreatedAt).toList();
            log.debug("Deployment frequency: {}/day ({}) from deployments", deploysPerDay, band);
            return new MetricResult(deploysPerDay, "deploys/day", band, true,
                    buildWeeklyBuckets(timestamps, windowDays), null);
        }

        // Signal 2: GitHub Releases (fallback when Deployments API is not configured)
        if (!releases.isEmpty()) {
            double releasesPerDay = (double) releases.size() / windowDays;
            DoraPerformanceBand band = DoraPerformanceBand.forDeploymentFrequency(releasesPerDay);
            List<Instant> timestamps = releases.stream()
                    .map(r -> r.getPublishedAt() != null ? r.getPublishedAt() : r.getCreatedAt())
                    .toList();
            log.debug("Deployment frequency: {}/day ({}) from releases (proxy)", releasesPerDay, band);
            return new MetricResult(releasesPerDay, "releases/day", band, true,
                    buildWeeklyBuckets(timestamps, windowDays),
                    "Based on GitHub Releases (no GitHub Deployments configured)");
        }

        // Signal 3: CI Workflow Runs on main (last resort fallback)
        List<GithubWorkflowRun> successfulRuns = workflowRuns.stream()
                .filter(r -> "success".equals(r.getConclusion()))
                .toList();

        if (successfulRuns.isEmpty()) {
            log.debug("No successful deployments, releases, or CI runs found in window of {} days", windowDays);
            return MetricResult.notAvailable("No deployment data found for this repository.");
        }

        double runsPerDay = (double) successfulRuns.size() / windowDays;
        DoraPerformanceBand band = DoraPerformanceBand.forDeploymentFrequency(runsPerDay);
        List<Instant> timestamps = successfulRuns.stream().map(GithubWorkflowRun::getCreatedAt).toList();
        log.debug("Deployment frequency: {}/day ({}) from CI runs (proxy)", runsPerDay, band);
        return new MetricResult(runsPerDay, "runs/day", band, true,
                buildWeeklyBuckets(timestamps, windowDays),
                "Based on successful CI runs on main (no GitHub Deployments configured)");
    }

    private List<WeekDataPoint> buildWeeklyBuckets(List<Instant> timestamps, int windowDays) {
        Instant windowStart = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        LocalDate startDate = windowStart.atZone(ZoneOffset.UTC).toLocalDate();
        // Align to Monday of the first ISO week
        LocalDate weekCursor = startDate.with(WeekFields.ISO.dayOfWeek(), 1);
        if (weekCursor.isAfter(startDate)) {
            weekCursor = weekCursor.minusWeeks(1);
        }

        int bucketCount = (int) Math.ceil(windowDays / 7.0);
        List<WeekDataPoint> buckets = new ArrayList<>();

        for (int i = 0; i < bucketCount; i++) {
            final LocalDate weekStart = weekCursor;
            final LocalDate weekEnd = weekStart.plusWeeks(1);

            long count = timestamps.stream().filter(ts -> {
                LocalDate d = ts.atZone(ZoneOffset.UTC).toLocalDate();
                return !d.isBefore(weekStart) && d.isBefore(weekEnd);
            }).count();

            buckets.add(new WeekDataPoint(weekStart, (double) count));
            weekCursor = weekCursor.plusWeeks(1);
        }

        return buckets;
    }
}
