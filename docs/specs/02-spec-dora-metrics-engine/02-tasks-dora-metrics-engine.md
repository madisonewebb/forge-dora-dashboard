# 02-tasks-dora-metrics-engine

## Relevant Files

### Production Code
- `src/main/java/com/liatrio/dora/dto/DoraPerformanceBand.java` — Enum with four bands: ELITE, HIGH, MEDIUM, LOW
- `src/main/java/com/liatrio/dora/dto/WeekDataPoint.java` — Java record holding `weekStart` (LocalDate) and `value` (Double) for time-series charts
- `src/main/java/com/liatrio/dora/dto/MetricResult.java` — Java record holding `value`, `unit`, `band`, `dataAvailable`, `timeSeries`, and optional `message` for one DORA metric
- `src/main/java/com/liatrio/dora/dto/MetricsMeta.java` — Java record holding `owner`, `repo`, `windowDays`, `generatedAt` for the response envelope
- `src/main/java/com/liatrio/dora/dto/MetricsResponse.java` — Java record holding `meta` and all four `MetricResult` fields
- `src/main/java/com/liatrio/dora/metrics/DeploymentFrequencyCalculator.java` — `@Component` that computes deploys/day, weekly buckets, and DORA band from a list of `GithubDeployment`
- `src/main/java/com/liatrio/dora/metrics/LeadTimeCalculator.java` — `@Component` that computes median lead time in hours and weekly buckets from PR + deployment lists
- `src/main/java/com/liatrio/dora/metrics/ChangeFailureRateCalculator.java` — `@Component` that identifies failure deployments via three signals and computes CFR percentage with weekly buckets
- `src/main/java/com/liatrio/dora/metrics/MttrCalculator.java` — `@Component` that computes median MTTR in hours via Issue and deployment-gap signals with weekly buckets
- `src/main/java/com/liatrio/dora/service/MetricsService.java` — `@Service` that calls all four calculators and assembles `MetricsResponse`
- `src/main/java/com/liatrio/dora/controller/MetricsController.java` — `@RestController` exposing `GET /api/metrics` with `owner`, `repo`, `token`, `days` parameters
- `src/main/java/com/liatrio/dora/config/TokenRedactionFilter.java` — Servlet filter that redacts the `token` query parameter value from access logs
- `src/main/java/com/liatrio/dora/exception/GlobalExceptionHandler.java` — Existing file; add handler for `IllegalArgumentException` → HTTP 400

### Test Code
- `src/test/java/com/liatrio/dora/dto/DoraPerformanceBandTest.java` — Plain JUnit 5; tests band classification boundary values
- `src/test/java/com/liatrio/dora/metrics/DeploymentFrequencyCalculatorTest.java` — Plain JUnit 5; tests all scenarios including weekly bucketing
- `src/test/java/com/liatrio/dora/metrics/LeadTimeCalculatorTest.java` — Plain JUnit 5; tests median calculation and outlier exclusion
- `src/test/java/com/liatrio/dora/metrics/ChangeFailureRateCalculatorTest.java` — Plain JUnit 5; tests each failure signal and deduplication
- `src/test/java/com/liatrio/dora/metrics/MttrCalculatorTest.java` — Plain JUnit 5; tests both MTTR signals and the `dataAvailable: false` guard
- `src/test/java/com/liatrio/dora/service/MetricsServiceTest.java` — `@ExtendWith(MockitoExtension.class)`; verifies all four calculators are called and response is assembled
- `src/test/java/com/liatrio/dora/controller/MetricsControllerTest.java` — `@WebMvcTest(MetricsController.class)`; tests JSON contract, parameter validation, and error mapping

### Notes

- All tests run with `./mvnw test`. Run a single test class with `./mvnw test -Dtest=ClassName`.
- Follow TDD strictly: write the failing test first (RED), implement the minimum to pass (GREEN), then clean up (REFACTOR).
- Use `@ExtendWith(MockitoExtension.class)` for calculator and service tests; `@WebMvcTest` for controller tests; plain JUnit 5 for DTO/enum tests.
- Use constructor injection everywhere — no `@Autowired` on fields.
- Log with `private static final Logger log = LoggerFactory.getLogger(ClassName.class);` — never `System.out.println`.
- The `token` parameter must never appear in any log statement at any level.
- Week bucketing uses `java.time.temporal.WeekFields.ISO` (Monday start). Each window always produces exactly `Math.ceil(windowDays / 7.0)` buckets; fill empty weeks with `0.0`.
- Median calculation: sort a `List<Double>`, pick the middle element (lower-middle for even-length lists). Do **not** use average.

