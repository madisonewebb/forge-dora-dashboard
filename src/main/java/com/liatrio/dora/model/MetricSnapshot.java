package com.liatrio.dora.model;

import com.liatrio.dora.dto.DoraPerformanceBand;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "metric_snapshots")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "repo_id", nullable = false)
    private String repoId;

    @Column(name = "metric_name", nullable = false)
    private String metricName;

    @Column(name = "window_days", nullable = false)
    private Integer windowDays;

    @Column(name = "metric_value")
    private Double value;

    @Column(name = "unit")
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "band")
    private DoraPerformanceBand band;

    @Column(name = "data_available")
    private Boolean dataAvailable;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;

    @Column(name = "window_start")
    private Instant windowStart;

    @Column(name = "window_end")
    private Instant windowEnd;
}
