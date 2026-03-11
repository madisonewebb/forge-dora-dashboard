package com.liatrio.dora.metrics;

import com.liatrio.dora.dto.DoraPerformanceBand;
import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.WeekDataPoint;
import com.liatrio.dora.model.GithubDeployment;
import com.liatrio.dora.model.GithubPullRequest;
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
import java.util.List;

@Component
public class LeadTimeCalculator {

    private static final Logger log = LoggerFactory.getLogger(LeadTimeCalculator.class);

    /** Maximum time between a PR merge and the following deployment to count as correlated. */
    private static final Duration MAX_CORRELATION_WINDOW = Duration.ofDays(7);

    private record PairData(Instant firstCommitAt, Instant deployedAt) {}

    public MetricResult calculate(List<GithubPullRequest> pullRequests,
                                  List<GithubDeployment> deployments,
                                  int windowDays) {
        if (pullRequests.isEmpty() || deployments.isEmpty()) {
            return MetricResult.notAvailable("No lead time data available.");
        }

        List<GithubDeployment> sortedDeployments = deployments.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .toList();

        List<PairData> pairs = new ArrayList<>();
        for (GithubPullRequest pr : pullRequests) {
            if (pr.getMergedAt() == null || pr.getFirstCommitAt() == null) continue;

            sortedDeployments.stream()
                    .filter(d -> d.getCreatedAt().isAfter(pr.getMergedAt()))
                    .filter(d -> Duration.between(pr.getMergedAt(), d.getCreatedAt())
                            .compareTo(MAX_CORRELATION_WINDOW) <= 0)
                    .findFirst()
                    .ifPresent(d -> pairs.add(new PairData(pr.getFirstCommitAt(), d.getCreatedAt())));
        }

        if (pairs.isEmpty()) {
            log.debug("No correlated PR→deployment pairs found");
            return MetricResult.notAvailable("No lead time data available.");
        }

        List<Double> leadTimesHours = pairs.stream()
                .map(p -> Duration.between(p.firstCommitAt(), p.deployedAt()).toMinutes() / 60.0)
                .sorted()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        double medianHours = median(leadTimesHours);
        DoraPerformanceBand band = DoraPerformanceBand.forLeadTime(medianHours);
        List<WeekDataPoint> timeSeries = buildWeeklyBuckets(pairs, windowDays);

        log.debug("Lead time median: {}h ({})", medianHours, band);
        return new MetricResult(medianHours, "hours", band, true, timeSeries, null);
    }

    private double median(List<Double> sorted) {
        return sorted.get((sorted.size() - 1) / 2);
    }

    private List<WeekDataPoint> buildWeeklyBuckets(List<PairData> pairs, int windowDays) {
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

            List<Double> weekLeadTimes = pairs.stream()
                    .filter(p -> {
                        LocalDate d = p.deployedAt().atZone(ZoneOffset.UTC).toLocalDate();
                        return !d.isBefore(weekStart) && d.isBefore(weekEnd);
                    })
                    .map(p -> Duration.between(p.firstCommitAt(), p.deployedAt()).toMinutes() / 60.0)
                    .sorted()
                    .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

            double weekValue = weekLeadTimes.isEmpty() ? 0.0 : median(weekLeadTimes);
            buckets.add(new WeekDataPoint(weekStart, weekValue));
            weekCursor = weekCursor.plusWeeks(1);
        }

        return buckets;
    }
}
