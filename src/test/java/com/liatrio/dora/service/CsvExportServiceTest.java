package com.liatrio.dora.service;

import com.liatrio.dora.dto.DoraPerformanceBand;
import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.MetricsMeta;
import com.liatrio.dora.dto.MetricsResponse;
import com.liatrio.dora.dto.WeekDataPoint;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportServiceTest {

    private final CsvExportService service = new CsvExportService();

    @Test
    void generateCsv_containsHeaderRow() {
        byte[] csv = service.generateCsv(buildResponse());
        String content = new String(csv, StandardCharsets.UTF_8);
        assertThat(content).contains("metric,value,unit,band,dataAvailable,windowDays");
    }

    @Test
    void generateCsv_containsAllFourMetrics() {
        byte[] csv = service.generateCsv(buildResponse());
        String content = new String(csv, StandardCharsets.UTF_8);
        assertThat(content).contains("deploymentFrequency");
        assertThat(content).contains("leadTime");
        assertThat(content).contains("changeFailureRate");
        assertThat(content).contains("mttr");
    }

    @Test
    void generateCsv_containsTimeseriesSection() {
        byte[] csv = service.generateCsv(buildResponse());
        String content = new String(csv, StandardCharsets.UTF_8);
        assertThat(content).contains("metric,weekStart,value");
    }

    @Test
    void generateCsv_timeseriesRowsPresent() {
        LocalDate weekStart = LocalDate.of(2026, 3, 1);
        MetricResult resultWithSeries = new MetricResult(
                2.0, "deploys/day", DoraPerformanceBand.HIGH, true,
                List.of(new WeekDataPoint(weekStart, 2.0)), null);

        MetricsMeta meta = new MetricsMeta("o", "r", 30, Instant.now());
        MetricResult noData = MetricResult.notAvailable("no data");
        MetricsResponse response = new MetricsResponse(meta, resultWithSeries, noData, noData, noData);

        byte[] csv = service.generateCsv(response);
        String content = new String(csv, StandardCharsets.UTF_8);
        assertThat(content).contains("\"deploymentFrequency\",2026-03-01,2.0");
    }

    @Test
    void generateCsv_isUtf8Encoded() {
        // Write a metric with a multi-byte UTF-8 unit (µ = U+00B5, 2 bytes in UTF-8)
        LocalDate weekStart = LocalDate.of(2026, 3, 1);
        MetricResult resultWithUnit = new MetricResult(
                1.0, "µs", DoraPerformanceBand.ELITE, true,
                List.of(new WeekDataPoint(weekStart, 1.0)), null);
        MetricsMeta meta = new MetricsMeta("o", "r", 30, Instant.now());
        MetricResult noData = MetricResult.notAvailable(null);
        MetricsResponse response = new MetricsResponse(meta, resultWithUnit, noData, noData, noData);

        byte[] csv = service.generateCsv(response);

        // µ is U+00B5 → UTF-8 bytes 0xC2 0xB5; verify the raw bytes contain this sequence
        String content = new String(csv, StandardCharsets.UTF_8);
        assertThat(content).contains("µs");
        // Confirm the bytes are UTF-8: re-decoding as UTF-8 must reproduce the same string
        assertThat(new String(csv, StandardCharsets.UTF_8)).isEqualTo(content);
        // Decoding as Latin-1 would give a different result (µ would be two garbage chars)
        assertThat(new String(csv, java.nio.charset.StandardCharsets.ISO_8859_1)).isNotEqualTo(content);
    }

    @Test
    void generateCsv_notAvailableMetric_hasEmptyValueAndBand() {
        MetricsMeta meta = new MetricsMeta("o", "r", 30, Instant.now());
        MetricResult noData = MetricResult.notAvailable("no data");
        MetricsResponse response = new MetricsResponse(meta, noData, noData, noData, noData);

        byte[] csv = service.generateCsv(response);
        String content = new String(csv, StandardCharsets.UTF_8);
        // value and band columns should be empty for unavailable metrics
        assertThat(content).contains("\"deploymentFrequency\",,,");
    }

    private MetricsResponse buildResponse() {
        MetricsMeta meta = new MetricsMeta("liatrio", "test-repo", 30, Instant.now());
        MetricResult r = new MetricResult(1.5, "deploys/day", DoraPerformanceBand.ELITE, true, List.of(), null);
        return new MetricsResponse(meta, r, r, r, r);
    }
}
