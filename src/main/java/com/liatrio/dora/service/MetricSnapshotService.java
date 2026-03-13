package com.liatrio.dora.service;

import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.MetricsResponse;
import com.liatrio.dora.model.MetricSnapshot;
import com.liatrio.dora.repository.MetricSnapshotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MetricSnapshotService {

    private static final Logger log = LoggerFactory.getLogger(MetricSnapshotService.class);

    private final MetricSnapshotRepository snapshotRepository;

    @Value("${snapshot.retention-days:730}")
    private int retentionDays;

    public MetricSnapshotService(MetricSnapshotRepository snapshotRepository) {
        this.snapshotRepository = snapshotRepository;
    }

    /**
     * Persists one snapshot per metric (4 total) for the given repo and window.
     * Uses upsert logic: if a snapshot already exists for today (same repoId + metricName +
     * windowDays + calendar date in UTC), it is updated rather than duplicated.
     */
    public void saveSnapshot(String owner, String repo, int windowDays, MetricsResponse response) {
        String repoId = owner + "/" + repo;
        Instant now = Instant.now();
        Instant windowStart = now.minus(windowDays, ChronoUnit.DAYS);

        Map<String, MetricResult> metrics = Map.of(
                "deploymentFrequency", response.deploymentFrequency(),
                "leadTime",            response.leadTime(),
                "changeFailureRate",   response.changeFailureRate(),
                "mttr",                response.mttr()
        );

        for (Map.Entry<String, MetricResult> entry : metrics.entrySet()) {
            String metricName = entry.getKey();
            MetricResult result  = entry.getValue();

            try {
                // Determine today's UTC day boundaries for upsert window
                Instant dayStart = now.atZone(ZoneOffset.UTC).toLocalDate()
                        .atStartOfDay(ZoneOffset.UTC).toInstant();
                Instant dayEnd   = dayStart.plus(1, ChronoUnit.DAYS);

                Optional<MetricSnapshot> existing = snapshotRepository
                        .findByRepoIdAndMetricNameAndWindowDaysAndSnapshotAtBetween(
                                repoId, metricName, windowDays, dayStart, dayEnd);

                MetricSnapshot snapshot = existing.orElseGet(MetricSnapshot::new);
                snapshot.setRepoId(repoId);
                snapshot.setMetricName(metricName);
                snapshot.setWindowDays(windowDays);
                snapshot.setValue(result.value());
                snapshot.setUnit(result.unit());
                snapshot.setBand(result.band());
                snapshot.setDataAvailable(result.dataAvailable());
                snapshot.setSnapshotAt(now);
                snapshot.setWindowStart(windowStart);
                snapshot.setWindowEnd(now);

                snapshotRepository.save(snapshot);
                log.debug("Saved snapshot for {}/{} metric={} windowDays={}", owner, repo, metricName, windowDays);
            } catch (Exception e) {
                log.error("Failed to save snapshot for metric {} repo {}: {}", metricName, repoId, e.getMessage(), e);
            }
        }
    }

    /**
     * Deletes snapshots older than the configured retention period. Runs daily at 03:00 UTC.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldSnapshots() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        log.info("Purging metric snapshots older than {} days (before {})", retentionDays, cutoff);
        snapshotRepository.deleteBySnapshotAtBefore(cutoff);
    }

    /**
     * Returns snapshots sorted ascending by snapshot time for the given lookback period.
     */
    public List<MetricSnapshot> getHistory(String owner, String repo, String metricName, int lookbackDays) {
        String repoId = owner + "/" + repo;
        Instant after = Instant.now().minus(lookbackDays, ChronoUnit.DAYS);
        return snapshotRepository.findByRepoIdAndMetricNameAndSnapshotAtAfterOrderBySnapshotAtAsc(
                repoId, metricName, after);
    }
}
