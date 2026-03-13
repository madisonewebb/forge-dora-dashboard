package com.liatrio.dora.service;

import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.MetricsResponse;
import com.liatrio.dora.dto.WeekDataPoint;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class CsvExportService {

    /** Wraps a field in double-quotes and escapes any embedded double-quotes per RFC 4180. */
    private static String csvField(String value) {
        if (value == null) return "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public byte[] generateCsv(MetricsResponse response) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

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
                    csvField(entry.getKey()),
                    r.value() != null ? r.value().toString() : "",
                    csvField(r.unit()),
                    csvField(r.band() != null ? r.band().name() : null),
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
                            csvField(entry.getKey()),
                            point.weekStart(),
                            point.value() != null ? point.value().toString() : "");
                }
            }
        }

        writer.flush();
        return baos.toByteArray();
    }
}
