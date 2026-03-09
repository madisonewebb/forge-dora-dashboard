# 01-tasks-github-data-ingestion

## Relevant Files

### Production Code
- `pom.xml` — Maven build file with all Spring Boot 3 / Java 17 dependencies
- `Tiltfile` — Local dev orchestration: builds and runs the backend container
- `.gitignore` — Ignores `.env`, IDE files, Maven targets, compiled classes
- `src/main/java/com/liatrio/dora/DoraApplication.java` — Spring Boot entry point
- `src/main/java/com/liatrio/dora/config/WebClientConfig.java` — `WebClient.Builder` bean configured for `https://api.github.com`
- `src/main/java/com/liatrio/dora/model/GithubDeployment.java` — JPA entity for GitHub Deployment events
- `src/main/java/com/liatrio/dora/model/GithubWorkflowRun.java` — JPA entity for GitHub Actions workflow runs
- `src/main/java/com/liatrio/dora/model/GithubPullRequest.java` — JPA entity for merged pull requests
- `src/main/java/com/liatrio/dora/model/GithubIssue.java` — JPA entity for GitHub Issues (incident tracking)
- `src/main/java/com/liatrio/dora/model/GithubCacheEntry.java` — JPA entity storing cache metadata (repo, data type, fetched-at, TTL)
- `src/main/java/com/liatrio/dora/repository/GithubDeploymentRepository.java` — Spring Data JPA repository with time-range query
- `src/main/java/com/liatrio/dora/repository/GithubWorkflowRunRepository.java` — Spring Data JPA repository with branch + time-range query
- `src/main/java/com/liatrio/dora/repository/GithubPullRequestRepository.java` — Spring Data JPA repository with merged-at time-range query
- `src/main/java/com/liatrio/dora/repository/GithubIssueRepository.java` — Spring Data JPA repository with label + time-range query
- `src/main/java/com/liatrio/dora/repository/GithubCacheEntryRepository.java` — Spring Data JPA repository for cache metadata lookups
- `src/main/java/com/liatrio/dora/client/GitHubApiClient.java` — WebClient-based GitHub REST API client (all 4 endpoints, pagination, time-window filtering)
- `src/main/java/com/liatrio/dora/service/GitHubCacheService.java` — Wraps `GitHubApiClient`; checks cache before fetching, stores on miss, re-fetches on TTL expiry
- `src/main/java/com/liatrio/dora/controller/CacheController.java` — `DELETE /api/cache/{owner}/{repo}` cache-bust endpoint
- `src/main/java/com/liatrio/dora/exception/GitHubRateLimitException.java` — Runtime exception carrying the rate-limit reset timestamp
- `src/main/java/com/liatrio/dora/exception/GlobalExceptionHandler.java` — `@ControllerAdvice` mapping exceptions to HTTP responses
- `src/main/resources/application.properties` — Dev-profile settings: H2 datasource, H2 console enabled, Flyway enabled, `github.cache.ttl-minutes=15`
- `src/main/resources/application-prod.properties` — Prod-profile settings: PostgreSQL via `DATABASE_URL` env var, H2 console disabled
- `src/main/resources/db/migration/V1__create_github_tables.sql` — Flyway migration creating the four GitHub data tables
- `src/main/resources/db/migration/V2__create_cache_table.sql` — Flyway migration creating the cache metadata table

### Test Code
- `src/test/java/com/liatrio/dora/DoraApplicationTests.java` — Spring context load smoke test
- `src/test/java/com/liatrio/dora/repository/GithubDeploymentRepositoryTest.java` — `@DataJpaTest` for deployment entity persistence
- `src/test/java/com/liatrio/dora/repository/GithubWorkflowRunRepositoryTest.java` — `@DataJpaTest` for workflow run entity persistence
- `src/test/java/com/liatrio/dora/repository/GithubPullRequestRepositoryTest.java` — `@DataJpaTest` for pull request entity persistence
- `src/test/java/com/liatrio/dora/repository/GithubIssueRepositoryTest.java` — `@DataJpaTest` for issue entity persistence
- `src/test/java/com/liatrio/dora/client/GitHubApiClientTest.java` — Unit tests with mocked WebClient for all four fetch methods
- `src/test/java/com/liatrio/dora/client/GitHubApiClientIntegrationTest.java` — Integration test against real GitHub API (requires `GITHUB_TEST_TOKEN` env var)
- `src/test/java/com/liatrio/dora/service/GitHubCacheServiceTest.java` — Unit tests verifying TTL cache hit/miss behavior with Mockito
- `src/test/java/com/liatrio/dora/controller/CacheControllerTest.java` — `@WebMvcTest` for the cache-bust DELETE endpoint
- `src/test/java/com/liatrio/dora/exception/GitHubRateLimitHandlerTest.java` — Unit tests for retry behavior and exception throw on rate limit
- `src/test/java/com/liatrio/dora/exception/GlobalExceptionHandlerTest.java` — `@WebMvcTest` asserting 429 JSON response shape

