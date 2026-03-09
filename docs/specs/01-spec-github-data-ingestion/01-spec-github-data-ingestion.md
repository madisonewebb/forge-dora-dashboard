# 01-spec-github-data-ingestion

## Introduction/Overview

This spec covers the GitHub data ingestion layer — the foundation everything else builds on. The system needs to fetch raw data from the GitHub REST API (deployments, workflow runs, pull requests, and issues) for a single repository, cache it to avoid redundant API calls, and expose it internally for the metrics engine. Without reliable data ingestion, no DORA metrics can be calculated.

## Goals

- Provide a GitHub API client that fetches all four data types needed for DORA metrics
- Cache responses in H2 (dev) and PostgreSQL (prod) with a configurable TTL to stay within GitHub's rate limits
- Handle GitHub API rate limiting gracefully: retry silently with backoff, surface an error only when retries are exhausted
- Accept a GitHub Personal Access Token (PAT) per request so any authenticated repository (public or private) can be queried
- Store raw data in a schema that the metrics engine can query without re-parsing

## User Stories

**As a developer**, I want to enter a GitHub repository and PAT so that the app can fetch data from any repo I have access to.

**As a developer**, I want the app to cache GitHub data for 15 minutes so that I can refresh the dashboard without burning my API rate limit.

**As a developer**, I want the app to tell me when GitHub's rate limit is exhausted (after retries) so that I know why data is unavailable and when to try again.

**As a developer**, I want the app to pull deployment events, workflow runs, pull requests, and issues so that all four DORA metrics have the raw data they need.

## Demoable Units of Work

### Unit 1: GitHub API Client — Fetch and Return Raw Data

**Purpose:** Proves the app can talk to the GitHub API with a PAT and retrieve all four data types for a given repository.

**Functional Requirements:**
- The system shall accept an `owner`, `repo`, and `githubToken` as inputs for every GitHub API request.
- The system shall fetch GitHub Deployments from `GET /repos/{owner}/{repo}/deployments` including deployment status events.
- The system shall fetch GitHub Actions workflow runs from `GET /repos/{owner}/{repo}/actions/runs` filtered to the `main` branch.
- The system shall fetch Pull Requests from `GET /repos/{owner}/{repo}/pulls` (state: closed, merged only).
- The system shall fetch GitHub Issues from `GET /repos/{owner}/{repo}/issues` (state: closed, labels filterable).
- The system shall support pagination for all four endpoints, fetching all pages within the requested time window.
- The system shall filter all results to a configurable time window (default: last 30 days).
- The system shall use Spring WebClient for all HTTP calls.

**Proof Artifacts:**
- Integration test: `GitHubApiClientIntegrationTest` calls the real GitHub API against a known public repo (e.g., `octocat/Hello-World`) using a test PAT from environment variables and asserts non-empty results for each data type.
- Unit test: `GitHubApiClientTest` mocks HTTP responses and asserts correct pagination handling, time-window filtering, and header construction.

---

### Unit 2: Data Models and Persistence

**Purpose:** Proves that raw GitHub data can be persisted in a structured schema that downstream services can query.

**Functional Requirements:**
- The system shall define JPA entities: `GithubDeployment`, `GithubWorkflowRun`, `GithubPullRequest`, `GithubIssue`.
- Each entity shall store at minimum: external GitHub ID, repository identifier (`owner/repo`), timestamps relevant to DORA calculations, status/state fields, and label lists.
- The system shall define Spring Data JPA repositories for each entity with query methods for time-window lookups.
- The system shall use Flyway or schema-auto-create to initialize the schema on startup.
- The system shall use H2 in the `dev` Spring profile and PostgreSQL in the `prod` profile.

**Proof Artifacts:**
- Unit tests: `GithubDeploymentRepositoryTest`, `GithubPullRequestRepositoryTest` etc. use `@DataJpaTest` against H2 and assert that save/findByRepoAndTimeRange queries return correct results.
- Screenshot: Running the app with `./mvnw spring-boot:run -Dspring.profiles.active=dev` shows Flyway migration logs and H2 console accessible at `/h2-console`.

---

### Unit 3: Caching Layer with TTL

**Purpose:** Proves that repeated fetches within the TTL window use cached data instead of hitting the GitHub API.

**Functional Requirements:**
- The system shall check the cache before making any GitHub API call; if a valid (non-expired) cache entry exists for the `owner/repo` + data type + time window combination, it shall return the cached data.
- The system shall store cache metadata (repo, data type, fetched-at timestamp, TTL) in the database alongside raw data.
- The TTL shall be configurable via `application.properties` (default: `github.cache.ttl-minutes=15`).
- The system shall invalidate and re-fetch when the TTL is exceeded.
- The system shall expose a `DELETE /api/cache/{owner}/{repo}` endpoint to manually bust the cache.