---

## Tasks

### [x] 1.0 Define Shared Domain Model (DORA Bands, Metric Result, Time-Series)

Create the shared value types used by all four calculators and the REST API: the `DoraPerformanceBand` enum, `MetricResult` record, `WeekDataPoint` record, and `MetricsResponse` record. This is the foundation every subsequent task depends on.

#### 1.0 Proof Artifact(s)

- Test: `DoraPerformanceBandTest` passes — asserts band classification helper methods return the correct band for boundary values (e.g., exactly 1/day → ELITE, just below 1/week → MEDIUM)
- CLI: `./mvnw test -Dtest=DoraPerformanceBandTest` exits 0

#### 1.0 Tasks

- [x] 1.1 Write `DoraPerformanceBandTest` (RED): create the test class in `src/test/java/com/liatrio/dora/dto/` using plain JUnit 5 (no Spring context needed). Write three test methods: `forDeploymentFrequency_elite()` asserts that a rate of 1.0 deploys/day returns `ELITE`; `forDeploymentFrequency_medium()` asserts that a rate of 0.14 (≈ 1/week) returns `MEDIUM`; `forLeadTime_high()` asserts that 4.0 hours returns `HIGH`. The tests should fail with a compilation error because `DoraPerformanceBand` does not exist yet.
- [x] 1.2 Create `DoraPerformanceBand.java` in `src/main/java/com/liatrio/dora/dto/` as a Java `enum` with four constants: `ELITE`, `HIGH`, `MEDIUM`, `LOW`. Add two static factory methods: `forDeploymentFrequency(double deploysPerDay)` and `forLeadTime(double hours)` implementing the DORA threshold logic from the spec. Run the test — it should go GREEN.
- [x] 1.3 Extend `DoraPerformanceBand` with two more static factory methods: `forChangeFailureRate(double percent)` and `forMttr(double hours)` using their respective DORA thresholds from the spec. Write additional test methods in `DoraPerformanceBandTest` for each boundary value (e.g., exactly 5% CFR → ELITE, 5.1% → HIGH). Run the tests — they should be GREEN.
- [x] 1.4 Create `WeekDataPoint.java` in `src/main/java/com/liatrio/dora/dto/` as a Java record: `record WeekDataPoint(java.time.LocalDate weekStart, Double value)`. No test needed — it is a pure data carrier with no logic.
- [x] 1.5 Create `MetricResult.java` in `src/main/java/com/liatrio/dora/dto/` as a Java record with fields: `Double value`, `String unit`, `DoraPerformanceBand band`, `boolean dataAvailable`, `List<WeekDataPoint> timeSeries`, `String message`. Add a static factory method `notAvailable(String message)` that returns an instance with `dataAvailable=false`, `value=null`, `band=null`, `timeSeries=List.of()`. No test needed for the record itself.
- [x] 1.6 Create `MetricsMeta.java` in `src/main/java/com/liatrio/dora/dto/` as a Java record: `record MetricsMeta(String owner, String repo, int windowDays, java.time.Instant generatedAt)`.
- [x] 1.7 Create `MetricsResponse.java` in `src/main/java/com/liatrio/dora/dto/` as a Java record: `record MetricsResponse(MetricsMeta meta, MetricResult deploymentFrequency, MetricResult leadTime, MetricResult changeFailureRate, MetricResult mttr)`.
- [x] 1.8 Run `./mvnw test` to confirm all existing tests still pass with the new `dto` package added. Fix any compilation errors before continuing.

---

### [x] 2.0 Implement Deployment Frequency Calculator

Build `DeploymentFrequencyCalculator` as a standalone `@Component` that computes deployments-per-day, weekly time-series buckets, and DORA band classification from a list of `GithubDeployment` entities.

#### 2.0 Proof Artifact(s)

- Test: `DeploymentFrequencyCalculatorTest` passes all scenarios: elite rate (≥1/day), low rate (<1/month), empty data returning `dataAvailable: false`, and weekly time-series producing the correct number of buckets for a 30-day window
- CLI: `./mvnw test -Dtest=DeploymentFrequencyCalculatorTest` exits 0

#### 2.0 Tasks

