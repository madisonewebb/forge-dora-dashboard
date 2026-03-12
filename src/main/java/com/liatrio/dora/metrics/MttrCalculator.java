package com.liatrio.dora.metrics;

import com.liatrio.dora.config.DoraLabelsProperties;
import com.liatrio.dora.dto.DoraPerformanceBand;
import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.WeekDataPoint;
import com.liatrio.dora.model.GithubDeployment;
import com.liatrio.dora.model.GithubIssue;
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
import java.util.stream.Collectors;

@Component
public class MttrCalculator {

    private static final Logger log = LoggerFactory.getLogger(MttrCalculator.class);
    private static final int MIN_INCIDENTS = 1;

    private final DoraLabelsProperties labelsProperties;

    public MttrCalculator(DoraLabelsProperties labelsProperties) {
        this.labelsProperties = labelsProperties;
    }

    public MetricResult calculate(List<GithubIssue> issues,
                                  List<GithubDeployment> deployments,
                                  int windowDays) {
        // Signal 1: incident issues with a closedAt timestamp
        List<GithubIssue> incidentIssues = issues.stream()
                .filter(i -> i.getClosedAt() != null)
                .filter(i -> hasIncidentLabel(i.getLabels()))
                .toList();

        if (incidentIssues.size() >= MIN_INCIDENTS) {
            return calculateFromIssues(incidentIssues, windowDays);
        }

        // Signal 2: bug-labeled issues as a proxy for incidents
        List<GithubIssue> bugIssues = issues.stream()
                .filter(i -> i.getClosedAt() != null)
                .filter(i -> hasBugLabel(i.getLabels()))
                .toList();

        if (bugIssues.size() >= MIN_INCIDENTS) {
            return calculateFromIssues(bugIssues, windowDays);
        }

        // Signal 3: failed deployment → next successful deployment
        MetricResult fromGaps = calculateFromDeploymentGaps(deployments, windowDays);
        if (fromGaps.dataAvailable()) {
            return fromGaps;
        }

        // Signal 4: all closed issues as a last-resort proxy for resolution time
        List<GithubIssue> closedIssues = issues.stream()
                .filter(i -> i.getClosedAt() != null)
                .toList();

        if (!closedIssues.isEmpty()) {
            log.debug("Using all closed issues as MTTR proxy");
            MetricResult result = calculateFromIssues(closedIssues, windowDays);
            return new MetricResult(result.value(), result.unit(), result.band(),
                    true, result.timeSeries(),
                    "Based on issue resolution time (label issues 'incident' for accuracy)");
        }

        return MetricResult.notAvailable("No closed issues found in this window.");
    }

