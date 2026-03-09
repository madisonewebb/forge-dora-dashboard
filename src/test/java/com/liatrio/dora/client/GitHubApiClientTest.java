package com.liatrio.dora.client;

import com.liatrio.dora.exception.GitHubRateLimitException;
import com.liatrio.dora.model.GithubDeployment;
import com.liatrio.dora.model.GithubIssue;
import com.liatrio.dora.model.GithubPullRequest;
import com.liatrio.dora.model.GithubWorkflowRun;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GitHubApiClientTest {

    private MockWebServer mockWebServer;
    private GitHubApiClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .defaultHeader("Accept", "application/vnd.github+json")
                .build();
        client = new GitHubApiClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void fetchDeployments_setsAuthorizationHeader() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("[]")
                .addHeader("Content-Type", "application/json"));

        client.fetchDeployments("owner", "repo", "my-test-token",
                Instant.now().minus(30, ChronoUnit.DAYS));

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer my-test-token");
    }

    @Test
    void fetchDeployments_returnsDeploymentsWithinWindow() {
        Instant now = Instant.now();
        String json = """
                [
                  {
                    "id": 1001,
                    "environment": "production",
                    "state": "success",
                    "created_at": "%s",
                    "updated_at": "%s"
                  }
                ]
                """.formatted(now.minus(1, ChronoUnit.HOURS), now);

        mockWebServer.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json"));

        List<GithubDeployment> results = client.fetchDeployments(
                "owner", "repo", "token", now.minus(30, ChronoUnit.DAYS));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getGithubId()).isEqualTo(1001L);
        assertThat(results.get(0).getStatus()).isEqualTo("success");
    }

    @Test
    void fetchDeployments_filtersOutItemsBeforeWindowStart() {
        Instant now = Instant.now();
        Instant windowStart = now.minus(30, ChronoUnit.DAYS);
        String json = """
                [
                  {
                    "id": 1002,
                    "environment": "production",
                    "state": "success",
                    "created_at": "%s",
                    "updated_at": "%s"
                  }
                ]
                """.formatted(now.minus(60, ChronoUnit.DAYS), now);

        mockWebServer.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json"));

        List<GithubDeployment> results = client.fetchDeployments("owner", "repo", "token", windowStart);

        assertThat(results).isEmpty();
    }

    @Test
    void fetchWorkflowRuns_includesOnlyMainBranchInQueryParam() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
                .setBody("{\"workflow_runs\": []}")
                .addHeader("Content-Type", "application/json"));

        client.fetchWorkflowRuns("owner", "repo", "token",
                Instant.now().minus(30, ChronoUnit.DAYS));

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).contains("branch=main");
    }

    @Test
    void fetchPullRequests_onlyIncludesMergedPrs() {
        Instant now = Instant.now();
        String json = """
                [
                  {
                    "id": 3001,
                    "title": "Merged PR",
                    "merged_at": "%s",
                    "created_at": "%s",
                    "labels": [{"name": "enhancement"}]
                  },
                  {
                    "id": 3002,
                    "title": "Open PR",
                    "merged_at": null,
                    "created_at": "%s",
                    "labels": []
                  }
                ]
                """.formatted(
                now.minus(2, ChronoUnit.HOURS),
                now.minus(1, ChronoUnit.DAYS),
                now.minus(1, ChronoUnit.HOURS));

        mockWebServer.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json"));

        List<GithubPullRequest> results = client.fetchPullRequests(
                "owner", "repo", "token", now.minus(30, ChronoUnit.DAYS));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Merged PR");
        assertThat(results.get(0).getLabels()).isEqualTo("enhancement");
    }

    @Test
    void fetchIssues_skipsPullRequestsReturnedByIssuesEndpoint() {
        Instant now = Instant.now();
        String json = """
                [
                  {
                    "id": 4001,
                    "title": "Real issue",
                    "state": "closed",
                    "labels": [{"name": "incident"}],
                    "created_at": "%s",
                    "closed_at": "%s"
                  },
                  {
                    "id": 4002,
                    "title": "A PR returned by issues endpoint",
                    "state": "closed",
                    "labels": [],
                    "pull_request": {"url": "https://api.github.com/repos/o/r/pulls/42"},
                    "created_at": "%s",
                    "closed_at": "%s"
                  }
                ]
                """.formatted(
                now.minus(3, ChronoUnit.HOURS),
                now.minus(1, ChronoUnit.HOURS),
                now.minus(3, ChronoUnit.HOURS),
                now.minus(1, ChronoUnit.HOURS));

        mockWebServer.enqueue(new MockResponse()
                .setBody(json)
                .addHeader("Content-Type", "application/json"));

        List<GithubIssue> results = client.fetchIssues(
                "owner", "repo", "token", now.minus(30, ChronoUnit.DAYS));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Real issue");
        assertThat(results.get(0).getLabels()).contains("incident");
    }

    @Test
    void fetchDeployments_throwsGitHubRateLimitExceptionAfterRetries() {
        Instant resetTime = Instant.now().plusSeconds(600);
        MockResponse rateLimitResponse = new MockResponse()
                .setResponseCode(403)
                .addHeader("X-RateLimit-Remaining", "0")
                .addHeader("X-RateLimit-Reset", String.valueOf(resetTime.getEpochSecond()))
                .setBody("{\"message\": \"API rate limit exceeded\"}");

        // Enqueue enough responses for the initial call + 3 retries
        for (int i = 0; i <= 3; i++) {
            mockWebServer.enqueue(rateLimitResponse);
        }

        assertThatThrownBy(() ->
                client.fetchDeployments("owner", "repo", "token",
                        Instant.now().minus(30, ChronoUnit.DAYS)))
                .isInstanceOf(GitHubRateLimitException.class)
                .satisfies(ex -> {
                    GitHubRateLimitException rateLimitEx = (GitHubRateLimitException) ex;
                    assertThat(rateLimitEx.getResetsAt().getEpochSecond())
                            .isEqualTo(resetTime.getEpochSecond());
                });
    }
}