- [x] 2.1 Write `DeploymentFrequencyCalculatorTest` (RED) in `src/test/java/com/liatrio/dora/metrics/` using plain JUnit 5 with `@ExtendWith(MockitoExtension.class)`. Write four test methods:
  - `calculate_elite_returnsCorrectRateAndBand()`: pass 60 deployments with status `"success"` spread over 30 days; assert `value` ≈ 2.0, `band == ELITE`, `dataAvailable == true`.
  - `calculate_low_returnsLowBand()`: pass 0 deployments over 30 days; assert `dataAvailable == false`.
  - `calculate_filtersNonSuccessDeployments()`: pass 5 deployments with status `"failure"` and 3 with `"success"`; assert only 3 are counted.
  - `calculate_weeklyTimeSeries_has5BucketsFor30Days()`: pass any deployments over a 30-day window; assert `timeSeries.size() == 5` (⌈30/7⌉ = 5).
  The tests should fail with a compilation error because `DeploymentFrequencyCalculator` does not exist.
- [x] 2.2 Create `DeploymentFrequencyCalculator.java` in `src/main/java/com/liatrio/dora/metrics/` as a `@Component`. Add the public method signature: `MetricResult calculate(List<GithubDeployment> deployments, int windowDays)`. Return `MetricResult.notAvailable("No deployment data found for this repository.")` as a stub. Run the test — the `dataAvailable == false` test should pass; the others should still fail.
- [x] 2.3 Implement the full `calculate` logic:
  1. Filter for `status.equals("success")`.
  2. If the filtered list is empty, return `MetricResult.notAvailable(...)`.
  3. Compute `deploysPerDay = successfulDeployments.size() / (double) windowDays`.
  4. Classify band via `DoraPerformanceBand.forDeploymentFrequency(deploysPerDay)`.
  5. Build weekly time-series: iterate ISO weeks from `windowStart` (= `now minus windowDays`) to `now`, counting deployments whose `createdAt` falls in each week. Produce exactly `Math.ceil(windowDays / 7.0)` buckets, filling empty weeks with `0.0`.
  6. Return `new MetricResult(deploysPerDay, "deploys/day", band, true, timeSeries, null)`.
  Run `./mvnw test -Dtest=DeploymentFrequencyCalculatorTest` — all four tests should go GREEN.
- [x] 2.4 Refactor: extract the weekly bucketing logic into a private helper method `buildWeeklyBuckets(List<Instant> timestamps, int windowDays)` so it can be reused visually (not shared yet — just clean it up within this class). Run tests again to confirm still GREEN.

---

### [x] 3.0 Implement Lead Time for Changes Calculator

Build `LeadTimeCalculator` as a `@Component` that correlates merged PRs to deployments, computes median lead time in hours using ISO-week bucketing, and classifies the result against DORA bands.

#### 3.0 Proof Artifact(s)

- Test: `LeadTimeCalculatorTest` passes all scenarios: correct median (not average) across multiple PRs, outlier PR exclusion (PR merged >7 days before any deployment is not correlated), empty data returning `dataAvailable: false`, and weekly bucketing producing the correct bucket count
- CLI: `./mvnw test -Dtest=LeadTimeCalculatorTest` exits 0

#### 3.0 Tasks

- [x] 3.1 Write `LeadTimeCalculatorTest` (RED) in `src/test/java/com/liatrio/dora/metrics/`. Write four test methods:
  - `calculate_computesMedianNotAverage()`: create 3 PRs with `firstCommitAt` values that produce lead times of 1h, 3h, and 10h; assert `value == 3.0` (median), not the average (4.67h).
  - `calculate_excludesOutlierPrs()`: create a PR whose `mergedAt` is 10 days before the earliest deployment; assert it is NOT included in the lead time calculation (result reflects only correlated PRs).
  - `calculate_noData_returnsNotAvailable()`: pass empty lists; assert `dataAvailable == false`.
  - `calculate_weeklyTimeSeries_correctBucketCount()`: pass data over a 90-day window; assert `timeSeries.size() == 13` (⌈90/7⌉ = 13).
  Tests should fail with a compilation error.
- [x] 3.2 Create `LeadTimeCalculator.java` in `src/main/java/com/liatrio/dora/metrics/` as a `@Component` with method: `MetricResult calculate(List<GithubPullRequest> pullRequests, List<GithubDeployment> deployments, int windowDays)`. Return `MetricResult.notAvailable("No lead time data available.")` as a stub. Run tests — the `noData` test should pass.
- [x] 3.3 Implement the correlation logic:
  1. For each merged PR in the window, find the earliest deployment that occurred after the PR's `mergedAt` and within 7 days of it (chronological proximity). If the PR has a `mergeCommitSha`, prefer matching to a deployment by SHA if available.
  2. Compute lead time in hours as `deployment.createdAt - pr.firstCommitAt` for each correlated pair.
  3. If no pairs found, return `MetricResult.notAvailable(...)`.
  4. Sort lead times and pick the median (middle element for odd count, lower-middle for even).
  5. Classify band via `DoraPerformanceBand.forLeadTime(medianHours)`.
  6. Build weekly time-series of median lead time per ISO week (fill empty weeks with `0.0`).
  7. Return `new MetricResult(medianHours, "hours", band, true, timeSeries, null)`.
  Run `./mvnw test -Dtest=LeadTimeCalculatorTest` — all tests should go GREEN.
