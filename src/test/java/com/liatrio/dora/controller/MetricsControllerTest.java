package com.liatrio.dora.controller;

import com.liatrio.dora.dto.DoraPerformanceBand;
import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.MetricsMeta;
import com.liatrio.dora.dto.MetricsResponse;
import com.liatrio.dora.model.MetricSnapshot;
import com.liatrio.dora.service.MetricSnapshotService;
import com.liatrio.dora.service.MetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MetricsController.class)
class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetricsService metricsService;

    @MockBean
    private MetricSnapshotService snapshotService;

    @Test
    void getMetrics_returns200WithCorrectShape() throws Exception {
        MetricsMeta meta = new MetricsMeta("liatrio", "liatrio", 30, Instant.now());
        MetricResult notAvailable = MetricResult.notAvailable("no data");
        MetricsResponse response = new MetricsResponse(meta, notAvailable, notAvailable, notAvailable, notAvailable);

        when(metricsService.getMetrics(eq("liatrio"), eq("liatrio"), anyString(), eq(30)))
                .thenReturn(response);

        mockMvc.perform(get("/api/metrics")
                        .param("owner", "liatrio")
                        .param("repo", "liatrio")
                        .header("Authorization", "Bearer test-token")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.owner").value("liatrio"))
                .andExpect(jsonPath("$.meta.repo").value("liatrio"))
                .andExpect(jsonPath("$.meta.windowDays").value(30))
                .andExpect(jsonPath("$.deploymentFrequency").exists())
                .andExpect(jsonPath("$.leadTime").exists())
                .andExpect(jsonPath("$.changeFailureRate").exists())
                .andExpect(jsonPath("$.mttr").exists());
    }

    @Test
    void getMetrics_invalidDays_returns400() throws Exception {
        mockMvc.perform(get("/api/metrics")
                        .param("owner", "liatrio")
                        .param("repo", "liatrio")
                        .header("Authorization", "Bearer test-token")
                        .param("days", "999"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMetrics_defaultDays_uses30() throws Exception {
        MetricsMeta meta = new MetricsMeta("liatrio", "liatrio", 30, Instant.now());
        MetricResult notAvailable = MetricResult.notAvailable("no data");
        MetricsResponse response = new MetricsResponse(meta, notAvailable, notAvailable, notAvailable, notAvailable);

        when(metricsService.getMetrics(anyString(), anyString(), anyString(), eq(30)))
                .thenReturn(response);

        mockMvc.perform(get("/api/metrics")
                        .param("owner", "liatrio")
                        .param("repo", "liatrio")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk());

        verify(metricsService).getMetrics(eq("liatrio"), eq("liatrio"), anyString(), eq(30));
    }

    @Test
    void getMetrics_days90_isValid() throws Exception {
        MetricsMeta meta = new MetricsMeta("liatrio", "liatrio", 90, Instant.now());
        MetricResult notAvailable = MetricResult.notAvailable("no data");
        MetricsResponse response = new MetricsResponse(meta, notAvailable, notAvailable, notAvailable, notAvailable);

        when(metricsService.getMetrics(anyString(), anyString(), anyString(), eq(90)))
                .thenReturn(response);

        mockMvc.perform(get("/api/metrics")
                        .param("owner", "liatrio")
                        .param("repo", "liatrio")
                        .header("Authorization", "Bearer test-token")
                        .param("days", "90"))
                .andExpect(status().isOk());
    }

    @Test
    void getHistory_returns200WithExpectedStructure() throws Exception {
        Instant now = Instant.now();
        MetricSnapshot snap = MetricSnapshot.builder()
                .id(1L)
                .repoId("liatrio/test-repo")
                .metricName("deploymentFrequency")
                .windowDays(30)
                .value(1.5)
                .unit("deploys/day")
                .band(DoraPerformanceBand.ELITE)
                .dataAvailable(true)
                .snapshotAt(now)
                .windowStart(now.minusSeconds(30L * 24 * 3600))
                .windowEnd(now)
                .build();

        when(snapshotService.getHistory(eq("liatrio"), eq("test-repo"), eq("deploymentFrequency"), eq(365)))
                .thenReturn(List.of(snap));

        mockMvc.perform(get("/api/metrics/history")
                        .param("owner", "liatrio")
                        .param("repo", "test-repo")
                        .param("metric", "deploymentFrequency")
                        .param("lookbackDays", "365"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricName").value("deploymentFrequency"))
                .andExpect(jsonPath("$.repoId").value("liatrio/test-repo"))
                .andExpect(jsonPath("$.snapshots").isArray())
                .andExpect(jsonPath("$.snapshots[0].metricName").value("deploymentFrequency"))
                .andExpect(jsonPath("$.snapshots[0].value").value(1.5))
                .andExpect(jsonPath("$.snapshots[0].band").value("ELITE"));
    }

    @Test
    void getHistory_defaultLookback_uses365() throws Exception {
        when(snapshotService.getHistory(anyString(), anyString(), anyString(), eq(365)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/metrics/history")
                        .param("owner", "liatrio")
                        .param("repo", "test-repo")
                        .param("metric", "leadTime"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshots").isArray());

        verify(snapshotService).getHistory(eq("liatrio"), eq("test-repo"), eq("leadTime"), eq(365));
    }

    @Test
    void getHistory_emptySnapshots_returns200WithEmptyList() throws Exception {
        when(snapshotService.getHistory(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/metrics/history")
                        .param("owner", "liatrio")
                        .param("repo", "test-repo")
                        .param("metric", "mttr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshots").isEmpty());
    }
}