### Notes

- All tests run with `./mvnw test`. Use `./mvnw test -Dtest=ClassName` to run a single test class.
- Follow TDD strictly: write the failing test first (RED), then write the minimum implementation to make it pass (GREEN), then clean up (REFACTOR).
- Use `@DataJpaTest` for repository tests (spins up H2 automatically), `@WebMvcTest` for controller tests, and plain JUnit 5 + Mockito for service/client tests.
- Use constructor injection everywhere — no `@Autowired` on fields.
- Log with `private static final Logger log = LoggerFactory.getLogger(ClassName.class);` — never use `System.out.println`.
- The GitHub PAT (`token`) must never be logged at any level. Double-check any log statements that touch request parameters.

---

## Tasks

### [x] 1.0 Scaffold the Spring Boot Project

Set up the new Spring Boot 3 / Java 17 project with all required dependencies, Spring profiles, Tilt local-dev config, and project layout before any feature code is written.

#### 1.0 Proof Artifact(s)

- CLI: `./mvnw spring-boot:run -Dspring.profiles.active=dev` starts the server and logs `Started DoraApplication` with no errors
- CLI: `./mvnw test` passes (at minimum the context load smoke test in `DoraApplicationTests`)
- Screenshot: H2 console accessible at `http://localhost:8080/h2-console` when running the `dev` profile
- Diff: `pom.xml` contains all required dependencies (Spring Boot Web, WebFlux, Data JPA, H2, PostgreSQL, Flyway, Lombok, Spring Boot Test)

#### 1.0 Tasks

- [x] 1.1 Create `pom.xml` with `spring-boot-starter-parent` (3.x), and the following dependencies: `spring-boot-starter-web`, `spring-boot-starter-webflux`, `spring-boot-starter-data-jpa`, `h2`, `postgresql`, `flyway-core`, `lombok`, `spring-boot-starter-test`. Set Java source/target to `17`.
- [x] 1.2 Create the main application class at `src/main/java/com/liatrio/dora/DoraApplication.java` annotated with `@SpringBootApplication`.
- [x] 1.3 Create `src/main/resources/application.properties` with dev-profile settings: H2 in-memory datasource (`jdbc:h2:mem:doradb`), H2 console enabled at `/h2-console`, Flyway enabled, `spring.profiles.default=dev`, and `github.cache.ttl-minutes=15`.
- [x] 1.4 Create `src/main/resources/application-prod.properties` with PostgreSQL datasource using `${DATABASE_URL}` and H2 console disabled (`spring.h2.console.enabled=false`).
- [x] 1.5 Write `src/test/java/com/liatrio/dora/DoraApplicationTests.java` with a single `@SpringBootTest` test method `contextLoads()` that has an empty body — this confirms the Spring context assembles without errors.
- [x] 1.6 Run `./mvnw test` and confirm the smoke test passes. Fix any missing configuration until it does.
- [x] 1.7 Create `.gitignore` covering: `target/`, `.env`, `*.class`, `.idea/`, `*.iml`, `.DS_Store`, `*.log`.
- [x] 1.8 Create a `Tiltfile` at the project root with a `local_resource` that runs `./mvnw spring-boot:run -Dspring.profiles.active=dev` for local development. (Full Docker-based Tilt setup is deferred until Spec 03 when the frontend container is added.)

---

### [x] 2.0 Define Data Models and Persistence Layer

Create the four JPA entities, their Spring Data repositories, and the Flyway migration that initializes the schema. Prove persistence works using `@DataJpaTest` against H2.

#### 2.0 Proof Artifact(s)

- Test: `GithubDeploymentRepositoryTest` passes — saves a `GithubDeployment` entity and retrieves it by `repoId` and time range
- Test: `GithubWorkflowRunRepositoryTest` passes — saves and retrieves by `repoId`, `headBranch`, and time range
- Test: `GithubPullRequestRepositoryTest` passes — saves and retrieves merged PRs by `repoId` and `mergedAt` range
- Test: `GithubIssueRepositoryTest` passes — saves and retrieves issues by `repoId` and `createdAt` range
- Screenshot: Dev startup logs show `Successfully applied 1 migration` (or similar Flyway output) and H2 console shows all four tables