- [x] 3.4 Run `./mvnw test` to confirm all tests across Specs 01 and 02 remain GREEN.

---

### [x] 4.0 Implement Change Failure Rate Calculator

Build `ChangeFailureRateCalculator` as a `@Component` that identifies failure deployments from three signals (labeled PRs, incident-adjacent issues, rollback workflow runs), deduplicates them, and returns a percentage with weekly time-series.

#### 4.0 Proof Artifact(s)

- Test: `ChangeFailureRateCalculatorTest` passes all scenarios: each of the three failure signals independently triggers a failure count, a deployment matching multiple signals is counted only once (deduplication), correct percentage calculation, and `dataAvailable: false` when no deployments exist
- CLI: `./mvnw test -Dtest=ChangeFailureRateCalculatorTest` exits 0

#### 4.0 Tasks

- [x] 4.1 Write `ChangeFailureRateCalculatorTest` (RED) in `src/test/java/com/liatrio/dora/metrics/`. Write five test methods:
  - `calculate_labeledPrSignal_countsFailure()`: one deployment + one PR with label `"hotfix"` merged before it; assert `value > 0`.
  - `calculate_incidentIssueSignal_countsFailure()`: one deployment + one issue labeled `"incident"` opened within 24 hours of that deployment; assert `value > 0`.
  - `calculate_rollbackWorkflowSignal_countsFailure()`: one deployment + one workflow run named `"rollback-production"`; assert `value > 0`.
  - `calculate_deduplication_multipleSignalsCounts1()`: one deployment matched by all three signals; assert the failure count is 1 (not 3).
  - `calculate_noDeployments_returnsNotAvailable()`: pass empty deployment list; assert `dataAvailable == false`.
  Tests should fail with a compilation error.
- [x] 4.2 Create `ChangeFailureRateCalculator.java` in `src/main/java/com/liatrio/dora/metrics/` as a `@Component` with method: `MetricResult calculate(List<GithubDeployment> deployments, List<GithubPullRequest> pullRequests, List<GithubIssue> issues, List<GithubWorkflowRun> workflowRuns, int windowDays)`. Return `MetricResult.notAvailable("No deployment data found.")` as a stub. Run tests — the `noDeployments` test should pass.
- [x] 4.3 Implement the three failure signals. Use a `Set<Long>` of deployment GitHub IDs to track counted failures (for deduplication):
  1. **Labeled PR signal**: for each deployment, check if any PR with label `"hotfix"`, `"revert"`, or `"bug"` was merged in the 24 hours before the deployment's `createdAt`. If yes, add the deployment's `githubId` to the failure set.
  2. **Incident issue signal**: for each deployment, check if any issue labeled `"incident"` or `"outage"` was opened within 24 hours after the deployment's `createdAt`. If yes, add the deployment's `githubId` to the failure set.
  3. **Rollback workflow signal**: for each deployment, check if any workflow run whose `name` matches `rollback*` or `revert*` (case-insensitive) was created within 24 hours after the deployment. If yes, add the deployment's `githubId` to the failure set.
  4. Calculate CFR: `failureSet.size() / (double) totalDeployments * 100`.
  5. Classify band via `DoraPerformanceBand.forChangeFailureRate(cfr)`.
  6. Build weekly time-series of CFR per ISO week (fill empty weeks with `0.0`).
  Run `./mvnw test -Dtest=ChangeFailureRateCalculatorTest` — all tests should go GREEN.
- [x] 4.4 Run `./mvnw test` to confirm all tests remain GREEN.

---

### [x] 5.0 Implement Mean Time to Restore Calculator

Build `MttrCalculator` as a `@Component` that computes median MTTR in hours using two signals (incident Issues, then failed-to-successful deployment gaps), with weekly bucketing and the `dataAvailable: false` guard when fewer than 3 incidents are found.

#### 5.0 Proof Artifact(s)

