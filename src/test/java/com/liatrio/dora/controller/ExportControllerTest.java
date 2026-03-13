package com.liatrio.dora.controller;

import com.liatrio.dora.dto.DoraPerformanceBand;
import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.MetricsMeta;
import com.liatrio.dora.dto.MetricsResponse;
import com.liatrio.dora.service.CsvExportService;
import com.liatrio.dora.service.MetricsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExportController.class)
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetricsService metricsService;

    @MockBean
    private CsvExportService csvExportService;

    @Test
    void exportCsv_happyPath_returns200WithCsvContent() throws Exception {
        MetricsMeta meta = new MetricsMeta("octocat", "hello-world", 30, Instant.now());
        MetricResult result = new MetricResult(1.5, "deploys/day", DoraPerformanceBand.ELITE, true, List.of(), null);
        MetricsResponse response = new MetricsResponse(meta, result, result, result, result);

        when(metricsService.getMetrics(eq("octocat"), eq("hello-world"), anyString(), eq(30)))
                .thenReturn(response);
        when(csvExportService.generateCsv(any(MetricsResponse.class)))
                .thenReturn("metric,value\ndeploymentFrequency,1.5\n".getBytes());

        mockMvc.perform(get("/api/export/csv")
                        .param("owner", "octocat")
                        .param("repo", "hello-world")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("dora-octocat-hello-world")));
    }

    @Test
    void exportCsv_missingAuthHeader_returns400() throws Exception {
        // Missing Authorization header — Spring MVC returns 400 for required header
        mockMvc.perform(get("/api/export/csv")
                        .param("owner", "octocat")
                        .param("repo", "hello-world"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportCsv_daysTooLarge_returns400() throws Exception {
        mockMvc.perform(get("/api/export/csv")
                        .param("owner", "octocat")
                        .param("repo", "hello-world")
                        .header("Authorization", "Bearer test-token")
                        .param("days", "366"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportCsv_daysTooSmall_returns400() throws Exception {
        mockMvc.perform(get("/api/export/csv")
                        .param("owner", "octocat")
                        .param("repo", "hello-world")
                        .header("Authorization", "Bearer test-token")
                        .param("days", "6"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportCsv_unsafeOwnerAndRepo_sanitizedInFilename() throws Exception {
        MetricsMeta meta = new MetricsMeta("bad\"owner", "bad\r\nrepo", 30, Instant.now());
        MetricResult result = MetricResult.notAvailable("no data");
        MetricsResponse response = new MetricsResponse(meta, result, result, result, result);

        when(metricsService.getMetrics(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(response);
        when(csvExportService.generateCsv(any())).thenReturn(new byte[0]);

        mockMvc.perform(get("/api/export/csv")
                        .param("owner", "bad\"owner")
                        .param("repo", "bad\r\nrepo")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                // unsafe characters replaced with '_'
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("bad_owner")))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("bad__repo")))
                // header value must not contain raw quote or CRLF after sanitization
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("bad\"owner"))))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("bad\r\nrepo"))));
    }
}