#### 2.0 Tasks

- [x] 2.1 Write `GithubDeploymentRepositoryTest` (RED): annotate with `@DataJpaTest`, create a `GithubDeployment` object with hardcoded test data, save it via the repository, and assert `findByRepoIdAndCreatedAtBetween(repoId, start, end)` returns it. The test should fail because neither the entity nor repository exists yet.
- [x] 2.2 Create `GithubDeployment.java` JPA entity with fields: `Long id` (`@GeneratedValue`), `Long githubId`, `String repoId` (format `owner/repo`), `String environment`, `String status`, `Instant createdAt`, `Instant updatedAt`. Annotate with `@Entity`, `@Table(name = "github_deployments")`.
- [x] 2.3 Create `GithubDeploymentRepository.java` extending `JpaRepository<GithubDeployment, Long>` with method `List<GithubDeployment> findByRepoIdAndCreatedAtBetween(String repoId, Instant start, Instant end)`. Run the test — it should go GREEN.
- [x] 2.4 Write `GithubWorkflowRunRepositoryTest` (RED): same pattern — save a `GithubWorkflowRun` and assert `findByRepoIdAndHeadBranchAndCreatedAtBetween` returns it.
- [x] 2.5 Create `GithubWorkflowRun.java` JPA entity with fields: `Long id`, `Long githubId`, `String repoId`, `String name`, `String status`, `String conclusion`, `String headBranch`, `Instant createdAt`, `Instant updatedAt`. Annotate with `@Entity`, `@Table(name = "github_workflow_runs")`.
- [x] 2.6 Create `GithubWorkflowRunRepository.java` with `findByRepoIdAndHeadBranchAndCreatedAtBetween`. Run the test — it should go GREEN.
- [x] 2.7 Write `GithubPullRequestRepositoryTest` (RED): save a `GithubPullRequest` with a non-null `mergedAt` and assert `findByRepoIdAndMergedAtBetween` returns it; also assert a PR with a null `mergedAt` is NOT returned.
- [x] 2.8 Create `GithubPullRequest.java` JPA entity with fields: `Long id`, `Long githubId`, `String repoId`, `String title`, `String labels` (comma-delimited string), `Instant mergedAt` (nullable), `Instant firstCommitAt` (nullable), `String mergeCommitSha`, `Instant createdAt`. Annotate with `@Entity`, `@Table(name = "github_pull_requests")`.
- [x] 2.9 Create `GithubPullRequestRepository.java` with `findByRepoIdAndMergedAtBetween(String repoId, Instant start, Instant end)`. Run the test — it should go GREEN.
- [x] 2.10 Write `GithubIssueRepositoryTest` (RED): save a `GithubIssue` with label `"incident"` and assert `findByRepoIdAndCreatedAtBetween` returns it.
- [x] 2.11 Create `GithubIssue.java` JPA entity with fields: `Long id`, `Long githubId`, `String repoId`, `String title`, `String state`, `String labels` (comma-delimited), `Instant createdAt`, `Instant closedAt` (nullable). Annotate with `@Entity`, `@Table(name = "github_issues")`.
- [x] 2.12 Create `GithubIssueRepository.java` with `findByRepoIdAndCreatedAtBetween(String repoId, Instant start, Instant end)`. Run the test — it should go GREEN.
- [x] 2.13 Create `src/main/resources/db/migration/V1__create_github_tables.sql` with `CREATE TABLE` statements for all four tables, matching the entity field names and types. Verify by running `./mvnw spring-boot:run -Dspring.profiles.active=dev` and checking the H2 console at `http://localhost:8080/h2-console` — all four tables should be visible.

---

### [x] 3.0 Build the GitHub API Client

Implement `GitHubApiClient` using Spring WebClient to fetch all four data types with pagination and time-window filtering. Includes unit tests with mocked HTTP and an integration test against a real public repo.

#### 3.0 Proof Artifact(s)

- Test: `GitHubApiClientTest` passes — mocked WebClient responses verify pagination loop, time-window filtering, and `Authorization: Bearer` header for all four fetch methods
- Test: `GitHubApiClientIntegrationTest` passes when `GITHUB_TEST_TOKEN` env var is set — calls real GitHub API for `octocat/Hello-World` and asserts each list is non-null (may be empty for some data types on that repo)
- CLI: `./mvnw test` fully green after this task

