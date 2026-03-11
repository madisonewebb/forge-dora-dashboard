package com.liatrio.dora.controller;

import com.liatrio.dora.exception.InsightsUnavailableException;
import com.liatrio.dora.service.InsightsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InsightsController.class)
class InsightsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InsightsService insightsService;

    @Test
    void getInsights_returnsTextEventStream() throws Exception {
        when(insightsService.streamInsights(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Flux.just("tok1", "tok2"));

        MvcResult result = mockMvc.perform(get("/api/insights")
                        .param("owner", "liatrio")
                        .param("repo", "liatrio")
                        .param("token", "abc")
                        .param("days", "30"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }

    @Test
    void getInsights_emitsMultipleTokens() throws Exception {
        when(insightsService.streamInsights(anyString(), anyString(), anyString(), anyInt()))
                .thenReturn(Flux.just("alpha", "beta", "gamma"));

        MvcResult result = mockMvc.perform(get("/api/insights")
                        .param("owner", "liatrio")
                        .param("repo", "liatrio")
                        .param("token", "abc")
                        .param("days", "30"))
                .andExpect(request().asyncStarted())
                .andReturn();

        String body = mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("alpha").contains("beta").contains("gamma");
    }

    @Test
    void getInsights_insightsUnavailable_returns503() throws Exception {
        when(insightsService.streamInsights(anyString(), anyString(), anyString(), anyInt()))
                .thenThrow(new InsightsUnavailableException("no key"));

        mockMvc.perform(get("/api/insights")
                        .param("owner", "liatrio")
                        .param("repo", "liatrio")
                        .param("token", "abc")
                        .param("days", "30"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("AI insights unavailable"))
                .andExpect(jsonPath("$.reason").value("no key"));
    }
}