    private MetricResult calculateFromIssues(List<GithubIssue> incidents, int windowDays) {
        List<Double> mttrHours = incidents.stream()
                .map(i -> Duration.between(i.getCreatedAt(), i.getClosedAt()).toMinutes() / 60.0)
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));

        double medianHours = median(mttrHours);
        DoraPerformanceBand band = DoraPerformanceBand.forMttr(medianHours);
        List<WeekDataPoint> timeSeries = buildIssueWeeklyBuckets(incidents, windowDays);

        log.debug("MTTR (issue signal): {}h ({})", medianHours, band);
        return new MetricResult(medianHours, "hours", band, true, timeSeries, null);
    }

    private MetricResult calculateFromDeploymentGaps(List<GithubDeployment> deployments,
                                                      int windowDays) {
        if (deployments.isEmpty()) {
            return MetricResult.notAvailable("No recovery data found.");
        }

        List<GithubDeployment> sorted = deployments.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .toList();

        record Gap(Instant failedAt, double hours) {}
        List<Gap> gaps = new ArrayList<>();

        for (int i = 0; i < sorted.size() - 1; i++) {
            GithubDeployment current = sorted.get(i);
            GithubDeployment next = sorted.get(i + 1);
            if ("failure".equals(current.getStatus()) && "success".equals(next.getStatus())) {
                double hours = Duration.between(current.getCreatedAt(), next.getCreatedAt()).toMinutes() / 60.0;
                gaps.add(new Gap(current.getCreatedAt(), hours));
            }
        }

        if (gaps.size() < MIN_INCIDENTS) {
            return MetricResult.notAvailable("No recovery data found.");
        }

        List<Double> gapHours = gaps.stream()
                .map(Gap::hours)
                .sorted()
                .collect(Collectors.toCollection(ArrayList::new));

        double medianHours = median(gapHours);
        DoraPerformanceBand band = DoraPerformanceBand.forMttr(medianHours);
        List<WeekDataPoint> timeSeries = buildGapWeeklyBuckets(gaps.stream()
                .map(g -> new GapData(g.failedAt(), g.hours()))
                .toList(), windowDays);

        log.debug("MTTR (deployment-gap signal): {}h ({})", medianHours, band);
        return new MetricResult(medianHours, "hours", band, true, timeSeries, null);
    }

    private static double median(List<Double> sorted) {
        int size = sorted.size();
        if (size % 2 == 1) {
            return sorted.get(size / 2);
        }
        return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
    }

    private boolean hasIncidentLabel(String labelsField) {
        return matchesAny(labelsField, labelsProperties.getIncident());
    }

    private boolean hasBugLabel(String labelsField) {
        return matchesAny(labelsField, labelsProperties.getBug());
    }

    /**
     * Returns true if any label in the comma-delimited {@code labelsField} matches any entry in
     * {@code targets}. Matching is case-insensitive and treats hyphens and underscores as equivalent.
     */
    private static boolean matchesAny(String labelsField, List<String> targets) {
        if (labelsField == null || labelsField.isBlank() || targets == null || targets.isEmpty()) {
            return false;
        }
        for (String label : labelsField.split(",")) {
            String normalized = normalize(label.trim());
            for (String target : targets) {
                if (normalized.equals(normalize(target))) return true;
            }
        }
        return false;
    }

    private static String normalize(String s) {
        return s.toLowerCase().replace('-', '_');
    }

    private List<WeekDataPoint> buildIssueWeeklyBuckets(List<GithubIssue> incidents, int windowDays) {
        Instant windowStart = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        LocalDate startDate = windowStart.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate weekCursor = startDate.with(WeekFields.ISO.dayOfWeek(), 1);
        if (weekCursor.isAfter(startDate)) weekCursor = weekCursor.minusWeeks(1);

        int bucketCount = (int) Math.ceil(windowDays / 7.0);
        List<WeekDataPoint> buckets = new ArrayList<>();

        for (int i = 0; i < bucketCount; i++) {
            final LocalDate weekStart = weekCursor;
            final LocalDate weekEnd = weekStart.plusWeeks(1);

            List<Double> weekMttrs = incidents.stream()
                    .filter(issue -> {
                        LocalDate d = issue.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
                        return !d.isBefore(weekStart) && d.isBefore(weekEnd);
                    })
                    .map(issue -> Duration.between(issue.getCreatedAt(), issue.getClosedAt()).toMinutes() / 60.0)
                    .sorted()
                    .collect(Collectors.toCollection(ArrayList::new));

            double weekValue = weekMttrs.isEmpty() ? 0.0 : median(weekMttrs);
            buckets.add(new WeekDataPoint(weekStart, weekValue));
            weekCursor = weekCursor.plusWeeks(1);
        }

        return buckets;
    }

    private List<WeekDataPoint> buildGapWeeklyBuckets(List<GapData> gaps, int windowDays) {
        Instant windowStart = Instant.now().minus(windowDays, ChronoUnit.DAYS);
        LocalDate startDate = windowStart.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate weekCursor = startDate.with(WeekFields.ISO.dayOfWeek(), 1);
        if (weekCursor.isAfter(startDate)) weekCursor = weekCursor.minusWeeks(1);

        int bucketCount = (int) Math.ceil(windowDays / 7.0);
        List<WeekDataPoint> buckets = new ArrayList<>();

        for (int i = 0; i < bucketCount; i++) {
            final LocalDate weekStart = weekCursor;
            final LocalDate weekEnd = weekStart.plusWeeks(1);

            List<Double> weekGaps = gaps.stream()
                    .filter(g -> {
                        LocalDate d = g.failedAt().atZone(ZoneOffset.UTC).toLocalDate();
                        return !d.isBefore(weekStart) && d.isBefore(weekEnd);
                    })
                    .map(GapData::hours)
                    .sorted()
                    .collect(Collectors.toCollection(ArrayList::new));

            double weekValue = weekGaps.isEmpty() ? 0.0 : median(weekGaps);
            buckets.add(new WeekDataPoint(weekStart, weekValue));
            weekCursor = weekCursor.plusWeeks(1);
        }

        return buckets;
    }

    private record GapData(Instant failedAt, double hours) {}
}
