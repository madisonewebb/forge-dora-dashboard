package com.liatrio.dora.service;

import com.liatrio.dora.dto.DoraPerformanceBand;
import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.MetricsMeta;
import com.liatrio.dora.dto.MetricsResponse;
import com.liatrio.dora.model.MetricSnapshot;
import com.liatrio.dora.repository.MetricSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricSnapshotServiceTest {

    @Mock
    private MetricSnapshotRepository snapshotRepository;

    private MetricSnapshotService snapshotService;

    @BeforeEach
    void setUp() {
        snapshotService = new MetricSnapshotService(snapshotRepository);
        // Inject the default retention value (730) that @Value would supply at runtime
        ReflectionTestUtils.setField(snapshotService, "retentionDays", 730);
    }

    // ── saveSnapshot — no existing snapshot today ─────────────────────────────

    @Test
    void saveSnapshot_noExistingRecord_insertsNewSnapshot() {
        when(snapshotRepository.findByRepoIdAndMetricNameAndWindowDaysAndSnapshotAtBetween(
                anyString(), anyString(), anyInt(), any(Instant.class), any(Instant.class)))
                .thenReturn(Optional.empty());

        snapshotService.saveSnapshot("liatrio", "test-repo", 30, buildResponse());

        // 4 metrics → 4 saves
        verify(snapshotRepository, times(4)).save(any(MetricSnapshot.class));
    }

    @Test
    void saveSnapshot_existingRecordToday_updatesInsteadOfInsert() {
        MetricSnapshot existing = MetricSnapshot.builder()
                .id(42L)
                .repoId("liatrio/test-repo")
                .metricName("deploymentFrequency")
                .windowDays(30)
                .value(0.5)
                .dataAvailable(true)
                .snapshotAt(Instant.now())
                .build();

        // Simulate one metric already having a snapshot today
        when(snapshotRepository.findByRepoIdAndMetricNameAndWindowDaysAndSnapshotAtBetween(
                eq("liatrio/test-repo"), eq("deploymentFrequency"), eq(30),
                any(Instant.class), any(Instant.class)))
                .thenReturn(Optional.of(existing));
        when(snapshotRepository.findByRepoIdAndMetricNameAndWindowDaysAndSnapshotAtBetween(
                eq("liatrio/test-repo"), argThat(s -> s != null && !s.equals("deploymentFrequency")), eq(30),
                any(Instant.class), any(Instant.class)))
                .thenReturn(Optional.empty());

        snapshotService.saveSnapshot("liatrio", "test-repo", 30, buildResponse());

        // Should still save all 4; the existing one is updated (same object mutated)
        ArgumentCaptor<MetricSnapshot> captor = ArgumentCaptor.forClass(MetricSnapshot.class);
        verify(snapshotRepository, times(4)).save(captor.capture());

        // The captured snapshot for deploymentFrequency must carry the pre-existing id
        List<MetricSnapshot> saved = captor.getAllValues();
        assertThat(saved).anyMatch(s -> Long.valueOf(42L).equals(s.getId()) &&
                "deploymentFrequency".equals(s.getMetricName()));
    }

    @Test
    void saveSnapshot_setsRepoIdAsOwnerSlashRepo() {
        when(snapshotRepository.findByRepoIdAndMetricNameAndWindowDaysAndSnapshotAtBetween(
                anyString(), anyString(), anyInt(), any(Instant.class), any(Instant.class)))
                .thenReturn(Optional.empty());

        snapshotService.saveSnapshot("myorg", "myrepo", 90, buildResponse());

        ArgumentCaptor<MetricSnapshot> captor = ArgumentCaptor.forClass(MetricSnapshot.class);
        verify(snapshotRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(s -> "myorg/myrepo".equals(s.getRepoId()));
    }

    @Test
    void saveSnapshot_setsWindowDaysOnAllSnapshots() {
        when(snapshotRepository.findByRepoIdAndMetricNameAndWindowDaysAndSnapshotAtBetween(
                anyString(), anyString(), anyInt(), any(Instant.class), any(Instant.class)))
                .thenReturn(Optional.empty());

        snapshotService.saveSnapshot("liatrio", "test-repo", 180, buildResponse());

        ArgumentCaptor<MetricSnapshot> captor = ArgumentCaptor.forClass(MetricSnapshot.class);
        verify(snapshotRepository, times(4)).save(captor.capture());
        assertThat(captor.getAllValues()).allMatch(s -> Integer.valueOf(180).equals(s.getWindowDays()));
    }

    // ── purgeOldSnapshots ─────────────────────────────────────────────────────

    @Test
    void purgeOldSnapshots_deletesSnapshotsBeyondRetentionWindow() {
        snapshotService.purgeOldSnapshots();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(snapshotRepository).deleteBySnapshotAtBefore(cutoffCaptor.capture());

        Instant cutoff = cutoffCaptor.getValue();
        // Default retention is 730 days; cutoff should be roughly 730 days ago
        Instant expectedMin = Instant.now().minus(731, java.time.temporal.ChronoUnit.DAYS);
        Instant expectedMax = Instant.now().minus(729, java.time.temporal.ChronoUnit.DAYS);
        assertThat(cutoff).isAfter(expectedMin).isBefore(expectedMax);
    }

    // ── getHistory ────────────────────────────────────────────────────────────

    @Test
    void getHistory_delegatesToRepositoryWithCorrectRepoId() {
        MetricSnapshot snap = MetricSnapshot.builder()
                .repoId("liatrio/test-repo")
                .metricName("leadTime")
                .snapshotAt(Instant.now())
                .build();
        when(snapshotRepository.findByRepoIdAndMetricNameAndSnapshotAtAfterOrderBySnapshotAtAsc(
                eq("liatrio/test-repo"), eq("leadTime"), any(Instant.class)))
                .thenReturn(List.of(snap));

        List<MetricSnapshot> result = snapshotService.getHistory("liatrio", "test-repo", "leadTime", 365);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMetricName()).isEqualTo("leadTime");
    }

    @Test
    void getHistory_emptyWhenNoSnapshots() {
        when(snapshotRepository.findByRepoIdAndMetricNameAndSnapshotAtAfterOrderBySnapshotAtAsc(
                anyString(), anyString(), any(Instant.class)))
                .thenReturn(List.of());

        List<MetricSnapshot> result = snapshotService.getHistory("owner", "repo", "mttr", 30);

        assertThat(result).isEmpty();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private MetricsResponse buildResponse() {
        MetricsMeta meta = new MetricsMeta("liatrio", "test-repo", 30, Instant.now());
        MetricResult deployFreq = new MetricResult(1.5, "deploys/day", DoraPerformanceBand.ELITE, true, List.of(), null);
        MetricResult leadTime   = new MetricResult(4.0, "hours",       DoraPerformanceBand.HIGH,  true, List.of(), null);
        MetricResult cfr        = new MetricResult(3.0, "%",           DoraPerformanceBand.ELITE, true, List.of(), null);
        MetricResult mttr       = new MetricResult(2.0, "hours",       DoraPerformanceBand.HIGH,  true, List.of(), null);
        return new MetricsResponse(meta, deployFreq, leadTime, cfr, mttr);
    }
}
