package com.liatrio.dora.client;

import com.liatrio.dora.model.GithubDeployment;
import com.liatrio.dora.model.GithubIssue;
import com.liatrio.dora.model.GithubPullRequest;
import com.liatrio.dora.model.GithubWorkflowRun;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that calls the real GitHub API.
 *
 * Requires environment variable GITHUB_TEST_TOKEN to be set.
 * Run with: GITHUB_TEST_TOKEN=<your-pat> ./mvnw test -Dtest=GitHubApiClientIntegrationTest
 */
@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfEnvironmentVariable(named = "GITHUB_TEST_TOKEN", matches = ".+")
class GitHubApiClientIntegrationTest {

    @Autowired
    private GitHubApiClient client;

    private static final String OWNER = "octocat";
    private static final String REPO = "Hello-World";

    @Test
    void fetchDeployments_returnsNonNullListFromRealApi() {
        String token = System.getenv("GITHUB_TEST_TOKEN");
        Instant windowStart = Instant.now().minus(365, ChronoUnit.DAYS);

        List<GithubDeployment> deployments = client.fetchDeployments(OWNER, REPO, token, windowStart);

        assertThat(deployments).isNotNull();
    }

    @Test
    void fetchWorkflowRuns_returnsNonNullListFromRealApi() {
        String token = System.getenv("GITHUB_TEST_TOKEN");
        Instant windowStart = Instant.now().minus(365, ChronoUnit.DAYS);

        List<GithubWorkflowRun> runs = client.fetchWorkflowRuns(OWNER, REPO, token, windowStart);

        assertThat(runs).isNotNull();
    }

    @Test
    void fetchPullRequests_returnsNonNullListFromRealApi() {
        String token = System.getenv("GITHUB_TEST_TOKEN");
        Instant windowStart = Instant.now().minus(365, ChronoUnit.DAYS);

        List<GithubPullRequest> prs = client.fetchPullRequests(OWNER, REPO, token, windowStart);

        assertThat(prs).isNotNull();
    }

    @Test
    void fetchIssues_returnsNonNullListFromRealApi() {
        String token = System.getenv("GITHUB_TEST_TOKEN");
        Instant windowStart = Instant.now().minus(365, ChronoUnit.DAYS);

        List<GithubIssue> issues = client.fetchIssues(OWNER, REPO, token, windowStart);

        assertThat(issues).isNotNull();
    }
}