**Proof Artifacts:**
- Unit test: `GitHubCacheServiceTest` asserts that a second call within TTL does not invoke the `GitHubApiClient` (verified via Mockito `verify(client, times(1))`).
- Unit test: asserts that a call after TTL expiry does invoke the client again.

---

### Unit 4: Rate Limit Handling

**Purpose:** Proves the app survives GitHub rate limit responses gracefully.

**Functional Requirements:**
- The system shall inspect the `X-RateLimit-Remaining` and `X-RateLimit-Reset` response headers on every GitHub API response.
- When remaining requests drop to zero, the system shall retry after the reset time with exponential backoff (max 3 retries).
- If all retries are exhausted, the system shall throw a `GitHubRateLimitException` containing the reset timestamp.
- The REST controller shall translate `GitHubRateLimitException` into an HTTP 429 response with a JSON body: `{ "error": "GitHub rate limit exceeded", "resetsAt": "<ISO timestamp>" }`.

**Proof Artifacts:**
- Unit test: `GitHubRateLimitHandlerTest` mocks a 403 response with rate-limit headers and asserts retry behavior and eventual exception throw.
- Unit test: `MetricsControllerTest` asserts that a `GitHubRateLimitException` produces a 429 response with the correct JSON body.

## Non-Goals (Out of Scope)

1. **GitHub Organization support**: Only single `owner/repo` input is supported. Org-wide aggregation is a stretch goal for a future spec.
2. **Webhooks / real-time updates**: Data is fetched on demand or from cache. No event-driven ingestion.
3. **GitHub GraphQL API**: Only REST API endpoints are used in this spec.
4. **OAuth / GitHub App authentication**: Only PAT-based authentication. OAuth is out of scope.
5. **Historical data beyond the selected time window**: The app does not backfill data older than the user's configured window.

## Design Considerations

No specific UI in this spec — this is a backend-only layer. The cache bust endpoint (`DELETE /api/cache/{owner}/{repo}`) is a developer/admin tool only; no UI is needed for it.

## Repository Standards

- Spring Boot 3 + Java 17; follow existing Spring patterns from `CLAUDE.md`
- Dependency injection via constructor injection (not field injection)
- SLF4J for all logging (`LoggerFactory.getLogger(ClassName.class)`)
- Exception handling via `@ControllerAdvice` / `@ExceptionHandler`
- Tests in `src/test/java` mirroring `src/main/java` structure
- Test class naming: `[ClassName]Test` or `[ClassName]Tests`
- Use JUnit 5 + Mockito; Spring Boot test slices (`@DataJpaTest`, `@WebMvcTest`) where appropriate
- TDD: write failing test first, then implementation (RED → GREEN → REFACTOR)

## Technical Considerations

- Use Spring WebClient (reactive HTTP client) for GitHub API calls; configure with a `WebClient.Builder` bean.
- Spring profiles: `dev` uses H2 + in-memory config; `prod` uses PostgreSQL (connection string via env var `DATABASE_URL`).
- The GitHub PAT is passed per-request in the `Authorization: Bearer {token}` header and is **never stored** in the database.
- Pagination: GitHub returns max 100 items per page; use `Link` header `rel="next"` to detect more pages.
- Time window filtering: fetch up to the GitHub API's max page limit, then filter by `created_at` / `merged_at` client-side to stay within the window.
- Flyway migrations go in `src/main/resources/db/migration/`.

## Security Considerations

- The GitHub PAT is transmitted over HTTPS only and is never logged or persisted.
- The PAT must not appear in any test fixtures, committed config files, or log output.
- Integration tests that require a real PAT must read it from an environment variable (`GITHUB_TEST_TOKEN`), never hardcoded.
- The H2 console (`/h2-console`) must be disabled in the `prod` profile.

## Success Metrics

1. **Data retrieval**: All four GitHub data types are successfully fetched and persisted for any public repository within a 30-day window.
2. **Cache hit rate**: A second request within TTL returns data without a GitHub API call (verified by test).
3. **Rate limit resilience**: The app retries on rate limit and returns a clear 429 JSON response when exhausted.
4. **Test coverage**: All `GitHubApiClient`, `GitHubCacheService`, and repository classes have unit test coverage; integration test passes against a real public GitHub repo.

## Open Questions

No open questions at this time.
