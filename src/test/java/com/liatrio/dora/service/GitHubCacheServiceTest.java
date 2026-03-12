package com.liatrio.dora.service;

import com.liatrio.dora.client.GitHubApiClient;
import com.liatrio.dora.model.*;
import com.liatrio.dora.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubCacheServiceTest {

    @Mock private GitHubApiClient apiClient;
    @Mock private GithubDeploymentRepository deploymentRepo;
    @Mock private GithubWorkflowRunRepository workflowRunRepo;
    @Mock private GithubPullRequestRepository pullRequestRepo;
    @Mock private GithubIssueRepository issueRepo;
    @Mock private GithubReleaseRepository releaseRepo;
    @Mock private GithubCacheEntryRepository cacheEntryRepo;

    private GitHubCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new GitHubCacheService(
                apiClient, deploymentRepo, workflowRunRepo,
                pullRequestRepo, issueRepo, releaseRepo, cacheEntryRepo, 15);
    }

    @Test
    void getDeployments_doesNotCallApiOnSecondRequestWithinTtl() {
        Instant now = Instant.now();
        GithubCacheEntry validEntry = GithubCacheEntry.builder()
                .repoId("owner/repo")
                .dataType("DEPLOYMENTS")
                .fetchedAt(now.minus(5, ChronoUnit.MINUTES)) // 5 min ago, TTL is 15 min
                .ttlMinutes(15)
                .build();

        when(cacheEntryRepo.findByRepoIdAndDataType("owner/repo", "DEPLOYMENTS"))
                .thenReturn(Optional.of(validEntry));
        when(deploymentRepo.findByRepoIdAndCreatedAtBetween(anyString(), any(), any()))
                .thenReturn(List.of());

        // First call — cache valid, should NOT call API
        cacheService.getDeployments("owner", "repo", "token", now.minus(30, ChronoUnit.DAYS));

        // Second call — still valid, should still NOT call API
        cacheService.getDeployments("owner", "repo", "token", now.minus(30, ChronoUnit.DAYS));

        verify(apiClient, never()).fetchDeployments(anyString(), anyString(), anyString(), any());
    }

    @Test
    void getDeployments_callsApiWhenCacheExpired() {
        Instant now = Instant.now();
        GithubCacheEntry expiredEntry = GithubCacheEntry.builder()
                .repoId("owner/repo")
                .dataType("DEPLOYMENTS")
                .fetchedAt(now.minus(20, ChronoUnit.MINUTES)) // 20 min ago, TTL is 15 min
                .ttlMinutes(15)
                .build();

        when(cacheEntryRepo.findByRepoIdAndDataType("owner/repo", "DEPLOYMENTS"))
                .thenReturn(Optional.of(expiredEntry));
        when(apiClient.fetchDeployments(anyString(), anyString(), anyString(), any()))
                .thenReturn(List.of());
        when(deploymentRepo.saveAll(any())).thenReturn(List.of());
        when(cacheEntryRepo.save(any())).thenReturn(expiredEntry);

        cacheService.getDeployments("owner", "repo", "token", now.minus(30, ChronoUnit.DAYS));

        verify(apiClient, times(1)).fetchDeployments("owner", "repo", "token",
                now.minus(30, ChronoUnit.DAYS));
    }

    @Test
    void getDeployments_callsApiWhenNoCacheEntryExists() {
        when(cacheEntryRepo.findByRepoIdAndDataType("owner/repo", "DEPLOYMENTS"))
                .thenReturn(Optional.empty());
        when(apiClient.fetchDeployments(anyString(), anyString(), anyString(), any()))
                .thenReturn(List.of());
        when(deploymentRepo.saveAll(any())).thenReturn(List.of());
        when(cacheEntryRepo.save(any())).thenReturn(mock(GithubCacheEntry.class));

        cacheService.getDeployments("owner", "repo", "token",
                Instant.now().minus(30, ChronoUnit.DAYS));

        verify(apiClient, times(1)).fetchDeployments(anyString(), anyString(), anyString(), any());
    }

    @Test
    void invalidate_deletesAllCacheEntriesAndData() {
        cacheService.invalidate("owner", "repo");

        verify(cacheEntryRepo).deleteByRepoId("owner/repo");
        verify(deploymentRepo).deleteByRepoId("owner/repo");
        verify(workflowRunRepo).deleteByRepoId("owner/repo");
        verify(pullRequestRepo).deleteByRepoId("owner/repo");
        verify(issueRepo).deleteByRepoId("owner/repo");
        verify(releaseRepo).deleteByRepoId("owner/repo");
    }
}
