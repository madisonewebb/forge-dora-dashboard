package com.liatrio.dora.client;

import com.liatrio.dora.exception.GitHubRateLimitException;
import com.liatrio.dora.model.GithubDeployment;
import com.liatrio.dora.model.GithubIssue;
import com.liatrio.dora.model.GithubPullRequest;
import com.liatrio.dora.model.GithubRelease;
import com.liatrio.dora.model.GithubWorkflowRun;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GitHubApiClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubApiClient.class);
    private static final int MAX_RETRIES = 3;
    private static final int PER_PAGE = 100;

    private final WebClient webClient;

    public GitHubApiClient(WebClient githubWebClient) {
        this.webClient = githubWebClient;
    }

    public List<GithubDeployment> fetchDeployments(String owner, String repo, String token, Instant windowStart) {
        List<GithubDeployment> results = new ArrayList<>();
        String url = "/repos/{owner}/{repo}/deployments?per_page=" + PER_PAGE;
        String nextUrl = url.replace("{owner}", owner).replace("{repo}", repo);

        while (nextUrl != null) {
            ResponseEntity<List<Map<String, Object>>> response = fetchPage(nextUrl, token);
            List<Map<String, Object>> page = response.getBody();
            if (page == null || page.isEmpty()) break;

            boolean reachedWindow = false;
            for (Map<String, Object> item : page) {
                Instant createdAt = parseInstant(item.get("created_at"));
                if (createdAt != null && createdAt.isBefore(windowStart)) {
                    reachedWindow = true;
                    break;
                }
                GithubDeployment deployment = GithubDeployment.builder()
                        .githubId(toLong(item.get("id")))
                        .repoId(owner + "/" + repo)
                        .environment((String) item.get("environment"))
                        .status(extractStatus(item))
                        .createdAt(createdAt)
                        .updatedAt(parseInstant(item.get("updated_at")))
                        .build();
                results.add(deployment);
            }
            if (reachedWindow) break;
            nextUrl = extractNextUrl(response);
        }
        return results;
    }

    public List<GithubWorkflowRun> fetchWorkflowRuns(String owner, String repo, String token, Instant windowStart) {
        List<GithubWorkflowRun> results = new ArrayList<>();
        String nextUrl = "/repos/" + owner + "/" + repo + "/actions/runs?branch=main&per_page=" + PER_PAGE;

        while (nextUrl != null) {
            ResponseEntity<Map<String, Object>> response = fetchPageRaw(nextUrl, token);
            Map<String, Object> body = response.getBody();
            if (body == null) break;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> runs = (List<Map<String, Object>>) body.get("workflow_runs");
            if (runs == null || runs.isEmpty()) break;

            boolean reachedWindow = false;
            for (Map<String, Object> item : runs) {
                Instant createdAt = parseInstant(item.get("created_at"));
                if (createdAt != null && createdAt.isBefore(windowStart)) {
                    reachedWindow = true;
                    break;
                }
                GithubWorkflowRun run = GithubWorkflowRun.builder()
                        .githubId(toLong(item.get("id")))
                        .repoId(owner + "/" + repo)
                        .name((String) item.get("name"))
                        .status((String) item.get("status"))
                        .conclusion((String) item.get("conclusion"))
                        .headBranch((String) item.get("head_branch"))
                        .createdAt(createdAt)
                        .updatedAt(parseInstant(item.get("updated_at")))
                        .build();
                results.add(run);
            }
            if (reachedWindow) break;
            nextUrl = extractNextUrl(response);
        }
        return results;
    }

    public List<GithubRelease> fetchReleases(String owner, String repo, String token, Instant windowStart) {
        List<GithubRelease> results = new ArrayList<>();
        String nextUrl = "/repos/" + owner + "/" + repo + "/releases?per_page=" + PER_PAGE;

        while (nextUrl != null) {
            ResponseEntity<List<Map<String, Object>>> response = fetchPage(nextUrl, token);
            List<Map<String, Object>> page = response.getBody();
            if (page == null || page.isEmpty()) break;

            boolean reachedWindow = false;
            for (Map<String, Object> item : page) {
                boolean isDraft = Boolean.TRUE.equals(item.get("draft"));
                boolean isPrerelease = Boolean.TRUE.equals(item.get("prerelease"));
                if (isDraft || isPrerelease) continue;

                Instant publishedAt = parseInstant(item.get("published_at"));
                Instant createdAt = parseInstant(item.get("created_at"));
                if (publishedAt != null && publishedAt.isBefore(windowStart)) {
                    reachedWindow = true;
                    break;
                }
                GithubRelease release = GithubRelease.builder()
                        .githubId(toLong(item.get("id")))
                        .repoId(owner + "/" + repo)
                        .tagName((String) item.get("tag_name"))
                        .name((String) item.get("name"))
                        .prerelease(isPrerelease)
                        .draft(isDraft)
                        .publishedAt(publishedAt)
                        .createdAt(createdAt != null ? createdAt : (publishedAt != null ? publishedAt : Instant.now()))
                        .build();
                results.add(release);
            }
            if (reachedWindow) break;
            nextUrl = extractNextUrl(response);
        }
        return results;
    }

    public List<GithubPullRequest> fetchPullRequests(String owner, String repo, String token, Instant windowStart) {
        List<GithubPullRequest> results = new ArrayList<>();
        String nextUrl = "/repos/" + owner + "/" + repo + "/pulls?state=closed&per_page=" + PER_PAGE;

        while (nextUrl != null) {
            ResponseEntity<List<Map<String, Object>>> response = fetchPage(nextUrl, token);
            List<Map<String, Object>> page = response.getBody();
            if (page == null || page.isEmpty()) break;

            boolean reachedWindow = false;
            for (Map<String, Object> item : page) {
                Instant mergedAt = parseInstant(item.get("merged_at"));
                if (mergedAt == null) continue; // not merged
                if (mergedAt.isBefore(windowStart)) {
                    reachedWindow = true;
                    break;
                }
                GithubPullRequest pr = GithubPullRequest.builder()
                        .githubId(toLong(item.get("id")))
                        .repoId(owner + "/" + repo)
                        .title((String) item.get("title"))
                        .labels(extractLabels(item))
                        .mergedAt(mergedAt)
                        .mergeCommitSha((String) item.get("merge_commit_sha"))
                        .createdAt(parseInstant(item.get("created_at")))
                        .build();
                results.add(pr);
            }
            if (reachedWindow) break;
            nextUrl = extractNextUrl(response);
        }
        return results;
    }

    public List<GithubIssue> fetchIssues(String owner, String repo, String token, Instant windowStart) {
        List<GithubIssue> results = new ArrayList<>();
        String nextUrl = "/repos/" + owner + "/" + repo + "/issues?state=closed&per_page=" + PER_PAGE;

        while (nextUrl != null) {
            ResponseEntity<List<Map<String, Object>>> response = fetchPage(nextUrl, token);
            List<Map<String, Object>> page = response.getBody();
            if (page == null || page.isEmpty()) break;

            boolean reachedWindow = false;
            for (Map<String, Object> item : page) {
                // Skip pull requests returned by the issues endpoint
                if (item.containsKey("pull_request")) continue;
                Instant createdAt = parseInstant(item.get("created_at"));
                if (createdAt != null && createdAt.isBefore(windowStart)) {
                    reachedWindow = true;
                    break;
                }
                GithubIssue issue = GithubIssue.builder()
                        .githubId(toLong(item.get("id")))
                        .repoId(owner + "/" + repo)
                        .title((String) item.get("title"))
                        .state((String) item.get("state"))
                        .labels(extractLabels(item))
                        .createdAt(createdAt)
                        .closedAt(parseInstant(item.get("closed_at")))
                        .build();
                results.add(issue);
            }
            if (reachedWindow) break;
            nextUrl = extractNextUrl(response);
        }
        return results;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private ResponseEntity<List<Map<String, Object>>> fetchPage(String url, String token) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return webClient.get()
                        .uri(url)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .toEntity((Class<List<Map<String, Object>>>) (Class<?>) List.class)
                        .block();
            } catch (WebClientResponseException e) {
                if (isPaginationStopSignal(e)) {
                    log.warn("GitHub pagination stopped at {} ({}), returning partial results", url, e.getStatusCode());
                    return ResponseEntity.ok(List.of());
                }
                handleRateLimitError(e, attempt);
            }
        }
        throw new GitHubRateLimitException(Instant.now().plusSeconds(3600));
    }

    @SuppressWarnings("unchecked")
    private ResponseEntity<Map<String, Object>> fetchPageRaw(String url, String token) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return webClient.get()
                        .uri(url)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .toEntity((Class<Map<String, Object>>) (Class<?>) Map.class)
                        .block();
            } catch (WebClientResponseException e) {
                if (isPaginationStopSignal(e)) {
                    log.warn("GitHub pagination stopped at {} ({}), returning partial results", url, e.getStatusCode());
                    return ResponseEntity.ok(Map.of());
                }
                handleRateLimitError(e, attempt);
            }
        }
        throw new GitHubRateLimitException(Instant.now().plusSeconds(3600));
    }

    /**
     * Returns true for GitHub responses that mean "stop paginating, use what you have".
     * 422 = pagination depth limit exceeded (GitHub caps at ~1000 results for some endpoints).
     * 404 = resource not found (repo has no deployments, actions not enabled, etc.).
     */
    private boolean isPaginationStopSignal(WebClientResponseException e) {
        int status = e.getStatusCode().value();
        return status == 404 || status == 422;
    }

    private void handleRateLimitError(WebClientResponseException e, int attempt) {
        HttpStatusCode status = e.getStatusCode();
        String remaining = e.getHeaders().getFirst("X-RateLimit-Remaining");
        String resetHeader = e.getHeaders().getFirst("X-RateLimit-Reset");

        if ((status.value() == 403 || status.value() == 429) && "0".equals(remaining)) {
            Instant resetsAt = resetHeader != null
                    ? Instant.ofEpochSecond(Long.parseLong(resetHeader))
                    : Instant.now().plusSeconds(3600);

            if (attempt >= MAX_RETRIES) {
                throw new GitHubRateLimitException(resetsAt);
            }
            long sleepSeconds = (long) Math.pow(2, attempt); // 1s, 2s, 4s
            log.warn("GitHub rate limit hit. Retrying in {}s (attempt {}/{})", sleepSeconds, attempt + 1, MAX_RETRIES);
            try {
                Thread.sleep(sleepSeconds * 1000);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new GitHubRateLimitException(resetsAt);
            }
        } else {
            throw e;
        }
    }

    private String extractNextUrl(ResponseEntity<?> response) {
        String linkHeader = response.getHeaders().getFirst("Link");
        return parseLinkNext(linkHeader);
    }

    private String parseLinkNext(String linkHeader) {
        if (linkHeader == null) return null;
        for (String part : linkHeader.split(",")) {
            part = part.trim();
            if (part.contains("rel=\"next\"")) {
                int start = part.indexOf('<') + 1;
                int end = part.indexOf('>');
                if (start > 0 && end > start) {
                    String fullUrl = part.substring(start, end);
                    // Strip base URL if present so WebClient can resolve it
                    return fullUrl.replace("https://api.github.com", "");
                }
            }
        }
        return null;
    }

    private Instant parseInstant(Object value) {
        if (value == null) return null;
        try {
            return Instant.parse(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractLabels(Map<String, Object> item) {
        Object labelsObj = item.get("labels");
        if (!(labelsObj instanceof List<?> labelList)) return "";
        List<String> names = new ArrayList<>();
        for (Object label : labelList) {
            if (label instanceof Map<?, ?> labelMap) {
                Object name = labelMap.get("name");
                if (name != null) names.add(name.toString());
            }
        }
        return String.join(",", names);
    }

    @SuppressWarnings("unchecked")
    private String extractStatus(Map<String, Object> item) {
        // Deployment status is nested; fall back to the deployment's own "state" field
        Object statusesObj = item.get("statuses_url");
        Object state = item.get("task");
        // Use the transient "state" field if present in the API response
        Object statusField = item.get("state");
        return statusField != null ? statusField.toString() : "unknown";
    }
}
