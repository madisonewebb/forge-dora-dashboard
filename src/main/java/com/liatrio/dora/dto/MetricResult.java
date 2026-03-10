package com.liatrio.dora.dto;

import java.util.List;

public record MetricResult(
        Double value,
        String unit,
        DoraPerformanceBand band,
        boolean dataAvailable,
        List<WeekDataPoint> timeSeries,
        String message
) {
    public static MetricResult notAvailable(String message) {
        return new MetricResult(null, null, null, false, List.of(), message);
    }
}
