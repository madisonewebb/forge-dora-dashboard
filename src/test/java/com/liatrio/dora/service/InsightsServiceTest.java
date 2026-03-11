package com.liatrio.dora.service;

import com.liatrio.dora.dto.*;
import com.liatrio.dora.exception.InsightsUnavailableException;
import com.liatrio.dora.insights.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsightsServiceTest {

    @Mock
    private MetricsService metricsService;

    @Mock
    private TrendDirectionCalculator trendDirectionCalculator;

    @Mock
    private PromptBuilder promptBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.StreamResponseSpec streamResponseSpec;

    private MetricsResponse buildFixtureResponse() {
        MetricsMeta meta = new MetricsMeta("liatrio", "liatrio", 30, Instant.now());
        MetricResult available = new MetricResult(2.3, "deploys/day", DoraPerformanceBand.ELITE, true, List.of(), null);
        MetricResult notAvailable = MetricResult.notAvailable("no data");
        return new MetricsResponse(meta, available, notAvailable, notAvailable, notAvailable);
    }

    private void setupChatClientMock(Flux<String> flux) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(flux);
    }

    @Test
    void streamInsights_callsMetricsServiceAndBuildsPrompt() {
        MetricsResponse fixture = buildFixtureResponse();
        when(metricsService.getMetrics(anyString(), anyString(), anyString(), anyInt())).thenReturn(fixture);
        when(trendDirectionCalculator.calculate(any())).thenReturn(TrendDirection.STABLE);
        when(promptBuilder.build(any(), any())).thenReturn(new BuiltPrompt("sys", "user"));
        setupChatClientMock(Flux.just("Hello", " world"));

        InsightsService service = new InsightsService(
                metricsService, trendDirectionCalculator, promptBuilder, chatClient, "test-key");

        Flux<String> result = service.streamInsights("liatrio", "liatrio", "token", 30);

        StepVerifier.create(result)
                .expectNext("Hello")
                .expectNext(" world")
                .verifyComplete();

        verify(metricsService).getMetrics("liatrio", "liatrio", "token", 30);
        verify(promptBuilder).build(eq(fixture), any());
    }

    @Test
    void streamInsights_promptContainsMetricData() {
        MetricsResponse fixture = buildFixtureResponse();
        when(metricsService.getMetrics(anyString(), anyString(), anyString(), anyInt())).thenReturn(fixture);
        when(trendDirectionCalculator.calculate(any())).thenReturn(TrendDirection.STABLE);

        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        when(promptBuilder.build(any(), any())).thenAnswer(inv -> {
            MetricsResponse m = inv.getArgument(0);
            String userContent = "Deployment Frequency: " + m.deploymentFrequency().value();
            return new BuiltPrompt("sys", userContent);
        });
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(userPromptCaptor.capture())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamResponseSpec);
        when(streamResponseSpec.content()).thenReturn(Flux.just("ok"));

        InsightsService service = new InsightsService(
                metricsService, trendDirectionCalculator, promptBuilder, chatClient, "test-key");

        StepVerifier.create(service.streamInsights("liatrio", "liatrio", "token", 30))
                .expectNext("ok")
                .verifyComplete();

        assertThat(userPromptCaptor.getValue()).contains("2.3");
    }

    @Test
    void streamInsights_missingApiKey_throwsInsightsUnavailableException() {
        InsightsService service = new InsightsService(
                metricsService, trendDirectionCalculator, promptBuilder, chatClient, "");

        assertThatThrownBy(() -> service.streamInsights("liatrio", "liatrio", "token", 30))
                .isInstanceOf(InsightsUnavailableException.class)
                .hasMessageContaining("ANTHROPIC_API_KEY");
    }
}