#### 3.0 Tasks

- [x] 3.1 Create `WebClientConfig.java` in `src/main/java/com/liatrio/dora/config/` as a `@Configuration` class with a `@Bean` method that returns a `WebClient` configured with base URL `https://api.github.com`, default header `Accept: application/vnd.github+json`, and default header `X-GitHub-Api-Version: 2022-11-28`.
- [x] 3.2 Write `GitHubApiClientTest` (RED) for `fetchDeployments(owner, repo, token, windowStart)`: use `MockWebServer` (OkHttp) or Spring's `WebClientTest` to return a single-page JSON response and assert the result contains the expected deployment, the `Authorization: Bearer <token>` header was set, and items before `windowStart` are filtered out.
- [x] 3.3 Create `GitHubApiClient.java` as a `@Component` with a constructor-injected `WebClient`. Implement `fetchDeployments(String owner, String repo, String token, Instant windowStart)` that calls `GET /repos/{owner}/{repo}/deployments?per_page=100`, parses the response into a `List<GithubDeployment>`, follows the `Link: <url>; rel="next"` header for pagination, and filters results where `createdAt` is before `windowStart`. Run the test — it should go GREEN.
- [x] 3.4 Write tests for `fetchWorkflowRuns`, `fetchPullRequests`, and `fetchIssues` in `GitHubApiClientTest` (RED): same pattern — mock a response, assert correct query parameters (e.g., `branch=main` for workflow runs, `state=closed` for PRs and issues), and assert time-window filtering.
- [x] 3.5 Implement `fetchWorkflowRuns(owner, repo, token, windowStart)` calling `GET /repos/{owner}/{repo}/actions/runs?branch=main&per_page=100`, `fetchPullRequests(owner, repo, token, windowStart)` calling `GET /repos/{owner}/{repo}/pulls?state=closed&per_page=100` filtering on non-null `merged_at`, and `fetchIssues(owner, repo, token, windowStart)` calling `GET /repos/{owner}/{repo}/issues?state=closed&per_page=100`. Run all tests — they should go GREEN.
- [x] 3.6 Create `GitHubApiClientIntegrationTest.java` annotated with `@SpringBootTest` and `@EnabledIfEnvironmentVariable(named = "GITHUB_TEST_TOKEN", matches = ".+")`. In the single test method, call all four fetch methods against `octocat/Hello-World` using `System.getenv("GITHUB_TEST_TOKEN")` and assert the returned lists are non-null. Run with `GITHUB_TEST_TOKEN=<your PAT> ./mvnw test -Dtest=GitHubApiClientIntegrationTest`.

---

### [x] 4.0 Implement Caching Layer with TTL

Build `GitHubCacheService` that checks the database for a valid (non-expired) cache entry before calling `GitHubApiClient`, and the `DELETE /api/cache/{owner}/{repo}` endpoint for manual cache invalidation.

#### 4.0 Proof Artifact(s)

- Test: `GitHubCacheServiceTest` passes — `GitHubApiClient` is called exactly once across two consecutive `getDeployments` calls within TTL (`Mockito.verify(client, times(1))`)
- Test: `GitHubCacheServiceTest` passes — `GitHubApiClient` is called a second time after TTL is artificially expired
- Test: `CacheControllerTest` passes — `DELETE /api/cache/octocat/Hello-World` returns HTTP 204
- CLI: `curl -X DELETE http://localhost:8080/api/cache/octocat/Hello-World` returns `204 No Content`

#### 4.0 Tasks

