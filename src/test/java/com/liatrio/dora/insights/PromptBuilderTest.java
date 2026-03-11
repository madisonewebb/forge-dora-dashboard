package com.liatrio.dora.insights;

import com.liatrio.dora.dto.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    private final PromptBuilder promptBuilder = new PromptBuilder();

    private MetricsResponse buildFixtureResponse() {
        MetricsMeta meta = new MetricsMeta("liatrio", "liatrio", 30, Instant.now());

        MetricResult deployFreq = new MetricResult(2.3, "deploys/day", DoraPerformanceBand.ELITE, true, List.of(), null);
        MetricResult leadTime = new MetricResult(6.2, "hours", DoraPerformanceBand.HIGH, true, List.of(), null);
        MetricResult cfr = MetricResult.notAvailable("No CFR data");
        MetricResult mttr = MetricResult.notAvailable("No MTTR data");

        return new MetricsResponse(meta, deployFreq, leadTime, cfr, mttr);
    }

    private Map<String, TrendDirection> buildFixtureTrends() {
        return Map.of(
                "deploymentFrequency", TrendDirection.IMPROVING,
                "leadTime", TrendDirection.STABLE,
                "changeFailureRate", TrendDirection.STABLE,
                "mttr", TrendDirection.STABLE
        );
    }

    @Test
    void buildPrompt_systemPrompt_containsAdvisorInstruction() {
        BuiltPrompt prompt = promptBuilder.build(buildFixtureResponse(), buildFixtureTrends());
        assertTrue(prompt.systemPrompt().contains("DevOps"), "System prompt should mention DevOps");
        assertTrue(prompt.systemPrompt().contains("DORA"), "System prompt should mention DORA");
    }

    @Test
    void buildPrompt_userPrompt_containsMetricValues() {
        BuiltPrompt prompt = promptBuilder.build(buildFixtureResponse(), buildFixtureTrends());
        assertTrue(prompt.userPrompt().contains("2.3"), "User prompt should contain deploymentFrequency value");
        assertTrue(prompt.userPrompt().contains("6.2"), "User prompt should contain leadTime value");
    }

    @Test
    void buildPrompt_userPrompt_containsBandAndTrend() {
        BuiltPrompt prompt = promptBuilder.build(buildFixtureResponse(), buildFixtureTrends());
        assertTrue(prompt.userPrompt().contains("ELITE"), "User prompt should contain ELITE band");
        assertTrue(prompt.userPrompt().contains("IMPROVING"), "User prompt should contain IMPROVING trend");
    }

    @Test
    void buildPrompt_userPrompt_containsSectionHeaders() {
        BuiltPrompt prompt = promptBuilder.build(buildFixtureResponse(), buildFixtureTrends());
        assertTrue(prompt.userPrompt().contains("## Summary"), "User prompt should request ## Summary section");
        assertTrue(prompt.userPrompt().contains("## Trend Analysis"), "User prompt should request ## Trend Analysis section");
        assertTrue(prompt.userPrompt().contains("## Recommendations"), "User prompt should request ## Recommendations section");
    }
}
