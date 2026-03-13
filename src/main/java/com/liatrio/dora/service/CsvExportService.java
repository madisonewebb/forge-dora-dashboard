package com.liatrio.dora.service;

import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.MetricsResponse;
import com.liatrio.dora.dto.WeekDataPoint;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CsvExportService {

    public byte[] generateCsv(MetricsResponse response) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(baos);

        // Summary section
        writer.println("metric,value,unit,band,dataAvailable,windowDays");
        Map<String, MetricResult> metrics = new LinkedHashMap<>();
        metrics.put("deploymentFrequency", response.deploymentFrequency());
        metrics.put("leadTime", response.leadTime());
        metrics.put("changeFailureRate", response.changeFailureRate());
        metrics.put("mttr", response.mttr());

        int windowDays = response.meta().windowDays();
        for (Map.Entry<String, MetricResult> entry : metrics.entrySet()) {
            MetricResult r = entry.getValue();
            writer.printf("%s,%s,%s,%s,%s,%d%n",
                    entry.getKey(),
                    r.value() != null ? r.value().toString() : "",
                    r.unit() != null ? r.unit() : "",
                    r.band() != null ? r.band().name() : "",
                    r.dataAvailable(),
                    windowDays);
        }

        // Timeseries section
        writer.println();
        writer.println("metric,weekStart,value");
        for (Map.Entry<String, MetricResult> entry : metrics.entrySet()) {
            MetricResult r = entry.getValue();
            if (r.timeSeries() != null) {
                for (WeekDataPoint point : r.timeSeries()) {
                    writer.printf("%s,%s,%s%n",
                            entry.getKey(),
                            point.weekStart(),
                            point.value() != null ? point.value().toString() : "");
                }
            }
        }

        writer.flush();
        return baos.toByteArray();
    }
}
