package com.liatrio.dora.controller;

import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.MetricsMeta;
import com.liatrio.dora.dto.MetricsResponse;
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
}
