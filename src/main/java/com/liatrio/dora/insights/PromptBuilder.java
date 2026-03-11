package com.liatrio.dora.insights;

import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.MetricsResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT =
            "You are a DevOps engineering effectiveness advisor with deep expertise in DORA metrics " +
            "(Deployment Frequency, Lead Time for Changes, Change Failure Rate, and Mean Time to Restore). " +
            "You analyze real metric data and provide specific, grounded insights — never generic advice. " +
            "Reference the actual values and DORA bands provided. Be concise and actionable.";

    public BuiltPrompt build(MetricsResponse metrics, Map<String, TrendDirection> trends) {
        String owner = metrics.meta().owner();
        String repo = metrics.meta().repo();
        int days = metrics.meta().windowDays();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "Here are the current DORA metrics for %s/%s over the last %d days:%n%n",
                owner, repo, days));

        appendMetric(sb, "Deployment Frequency", metrics.deploymentFrequency(),
                trends.get("deploymentFrequency"),
                "Elite = ≥1/day, High = ≥1/week, Medium = ≥1/month, Low = <1/month");

        appendMetric(sb, "Lead Time for Changes", metrics.leadTime(),
                trends.get("leadTime"),
                "Elite = <1h, High = 1–24h, Medium = 1–7 days, Low = >1 week");

        appendMetric(sb, "Change Failure Rate", metrics.changeFailureRate(),
                trends.get("changeFailureRate"),
                "Elite = 0–5%, High = 5–10%, Medium = 10–15%, Low = >15%");

        appendMetric(sb, "Mean Time to Restore (MTTR)", metrics.mttr(),
                trends.get("mttr"),
                "Elite = <1h, High = 1–24h, Medium = 1–7 days, Low = >1 week");

        sb.append("Please analyze these metrics and respond with exactly three sections:\n");
        sb.append("## Summary\n");
        sb.append("## Trend Analysis\n");
        sb.append("## Recommendations\n\n");
        sb.append("In ## Recommendations, provide 3–5 specific, actionable bullet points referencing " +
                "the actual metric values and bands above. Do not give generic DevOps advice.");

        return new BuiltPrompt(SYSTEM_PROMPT, sb.toString());
    }

    private void appendMetric(StringBuilder sb, String name, MetricResult result,
                               TrendDirection trend, String benchmarks) {
        sb.append("Metric: ").append(name).append("\n");
        if (result.dataAvailable()) {
            sb.append(String.format("Value: %s %s | Band: %s | Trend: %s%n",
                    formatValue(result.value()), result.unit(), result.band(), trend));
            sb.append("DORA Benchmark: ").append(benchmarks).append("\n");
        } else {
            sb.append("Status: No data available\n");
        }
        sb.append("\n");
    }

    private String formatValue(Double value) {
        if (value == null) return "N/A";
        if (value == Math.floor(value)) {
            return String.valueOf(value.intValue());
        }
        return String.format("%.1f", value);
    }
}