- Test: `MttrCalculatorTest` passes all scenarios: Issue-based MTTR (open → close time), deployment-gap MTTR fallback, `dataAvailable: false` when fewer than 3 incidents exist with the correct message, and correct band classification
- CLI: `./mvnw test -Dtest=MttrCalculatorTest` exits 0

#### 5.0 Tasks

- [x] 5.1 Write `MttrCalculatorTest` (RED) in `src/test/java/com/liatrio/dora/metrics/`. Write four test methods:
  - `calculate_issueSignal_computesOpenToCloseTime()`: create 3 `GithubIssue` records labeled `"incident"` with known `createdAt` and `closedAt` values; assert the computed `value` equals the expected median hours.
  - `calculate_deploymentGapFallback_usedWhenFewIssues()`: pass fewer than 3 incident issues and a sequence of deployments where one has status `"failure"` followed by a `"success"`; assert the deployment gap is used and `dataAvailable == true`.
  - `calculate_fewerThan3Incidents_returnsNotAvailable()`: pass 2 incident issues and no failed deployments; assert `dataAvailable == false` and `message` contains `"incident"`.
  - `calculate_elite_returnsEliteBand()`: pass 3 incidents each resolved in 30 minutes; assert `band == ELITE`.
  Tests should fail with a compilation error.
- [x] 5.2 Create `MttrCalculator.java` in `src/main/java/com/liatrio/dora/metrics/` as a `@Component` with method: `MetricResult calculate(List<GithubIssue> issues, List<GithubDeployment> deployments, int windowDays)`. Return `MetricResult.notAvailable("Not enough incident data — label GitHub Issues with 'incident' to improve this metric.")` as a stub. Run tests — the `fewerThan3Incidents` test should pass.
- [x] 5.3 Implement the two MTTR signals (preferred order):
  1. **Issue signal**: filter issues with label `"incident"` that have a non-null `closedAt`. If there are ≥ 3 such issues in the window, compute MTTR as `closedAt - createdAt` in hours for each and take the median.
  2. **Deployment gap fallback**: if fewer than 3 incident issues are found, scan the deployments list (sorted by `createdAt` ascending) for a `"failure"` deployment followed by a `"success"` deployment. Compute gap in hours for each such pair. If ≥ 3 pairs found, take the median. Otherwise return `MetricResult.notAvailable(...)`.
  3. Classify band via `DoraPerformanceBand.forMttr(medianHours)`.
  4. Build weekly time-series of median MTTR per ISO week (fill empty weeks with `0.0`).
  Run `./mvnw test -Dtest=MttrCalculatorTest` — all tests should go GREEN.
- [x] 5.4 Run `./mvnw test` to confirm all tests remain GREEN.

---

### [x] 6.0 Expose Metrics REST API with Token Redaction

Wire all four calculators into `MetricsService`, expose `GET /api/metrics` via `MetricsController`, add a servlet filter that redacts the `token` parameter from access logs, and validate the full JSON response contract.

#### 6.0 Proof Artifact(s)

- Test: `MetricsControllerTest` (`@WebMvcTest`) passes — stubbed `MetricsService` returns a fixed response; asserts HTTP 200 JSON shape includes `meta.owner`, `deploymentFrequency.band`, HTTP 400 for invalid `days` value, and HTTP 429 propagation
- Test: `MetricsServiceTest` passes — Mockito-injected calculator mocks are called with the correct window, and the assembled `MetricsResponse` contains all four metric results
- CLI: `./mvnw test` fully green across all test classes in Specs 01 and 02

#### 6.0 Tasks

- [x] 6.1 Write `MetricsServiceTest` (RED) in `src/test/java/com/liatrio/dora/service/` using `@ExtendWith(MockitoExtension.class)`. Mock `GitHubCacheService`, `DeploymentFrequencyCalculator`, `LeadTimeCalculator`, `ChangeFailureRateCalculator`, and `MttrCalculator`. Write two test methods:
  - `getMetrics_callsAllFourCalculators()`: call `metricsService.getMetrics("owner", "repo", "token", 30)`; use `Mockito.verify` to assert each of the four calculators' `calculate` methods was called exactly once.
  - `getMetrics_assemblesToMetricsResponse()`: stub each calculator to return a fixed `MetricResult`; assert the returned `MetricsResponse` has `meta.owner == "owner"`, `meta.windowDays == 30`, and each metric result matches the stub.
  Tests should fail with a compilation error.
