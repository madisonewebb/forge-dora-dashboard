package com.liatrio.dora.dto;

import com.liatrio.dora.model.MetricSnapshot;

import java.util.List;

public record MetricHistoryResponse(String metricName, String repoId, List<MetricSnapshot> snapshots) {
}
