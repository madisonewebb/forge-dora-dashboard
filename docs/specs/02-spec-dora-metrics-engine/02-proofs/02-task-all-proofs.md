# Spec 02 — DORA Metrics Engine: Proof Artifacts

## Task 1.0 — Shared Domain Model

### CLI Output
```
$ ./mvnw test -Dtest=DoraPerformanceBandTest
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Files Created
- `src/main/java/com/liatrio/dora/dto/DoraPerformanceBand.java` — enum with 4 factory methods
- `src/main/java/com/liatrio/dora/dto/WeekDataPoint.java` — record(weekStart, value)
- `src/main/java/com/liatrio/dora/dto/MetricResult.java` — record with `notAvailable()` factory
- `src/main/java/com/liatrio/dora/dto/MetricsMeta.java` — record(owner, repo, windowDays, generatedAt)
- `src/main/java/com/liatrio/dora/dto/MetricsResponse.java` — record(meta, deploymentFrequency, leadTime, changeFailureRate, mttr)

### Verification
- 19 boundary-value tests covering all 4 band classifiers (Deployment Frequency, Lead Time, CFR, MTTR)
- Verified ELITE/HIGH/MEDIUM/LOW thresholds match DORA 2023 research standards

---

## Task 2.0 — Deployment Frequency Calculator

### CLI Output
```
$ ./mvnw test -Dtest=DeploymentFrequencyCalculatorTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Test Scenarios Covered
1. Elite band — 60 deploys/30 days = 2.0/day → ELITE
2. Low band — single deploy over 60 days → LOW
3. Non-success deployments filtered (5 failures + 3 successes → only 3 counted)
4. Weekly time-series: 30-day window → exactly 5 buckets (ceil(30/7))
5. Weekly time-series: 90-day window → exactly 13 buckets (ceil(90/7))
6. `dataAvailable: false` when no successful deployments

---

## Task 3.0 — Lead Time Calculator

### CLI Output
```
$ ./mvnw test -Dtest=LeadTimeCalculatorTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Test Scenarios Covered
1. Median (not average): lead times 1h/3h/10h → result is 3.0h
2. Outlier exclusion: PR merged >7 days before deployment → not correlated → `dataAvailable: false`
3. No data → `dataAvailable: false`
4. 90-day window → 13 buckets
5. Even-count list lower-middle: 1h/2h/4h/8h → 2.0h

---

## Task 4.0 — Change Failure Rate Calculator

### CLI Output
```
$ ./mvnw test -Dtest=ChangeFailureRateCalculatorTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Test Scenarios Covered
1. Labeled PR signal (hotfix) — deployment counted as failure
2. Incident issue signal (opened within 24h) — deployment counted as failure
3. Rollback workflow signal (name matches `rollback*`) — deployment counted as failure
4. Deduplication — all 3 signals on 1 deployment → failure count = 1, CFR = 100%
5. No deployments → `dataAvailable: false`
6. No failure signals → CFR = 0%
7. Weekly buckets: 30 days → 5 buckets

---

## Task 5.0 — MTTR Calculator

### CLI Output
```
$ ./mvnw test -Dtest=MttrCalculatorTest
[INFO] Tests run: 6, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Test Scenarios Covered
1. Issue signal: 3 incidents (1h/3h/5h) → median = 3.0h → HIGH band
2. Deployment-gap fallback: 1 incident issue (< threshold) → 3 failure→success pairs (2h/4h/6h) → median = 4.0h
3. Fewer than 3 incidents, no deployment gaps → `dataAvailable: false` with message containing "incident"
4. Elite band: 3 incidents each resolved in 30 minutes → ELITE
5. 30-day window → 5 weekly buckets
6. No data at all → `dataAvailable: false`

---

## Task 6.0 — Metrics REST API

### CLI Output: Controller Test
```
$ ./mvnw test -Dtest=MetricsControllerTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### CLI Output: Service Test
```
$ ./mvnw test -Dtest=MetricsServiceTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### CLI Output: Exception Handler Test
```
$ ./mvnw test -Dtest=GlobalExceptionHandlerTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### API Contract Verified
- `GET /api/metrics?owner=liatrio&repo=liatrio&token=REDACTED&days=30` → HTTP 200 with full JSON shape
- `GET /api/metrics?...&days=999` → HTTP 400 with `{"error": "days must be 30, 90, or 180"}`
- Omitting `days` parameter defaults to 30
- HTTP 429 propagated from `GitHubRateLimitException`

### Token Redaction
- `TokenRedactionFilter` wraps all requests, replacing `token=<value>` with `token=REDACTED` in query string before any logging

---

## Full Test Suite Summary

```
$ ./mvnw test

Running com.liatrio.dora.dto.DoraPerformanceBandTest            Tests run: 19, Failures: 0
Running com.liatrio.dora.metrics.DeploymentFrequencyCalculatorTest  Tests run: 6,  Failures: 0
Running com.liatrio.dora.metrics.MttrCalculatorTest             Tests run: 6,  Failures: 0
Running com.liatrio.dora.metrics.LeadTimeCalculatorTest         Tests run: 5,  Failures: 0
Running com.liatrio.dora.metrics.ChangeFailureRateCalculatorTest    Tests run: 7,  Failures: 0
Running com.liatrio.dora.repository.GithubWorkflowRunRepositoryTest Tests run: 2,  Failures: 0
Running com.liatrio.dora.repository.GithubIssueRepositoryTest   Tests run: 2,  Failures: 0
Running com.liatrio.dora.repository.GithubPullRequestRepositoryTest Tests run: 2,  Failures: 0
Running com.liatrio.dora.repository.GithubDeploymentRepositoryTest  Tests run: 2,  Failures: 0
Running com.liatrio.dora.controller.CacheControllerTest         Tests run: 1,  Failures: 0
Running com.liatrio.dora.controller.MetricsControllerTest       Tests run: 4,  Failures: 0
Running com.liatrio.dora.DoraApplicationTests                   Tests run: 1,  Failures: 0
Running com.liatrio.dora.service.MetricsServiceTest             Tests run: 2,  Failures: 0
Running com.liatrio.dora.service.GitHubCacheServiceTest         Tests run: 4,  Failures: 0
Running com.liatrio.dora.exception.GlobalExceptionHandlerTest   Tests run: 2,  Failures: 0
Running com.liatrio.dora.client.GitHubApiClientIntegrationTest  Tests run: 4,  Failures: 0, Skipped: 4 (requires GITHUB_TEST_TOKEN)
Running com.liatrio.dora.client.GitHubApiClientTest             Tests run: 7,  Failures: 0

Tests run: 76, Failures: 0, Errors: 0, Skipped: 4
BUILD SUCCESS
```