- [x] 4.1 Create `src/main/resources/db/migration/V2__create_cache_table.sql` with a `CREATE TABLE github_cache_entries` statement with columns: `id`, `repo_id` (VARCHAR), `data_type` (VARCHAR — values: `DEPLOYMENTS`, `WORKFLOW_RUNS`, `PULL_REQUESTS`, `ISSUES`), `fetched_at` (TIMESTAMP), `ttl_minutes` (INTEGER).
- [x] 4.2 Create `GithubCacheEntry.java` JPA entity mapping to `github_cache_entries`. Create `GithubCacheEntryRepository.java` with `Optional<GithubCacheEntry> findByRepoIdAndDataType(String repoId, String dataType)` and `deleteByRepoId(String repoId)`.
- [x] 4.3 Write `GitHubCacheServiceTest` (RED) with two tests: (a) two calls to `getDeployments` within TTL invoke `GitHubApiClient.fetchDeployments` only once; (b) a call after TTL expiry (achieved by constructing a `GithubCacheEntry` with `fetchedAt` set to 20 minutes ago) invokes the client a second time. Mock `GitHubApiClient` with Mockito.
- [x] 4.4 Create `GitHubCacheService.java` as a `@Service` with constructor-injected `GitHubApiClient`, `GithubCacheEntryRepository`, and `@Value("${github.cache.ttl-minutes:15}") int ttlMinutes`. Implement `getDeployments`, `getWorkflowRuns`, `getPullRequests`, `getIssues` — each method: (1) checks for a non-expired `GithubCacheEntry`, (2) if found, queries the corresponding entity repository for cached rows, (3) if not found or expired, calls `GitHubApiClient`, saves the results to the entity repository, and saves/updates the `GithubCacheEntry`. Run the tests — they should go GREEN.
- [x] 4.5 Write `CacheControllerTest` (RED) using `@WebMvcTest(CacheController.class)`: mock `GitHubCacheService` and assert `DELETE /api/cache/octocat/Hello-World` calls `cacheService.invalidate("octocat", "Hello-World")` and returns HTTP 204.
- [x] 4.6 Create `CacheController.java` as a `@RestController` with `@DeleteMapping("/api/cache/{owner}/{repo}")` that calls `gitHubCacheService.invalidate(owner, repo)` (which deletes all `GithubCacheEntry` rows for that `repoId`) and returns `ResponseEntity.noContent().build()`. Run the test — it should go GREEN.

---

### [x] 5.0 Add Rate Limit Handling and Global Error Mapping

Implement rate-limit detection from GitHub response headers, silent exponential backoff (max 3 retries), and a `@ControllerAdvice` that maps `GitHubRateLimitException` to HTTP 429 with a structured JSON body.

#### 5.0 Proof Artifact(s)

- Test: `GitHubRateLimitHandlerTest` passes — a mocked 403 response with `X-RateLimit-Remaining: 0` triggers exactly 3 retry attempts then throws `GitHubRateLimitException` containing the correct reset timestamp
- Test: `GlobalExceptionHandlerTest` passes — a thrown `GitHubRateLimitException` produces HTTP 429 with body `{ "error": "GitHub rate limit exceeded", "resetsAt": "<ISO timestamp>" }`
- CLI: `./mvnw test` fully green across all 11 test classes in this spec

#### 5.0 Tasks

- [x] 5.1 Create `GitHubRateLimitException.java` as a class extending `RuntimeException` with a constructor that accepts `Instant resetsAt` and stores it as a field with a getter.
- [x] 5.2 Write `GitHubRateLimitHandlerTest` (RED): mock the `WebClient` exchange to return an HTTP 403 response with headers `X-RateLimit-Remaining: 0` and `X-RateLimit-Reset: <unix epoch seconds>`. Assert that calling `fetchDeployments` results in exactly 3 internal retry calls (use a counter or Mockito spy) and ultimately throws a `GitHubRateLimitException` whose `resetsAt` matches the header value.
- [x] 5.3 Add rate-limit handling to `GitHubApiClient`: after each GitHub API response, read `X-RateLimit-Remaining`. If it is `0`, read `X-RateLimit-Reset` (a Unix epoch integer), convert it to an `Instant`, and schedule a retry using a simple loop with `Thread.sleep` delays of 1s, 2s, 4s (exponential backoff). After 3 failed retries, throw `GitHubRateLimitException`. Run the test — it should go GREEN.
- [x] 5.4 Write `GlobalExceptionHandlerTest` (RED) using `@WebMvcTest` with a test controller stub that throws `GitHubRateLimitException`: assert the response is HTTP 429 with `Content-Type: application/json` and body `{ "error": "GitHub rate limit exceeded", "resetsAt": "2026-03-09T12:00:00Z" }`.
- [x] 5.5 Create `GlobalExceptionHandler.java` annotated with `@RestControllerAdvice`. Add a method annotated with `@ExceptionHandler(GitHubRateLimitException.class)` that returns `ResponseEntity` with status 429 and a `Map<String, String>` body containing `"error"` and `"resetsAt"` (formatted as ISO-8601). Run the test — it should go GREEN.
- [x] 5.6 Run `./mvnw test` and confirm all tests across the entire spec are green. Fix any remaining failures before marking this task complete.
