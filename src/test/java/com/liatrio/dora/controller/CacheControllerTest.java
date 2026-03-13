package com.liatrio.dora.controller;

import com.liatrio.dora.service.GitHubCacheService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CacheController.class)
class CacheControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GitHubCacheService cacheService;

    @Test
    void deleteCache_returns204AndCallsInvalidate() throws Exception {
        mockMvc.perform(delete("/api/cache/octocat/Hello-World")
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent());

        verify(cacheService).invalidate("octocat", "Hello-World");
    }

    @Test
    void deleteCache_returns401WhenNoAuth() throws Exception {
        mockMvc.perform(delete("/api/cache/octocat/Hello-World"))
                .andExpect(status().isUnauthorized());
    }
}
