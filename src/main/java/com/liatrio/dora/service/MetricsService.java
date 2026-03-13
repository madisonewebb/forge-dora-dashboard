package com.liatrio.dora.service;

import com.liatrio.dora.dto.MetricResult;
import com.liatrio.dora.dto.MetricsMeta;
import com.liatrio.dora.dto.MetricsResponse;
import com.liatrio.dora.metrics.ChangeFailureRateCalculator;
import com.liatrio.dora.metrics.DeploymentFrequencyCalculator;
import com.liatrio.dora.metrics.LeadTimeCalculator;
import com.liatrio.dora.metrics.MttrCalculator;
import com.liatrio.dora.model.GithubDeployment;
import com.liatrio.dora.model.GithubIssue;
import com.liatrio.dora.model.GithubPullRequest;
import com.liatrio.dora.model.GithubRelease;
import com.liatrio.dora.model.GithubWorkflowRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class MetricsService {

    private static final Logger log = LoggerFactory.getLogger(MetricsService.class);

    private final GitHubCacheService gitHubCacheService;
    private final DeploymentFrequencyCalculator deploymentFrequencyCalculator;
    private final LeadTimeCalculator leadTimeCalculator;
    private final ChangeFailureRateCalculator changeFailureRateCalculator;
    private final MttrCalculator mttrCalculator;

    public MetricsService(GitHubCacheService gitHubCacheService,
                          DeploymentFrequencyCalculator deploymentFrequencyCalculator,
                          LeadTimeCalculator leadTimeCalculator,
                          ChangeFailureRateCalculator changeFailureRateCalculator,
                          MttrCalculator mttrCalculator) {
        this.gitHubCacheService = gitHubCacheService;
        this.deploymentFrequencyCalculator = deploymentFrequencyCalculator;
        this.leadTimeCalculator = leadTimeCalculator;
        this.changeFailureRateCalculator = changeFailureRateCalculator;
        this.mttrCalculator = mttrCalculator;
    }

    public MetricsResponse getMetrics(String owner, String repo, String token, int windowDays) {
        log.info("Calculating DORA metrics for {}/{} over {} days", owner, repo, windowDays);
        Instant windowStart = Instant.now().minus(windowDays, ChronoUnit.DAYS);

        // Run all 5 independent GitHub fetches concurrently on the common ForkJoinPool.
        // CompletableFuture.join() unwraps and re-throws any CompletionException automatically.
        CompletableFuture<List<GithubDeployment>> deploymentsFuture = CompletableFuture
                .supplyAsync(() -> gitHubCacheService.getDeployments(owner, repo, token, windowStart));
        CompletableFuture<List<GithubRelease>> releasesFuture = CompletableFuture
                .supplyAsync(() -> gitHubCacheService.getReleases(owner, repo, token, windowStart));
        CompletableFuture<List<GithubWorkflowRun>> workflowRunsFuture = CompletableFuture
                .supplyAsync(() -> gitHubCacheService.getWorkflowRuns(owner, repo, token, windowStart));
        CompletableFuture<List<GithubPullRequest>> pullRequestsFuture = CompletableFuture
                .supplyAsync(() -> gitHubCacheService.getPullRequests(owner, repo, token, windowStart));
        CompletableFuture<List<GithubIssue>> issuesFuture = CompletableFuture
                .supplyAsync(() -> gitHubCacheService.getIssues(owner, repo, token, windowStart));

        CompletableFuture.allOf(deploymentsFuture, releasesFuture, workflowRunsFuture, pullRequestsFuture, issuesFuture).join();

        List<GithubDeployment> deployments = deploymentsFuture.join();
        List<GithubRelease> releases = releasesFuture.join();
        List<GithubWorkflowRun> workflowRuns = workflowRunsFuture.join();
        List<GithubPullRequest> pullRequests = pullRequestsFuture.join();
        List<GithubIssue> issues = issuesFuture.join();

        MetricResult deployFreq = deploymentFrequencyCalculator.calculate(deployments, releases, workflowRuns, windowDays);
        MetricResult leadTime = leadTimeCalculator.calculate(pullRequests, deployments, windowDays);
        MetricResult cfr = changeFailureRateCalculator.calculate(
                deployments, pullRequests, issues, workflowRuns, windowDays);
        MetricResult mttr = mttrCalculator.calculate(issues, deployments, windowDays);

        MetricsMeta meta = new MetricsMeta(owner, repo, windowDays, Instant.now());
        return new MetricsResponse(meta, deployFreq, leadTime, cfr, mttr);
    }
}
