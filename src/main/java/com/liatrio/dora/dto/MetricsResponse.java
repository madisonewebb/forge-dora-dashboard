package com.liatrio.dora.dto;

public record MetricsResponse(
        MetricsMeta meta,
        MetricResult deploymentFrequency,
        MetricResult leadTime,
        MetricResult changeFailureRate,
        MetricResult mttr
) {
}
