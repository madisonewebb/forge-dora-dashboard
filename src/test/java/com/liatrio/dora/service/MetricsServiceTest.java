package com.liatrio.dora.service;

import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.MetricsResponse;
import com.liatrio.dora.metrics.ChangeFailureRateCalculator;
import com.liatrio.dora.metrics.DeploymentFrequencyCalculator;
import com.liatrio.dora.metrics.LeadTimeCalculator;
import com.liatrio.dora.metrics.MttrCalculator;
import com.liatrio.dora.model.GithubDeployment;
import com.liatrio.dora.model.GithubIssue;
import com.liatrio.dora.model.GithubPullRequest;
import com.liatrio.dora.model.GithubWorkflowRun;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MetricsServiceTest {

    @Mock private GitHubCacheService cacheService;
    @Mock private DeploymentFrequencyCalculator deploymentFrequencyCalculator;
    @Mock private LeadTimeCalculator leadTimeCalculator;
    @Mock private ChangeFailureRateCalculator changeFailureRateCalculator;
    @Mock private MttrCalculator mttrCalculator;

    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        metricsService = new MetricsService(
                cacheService,
                deploymentFrequencyCalculator,
                leadTimeCalculator,
                changeFailureRateCalculator,
                mttrCalculator);
    }

    @Test
    void getMetrics_callsAllFourCalculators() {
        stubCacheService();
        stubAllCalculators();

        metricsService.getMetrics("owner", "repo", "token", 30);

        verify(deploymentFrequencyCalculator, times(1)).calculate(anyList(), eq(30));
        verify(leadTimeCalculator, times(1)).calculate(anyList(), anyList(), eq(30));
        verify(changeFailureRateCalculator, times(1)).calculate(anyList(), anyList(), anyList(), anyList(), eq(30));
        verify(mttrCalculator, times(1)).calculate(anyList(), anyList(), eq(30));
    }

    @Test
    void getMetrics_assemblesToMetricsResponse() {
        stubCacheService();

        MetricResult deployFreqResult = MetricResult.notAvailable("no deploys");
        MetricResult leadTimeResult = MetricResult.notAvailable("no lt");
        MetricResult cfrResult = MetricResult.notAvailable("no cfr");
        MetricResult mttrResult = MetricResult.notAvailable("no mttr");

        when(deploymentFrequencyCalculator.calculate(anyList(), anyInt())).thenReturn(deployFreqResult);
        when(leadTimeCalculator.calculate(anyList(), anyList(), anyInt())).thenReturn(leadTimeResult);
        when(changeFailureRateCalculator.calculate(anyList(), anyList(), anyList(), anyList(), anyInt())).thenReturn(cfrResult);
        when(mttrCalculator.calculate(anyList(), anyList(), anyInt())).thenReturn(mttrResult);

        MetricsResponse response = metricsService.getMetrics("owner", "repo", "token", 30);

        assertNotNull(response);
        assertEquals("owner", response.meta().owner());
        assertEquals("repo", response.meta().repo());
        assertEquals(30, response.meta().windowDays());
        assertNotNull(response.meta().generatedAt());
        assertSame(deployFreqResult, response.deploymentFrequency());
        assertSame(leadTimeResult, response.leadTime());
        assertSame(cfrResult, response.changeFailureRate());
        assertSame(mttrResult, response.mttr());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void stubCacheService() {
        when(cacheService.getDeployments(anyString(), anyString(), anyString(), any(Instant.class)))
                .thenReturn(List.of());
        when(cacheService.getWorkflowRuns(anyString(), anyString(), anyString(), any(Instant.class)))
                .thenReturn(List.of());
        when(cacheService.getPullRequests(anyString(), anyString(), anyString(), any(Instant.class)))
                .thenReturn(List.of());
        when(cacheService.getIssues(anyString(), anyString(), anyString(), any(Instant.class)))
                .thenReturn(List.of());
    }

    private void stubAllCalculators() {
        when(deploymentFrequencyCalculator.calculate(anyList(), anyInt()))
                .thenReturn(MetricResult.notAvailable("test"));
        when(leadTimeCalculator.calculate(anyList(), anyList(), anyInt()))
                .thenReturn(MetricResult.notAvailable("test"));
        when(changeFailureRateCalculator.calculate(anyList(), anyList(), anyList(), anyList(), anyInt()))
                .thenReturn(MetricResult.notAvailable("test"));
        when(mttrCalculator.calculate(anyList(), anyList(), anyInt()))
                .thenReturn(MetricResult.notAvailable("test"));
    }
}
