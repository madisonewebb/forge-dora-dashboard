package com.liatrio.dora.service;

import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.MetricsResponse;
import com.liatrio.dora.exception.InsightsUnavailableException;
import com.liatrio.dora.insights.BuiltPrompt;
import com.liatrio.dora.insights.PromptBuilder;
import com.liatrio.dora.insights.TrendDirection;
import com.liatrio.dora.insights.TrendDirectionCalculator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

@Service
public class InsightsService {

    private static final Logger log = LoggerFactory.getLogger(InsightsService.class);

    private final MetricsService metricsService;
    private final TrendDirectionCalculator trendDirectionCalculator;
    private final PromptBuilder promptBuilder;
    private final ChatClient chatClient;
    private final String anthropicApiKey;

    public InsightsService(MetricsService metricsService,
                           TrendDirectionCalculator trendDirectionCalculator,
                           PromptBuilder promptBuilder,
                           ChatClient chatClient,
                           @Value("${spring.ai.anthropic.api-key:}") String anthropicApiKey) {
        this.metricsService = metricsService;
        this.trendDirectionCalculator = trendDirectionCalculator;
        this.promptBuilder = promptBuilder;
        this.chatClient = chatClient;
        this.anthropicApiKey = anthropicApiKey;
    }

    public Flux<String> streamInsights(String owner, String repo, String token, int days) {
        if (anthropicApiKey == null || anthropicApiKey.isBlank()) {
            throw new InsightsUnavailableException("ANTHROPIC_API_KEY is not configured");
        }

        log.info("Streaming AI insights for {}/{} over {} days", owner, repo, days);

        MetricsResponse metrics = metricsService.getMetrics(owner, repo, token, days);

        Map<String, TrendDirection> trends = Map.of(
                "deploymentFrequency", trendDirection(metrics.deploymentFrequency(), false),
                "leadTime", trendDirection(metrics.leadTime(), true),
                "changeFailureRate", trendDirection(metrics.changeFailureRate(), true),
                "mttr", trendDirection(metrics.mttr(), true)
        );

        BuiltPrompt builtPrompt = promptBuilder.build(metrics, trends);

        return chatClient.prompt()
                .system(builtPrompt.systemPrompt())
                .user(builtPrompt.userPrompt())
                .stream()
                .content()
                .retry(1)
                .onErrorMap(ex -> new InsightsUnavailableException("Claude API error: " + ex.getMessage()));
    }

    private TrendDirection trendDirection(MetricResult result, boolean lowerIsBetter) {
        if (!result.dataAvailable()) {
            return TrendDirection.STABLE;
        }
        return trendDirectionCalculator.calculate(result.timeSeries(), lowerIsBetter);
    }
}
