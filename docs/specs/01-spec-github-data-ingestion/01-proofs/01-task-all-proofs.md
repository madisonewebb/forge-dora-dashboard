# Spec 01 — GitHub Data Ingestion: Proof Artifacts

## Task 1.0 — Scaffold the Spring Boot Project

### pom.xml Dependencies

Key dependencies confirmed in `pom.xml`:
- `spring-boot-starter-web`
- `spring-boot-starter-webflux`
- `spring-boot-starter-data-jpa`
- `h2` (runtime)
- `postgresql` (runtime)
- `flyway-core`
- `lombok`
- `spring-boot-starter-test`
- `mockwebserver` + `okhttp` (test)

### Context Load Test

```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- DoraApplicationTests
```

The `DoraApplicationTests.contextLoads()` test confirms the Spring context assembles without errors using the `dev` profile.

### application.properties (dev profile)

```properties
spring.datasource.url=jdbc:h2:mem:doradb;DB_CLOSE_DELAY=-1
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
spring.flyway.enabled=true
github.cache.ttl-minutes=15
```

---

## Task 2.0 — Data Models and Persistence Layer

### Repository Tests

```
Tests run: 2, Failures: 0, Errors: 0 -- GithubDeploymentRepositoryTest
Tests run: 2, Failures: 0, Errors: 0 -- GithubWorkflowRunRepositoryTest
Tests run: 2, Failures: 0, Errors: 0 -- GithubPullRequestRepositoryTest
Tests run: 2, Failures: 0, Errors: 0 -- GithubIssueRepositoryTest
```

Each test verifies:
1. Save and retrieve by `repoId` + time range (GREEN)
2. Items outside time range (or on wrong branch / unmerged) are excluded (GREEN)

### Flyway Migration

Two migrations applied on startup:
- `V1__create_github_tables.sql` — creates `github_deployments`, `github_workflow_runs`, `github_pull_requests`, `github_issues` with indexes
- `V2__create_cache_table.sql` — creates `github_cache_entries` with unique constraint on `(repo_id, data_type)`

---

## Task 3.0 — GitHub API Client

### Unit Tests

```
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- GitHubApiClientTest
```

Tests verified:
- `fetchDeployments` sets `Authorization: Bearer <token>` header
- Items before `windowStart` are excluded from results
- `fetchWorkflowRuns` includes `branch=main` query parameter
- `fetchPullRequests` excludes unmerged PRs (null `merged_at`)
- `fetchIssues` skips pull requests returned by the issues endpoint
- Rate limit 403 response triggers retries and throws `GitHubRateLimitException`

### Integration Tests

```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 4 -- GitHubApiClientIntegrationTest
```

Skipped: `GITHUB_TEST_TOKEN` environment variable not set in this run.
To run: `GITHUB_TEST_TOKEN=<your-pat> ./mvnw test -Dtest=GitHubApiClientIntegrationTest`

---

## Task 4.0 — Caching Layer with TTL

### Cache Service Tests

```
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- GitHubCacheServiceTest
```

Tests verified:
- Two calls within TTL → `GitHubApiClient` invoked `never()` (cache hit both times)
- Expired cache entry (fetched 20 min ago, TTL=15 min) → API invoked `times(1)`
- No cache entry → API invoked `times(1)`
- `invalidate()` calls `deleteByRepoId` on all five repositories

### Cache Controller Test

```
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- CacheControllerTest
```

`DELETE /api/cache/octocat/Hello-World` returns HTTP 204 and calls `cacheService.invalidate("octocat", "Hello-World")`.

---

## Task 5.0 — Rate Limit Handling and Global Error Mapping

### Exception Handler Test

```
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- GlobalExceptionHandlerTest
```

`GitHubRateLimitException` → HTTP 429 with JSON body:
```json
{
  "error": "GitHub rate limit exceeded",
  "resetsAt": "2026-03-09T12:00:00Z"
}
```

Rate limit retry behavior (403 + `X-RateLimit-Remaining: 0`) verified in `GitHubApiClientTest.fetchDeployments_throwsGitHubRateLimitExceptionAfterRetries`.

---

## Full Test Suite Summary

```
Tests run: 26, Failures: 0, Errors: 0, Skipped: 4
BUILD SUCCESS
```

Skipped: 4 integration tests (require `GITHUB_TEST_TOKEN` env var — intentional, not failures).
All 22 unit/slice tests: GREEN.
