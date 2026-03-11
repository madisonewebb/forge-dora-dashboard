package com.liatrio.dora.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(GlobalExceptionHandlerTest.TestConfig.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rateLimitException_returns429WithJsonBody() throws Exception {
        mockMvc.perform(get("/test/rate-limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.error").value("GitHub rate limit exceeded"))
                .andExpect(jsonPath("$.resetsAt").exists());
    }

    @Test
    void illegalArgumentException_returns400WithJsonBody() throws Exception {
        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.error").value("days must be 30, 90, or 180"));
    }

    @Test
    void insightsUnavailableException_returns503WithJsonBody() throws Exception {
        mockMvc.perform(get("/test/insights-unavailable"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.error").value("AI insights unavailable"))
                .andExpect(jsonPath("$.reason").value("ANTHROPIC_API_KEY is not configured"));
    }

    @Configuration
    static class TestConfig {
        @Bean
        public TestController testController() {
            return new TestController();
        }

        @Bean
        public GlobalExceptionHandler globalExceptionHandler() {
            return new GlobalExceptionHandler();
        }
    }

    @RestController
    static class TestController {
        @GetMapping("/test/rate-limit")
        public void throwRateLimit() {
            throw new GitHubRateLimitException(Instant.parse("2026-03-09T12:00:00Z"));
        }

        @GetMapping("/test/bad-request")
        public void throwBadRequest() {
            throw new IllegalArgumentException("days must be 30, 90, or 180");
        }

        @GetMapping("/test/insights-unavailable")
        public void throwInsightsUnavailable() {
            throw new InsightsUnavailableException("ANTHROPIC_API_KEY is not configured");
        }
    }
}