- [x] 6.2 Create `MetricsService.java` in `src/main/java/com/liatrio/dora/service/` as a `@Service`. Constructor-inject `GitHubCacheService`, `DeploymentFrequencyCalculator`, `LeadTimeCalculator`, `ChangeFailureRateCalculator`, and `MttrCalculator`. Implement `MetricsResponse getMetrics(String owner, String repo, String token, int windowDays)`:
  1. Compute `windowStart = Instant.now().minus(windowDays, ChronoUnit.DAYS)`.
  2. Call `gitHubCacheService.getDeployments/getWorkflowRuns/getPullRequests/getIssues` to retrieve all four data lists.
  3. Call each calculator's `calculate` method with the appropriate lists and `windowDays`.
  4. Build `MetricsMeta` with `owner`, `repo`, `windowDays`, and `Instant.now()`.
  5. Return `new MetricsResponse(meta, deployFreq, leadTime, cfr, mttr)`.
  Run `./mvnw test -Dtest=MetricsServiceTest` — both tests should go GREEN.
- [x] 6.3 Write `MetricsControllerTest` (RED) in `src/test/java/com/liatrio/dora/controller/` using `@WebMvcTest(MetricsController.class)` and `@MockBean MetricsService`. Write three test methods:
  - `getMetrics_returns200WithCorrectShape()`: stub `metricsService.getMetrics(...)` to return a fixed `MetricsResponse`; perform `GET /api/metrics?owner=liatrio&repo=liatrio&token=abc&days=30`; assert HTTP 200 and JSON path `$.meta.owner == "liatrio"` and `$.deploymentFrequency` exists.
  - `getMetrics_invalidDays_returns400()`: perform `GET /api/metrics?owner=liatrio&repo=liatrio&token=abc&days=999`; assert HTTP 400.
  - `getMetrics_defaultDays_uses30()`: perform `GET /api/metrics?owner=liatrio&repo=liatrio&token=abc` (no `days` param); verify `metricsService.getMetrics` was called with `windowDays == 30`.
  Tests should fail with a compilation error.
- [x] 6.4 Create `MetricsController.java` in `src/main/java/com/liatrio/dora/controller/` as a `@RestController` with `@RequestMapping("/api/metrics")`. Constructor-inject `MetricsService`. Add `@GetMapping` method with `@RequestParam String owner`, `@RequestParam String repo`, `@RequestParam String token`, `@RequestParam(defaultValue = "30") int days`:
  1. Validate that `days` is one of `30`, `90`, or `180`; if not, throw `new IllegalArgumentException("days must be 30, 90, or 180")`.
  2. Call `metricsService.getMetrics(owner, repo, token, days)` and return `ResponseEntity.ok(result)`.
  Run `./mvnw test -Dtest=MetricsControllerTest` — all three tests should go GREEN.
- [x] 6.5 Update `GlobalExceptionHandler.java` to add a handler for `IllegalArgumentException`: return HTTP 400 with body `{ "error": "<exception message>" }`. Add a test method to the existing `GlobalExceptionHandlerTest` that asserts a thrown `IllegalArgumentException` produces HTTP 400 with the correct JSON body. Run `./mvnw test -Dtest=GlobalExceptionHandlerTest` — should be GREEN.
- [x] 6.6 Create `TokenRedactionFilter.java` in `src/main/java/com/liatrio/dora/config/` as a class implementing `jakarta.servlet.Filter`, annotated with `@Component`. In the `doFilter` method, wrap the incoming `HttpServletRequest` in a custom `HttpServletRequestWrapper` that overrides `getQueryString()` to replace the `token=<value>` segment with `token=REDACTED` using a regex. Call `chain.doFilter(wrappedRequest, response)`. Add `log.debug("Request: {} {}", wrappedRequest.getMethod(), wrappedRequest.getRequestURI())` — note: log URI, not query string, to avoid any accidental token leakage.
- [x] 6.7 Run `./mvnw test` to confirm all tests across both Spec 01 and Spec 02 are fully GREEN. Fix any remaining failures before marking this task complete.
- [x] 6.8 Manually verify the full response contract: start the app with `./mvnw spring-boot:run -Dspring.profiles.active=dev`, then run `curl "http://localhost:8080/api/metrics?owner=liatrio&repo=liatrio&token=<YOUR_PAT>&days=30"`. Confirm the response contains `meta`, `deploymentFrequency`, `leadTime`, `changeFailureRate`, and `mttr` fields. Save a copy of the response (with the token value replaced with `REDACTED`) to `docs/specs/02-spec-dora-metrics-engine/02-proofs/curl-metrics-response.json`.
