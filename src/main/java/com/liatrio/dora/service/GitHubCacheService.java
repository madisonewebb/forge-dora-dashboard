package com.liatrio.dora.service;

import com.liatrio.dora.client.GitHubApiClient;
import com.liatrio.dora.model.*;
import com.liatrio.dora.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class GitHubCacheService {

    private static final Logger log = LoggerFactory.getLogger(GitHubCacheService.class);

    private static final String TYPE_DEPLOYMENTS    = "DEPLOYMENTS";
    private static final String TYPE_WORKFLOW_RUNS  = "WORKFLOW_RUNS";
    private static final String TYPE_PULL_REQUESTS  = "PULL_REQUESTS";
    private static final String TYPE_ISSUES         = "ISSUES";
    private static final String TYPE_RELEASES       = "RELEASES";

    private final GitHubApiClient apiClient;
    private final GithubDeploymentRepository deploymentRepo;
    private final GithubWorkflowRunRepository workflowRunRepo;
    private final GithubPullRequestRepository pullRequestRepo;
    private final GithubIssueRepository issueRepo;
    private final GithubReleaseRepository releaseRepo;
    private final GithubCacheEntryRepository cacheEntryRepo;
    private final int ttlMinutes;

    public GitHubCacheService(
            GitHubApiClient apiClient,
            GithubDeploymentRepository deploymentRepo,
            GithubWorkflowRunRepository workflowRunRepo,
            GithubPullRequestRepository pullRequestRepo,
            GithubIssueRepository issueRepo,
            GithubReleaseRepository releaseRepo,
            GithubCacheEntryRepository cacheEntryRepo,
            @Value("${github.cache.ttl-minutes:15}") int ttlMinutes) {
        this.apiClient = apiClient;
        this.deploymentRepo = deploymentRepo;
        this.workflowRunRepo = workflowRunRepo;
        this.pullRequestRepo = pullRequestRepo;
        this.issueRepo = issueRepo;
        this.releaseRepo = releaseRepo;
        this.cacheEntryRepo = cacheEntryRepo;
        this.ttlMinutes = ttlMinutes;
    }

    @Transactional
    public List<GithubDeployment> getDeployments(String owner, String repo, String token, Instant windowStart) {
        String repoId = owner + "/" + repo;
        if (isCacheValid(repoId, TYPE_DEPLOYMENTS)) {
            log.debug("Cache hit for deployments: {}", repoId);
            return deploymentRepo.findByRepoIdAndCreatedAtBetween(repoId, windowStart, Instant.now());
        }
        log.debug("Cache miss for deployments: {}. Fetching from GitHub.", repoId);
        deploymentRepo.deleteByRepoId(repoId);
        List<GithubDeployment> fresh = apiClient.fetchDeployments(owner, repo, token, windowStart);
        deploymentRepo.saveAll(fresh);
        updateCacheEntry(repoId, TYPE_DEPLOYMENTS);
        return fresh;
    }

    @Transactional
    public List<GithubWorkflowRun> getWorkflowRuns(String owner, String repo, String token, Instant windowStart) {
        String repoId = owner + "/" + repo;
        if (isCacheValid(repoId, TYPE_WORKFLOW_RUNS)) {
            log.debug("Cache hit for workflow runs: {}", repoId);
            return workflowRunRepo.findByRepoIdAndHeadBranchAndCreatedAtBetween(
                    repoId, "main", windowStart, Instant.now());
        }
        log.debug("Cache miss for workflow runs: {}. Fetching from GitHub.", repoId);
        workflowRunRepo.deleteByRepoId(repoId);
        List<GithubWorkflowRun> fresh = apiClient.fetchWorkflowRuns(owner, repo, token, windowStart);
        workflowRunRepo.saveAll(fresh);
        updateCacheEntry(repoId, TYPE_WORKFLOW_RUNS);
        return fresh;
    }

    @Transactional
    public List<GithubPullRequest> getPullRequests(String owner, String repo, String token, Instant windowStart) {
        String repoId = owner + "/" + repo;
        if (isCacheValid(repoId, TYPE_PULL_REQUESTS)) {
            log.debug("Cache hit for pull requests: {}", repoId);
            return pullRequestRepo.findByRepoIdAndMergedAtBetween(repoId, windowStart, Instant.now());
        }
        log.debug("Cache miss for pull requests: {}. Fetching from GitHub.", repoId);
        pullRequestRepo.deleteByRepoId(repoId);
        List<GithubPullRequest> fresh = apiClient.fetchPullRequests(owner, repo, token, windowStart);
        pullRequestRepo.saveAll(fresh);
        updateCacheEntry(repoId, TYPE_PULL_REQUESTS);
        return fresh;
    }

    @Transactional
    public List<GithubIssue> getIssues(String owner, String repo, String token, Instant windowStart) {
        String repoId = owner + "/" + repo;
        if (isCacheValid(repoId, TYPE_ISSUES)) {
            log.debug("Cache hit for issues: {}", repoId);
            return issueRepo.findByRepoIdAndCreatedAtBetween(repoId, windowStart, Instant.now());
        }
        log.debug("Cache miss for issues: {}. Fetching from GitHub.", repoId);
        issueRepo.deleteByRepoId(repoId);
        List<GithubIssue> fresh = apiClient.fetchIssues(owner, repo, token, windowStart);
        issueRepo.saveAll(fresh);
        updateCacheEntry(repoId, TYPE_ISSUES);
        return fresh;
    }

    @Transactional
    public List<GithubRelease> getReleases(String owner, String repo, String token, Instant windowStart) {
        String repoId = owner + "/" + repo;
        if (isCacheValid(repoId, TYPE_RELEASES)) {
            log.debug("Cache hit for releases: {}", repoId);
            return releaseRepo.findByRepoIdAndPublishedAtBetween(repoId, windowStart, Instant.now());
        }
        log.debug("Cache miss for releases: {}. Fetching from GitHub.", repoId);
        releaseRepo.deleteByRepoId(repoId);
        List<GithubRelease> fresh = apiClient.fetchReleases(owner, repo, token, windowStart);
        releaseRepo.saveAll(fresh);
        updateCacheEntry(repoId, TYPE_RELEASES);
        return fresh;
    }

    @Transactional
    public void invalidate(String owner, String repo) {
        String repoId = owner + "/" + repo;
        log.info("Invalidating cache for: {}", repoId);
        cacheEntryRepo.deleteByRepoId(repoId);
        deploymentRepo.deleteByRepoId(repoId);
        workflowRunRepo.deleteByRepoId(repoId);
        pullRequestRepo.deleteByRepoId(repoId);
        issueRepo.deleteByRepoId(repoId);
        releaseRepo.deleteByRepoId(repoId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean isCacheValid(String repoId, String dataType) {
        Optional<GithubCacheEntry> entry = cacheEntryRepo.findByRepoIdAndDataType(repoId, dataType);
        return entry.isPresent() && !entry.get().isExpired();
    }

    private void updateCacheEntry(String repoId, String dataType) {
        GithubCacheEntry entry = cacheEntryRepo
                .findByRepoIdAndDataType(repoId, dataType)
                .orElse(GithubCacheEntry.builder()
                        .repoId(repoId)
                        .dataType(dataType)
                        .ttlMinutes(ttlMinutes)
                        .build());
        entry.setFetchedAt(Instant.now());
        entry.setTtlMinutes(ttlMinutes);
        cacheEntryRepo.save(entry);
    }
}
