package com.liatrio.dora.repository;

import com.liatrio.dora.model.MetricSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MetricSnapshotRepository extends JpaRepository<MetricSnapshot, Long> {

    List<MetricSnapshot> findByRepoIdAndMetricNameOrderBySnapshotAtDesc(String repoId, String metricName);

    List<MetricSnapshot> findByRepoIdAndMetricNameAndSnapshotAtAfterOrderBySnapshotAtAsc(
            String repoId, String metricName, Instant after);

    Optional<MetricSnapshot> findByRepoIdAndMetricNameAndWindowDaysAndSnapshotAtBetween(
            String repoId, String metricName, Integer windowDays, Instant start, Instant end);

    void deleteBySnapshotAtBefore(Instant cutoff);
}
