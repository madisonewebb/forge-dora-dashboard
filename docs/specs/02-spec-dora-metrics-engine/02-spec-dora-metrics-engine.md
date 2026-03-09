# 02-spec-dora-metrics-engine

## Introduction/Overview

This spec covers the DORA metrics calculation engine and its REST API. Given raw GitHub data from Spec 01, the engine computes all four DORA metrics — Deployment Frequency, Lead Time for Changes, Change Failure Rate (CFR), and Mean Time to Restore (MTTR) — and exposes them as a single REST endpoint. The output powers both the dashboard UI and the AI insights panel.

## Goals

- Calculate all four DORA metrics from persisted GitHub data with clear, testable logic
- Classify each metric against official DORA performance bands (Elite / High / Medium / Low)
- Expose a single REST endpoint that triggers data ingestion (if needed) and returns all metrics in one response
- Support a user-selectable time window (30 / 90 / 180 days) with 30 days as the default
- Return enough time-series data per metric for trend charts in the frontend

## User Stories

**As a developer**, I want to see my Deployment Frequency so that I understand how often my team ships to production.

**As a developer**, I want to see my Lead Time for Changes so that I know how long it takes from first commit to a merged and deployed change.

**As a developer**, I want to see my Change Failure Rate so that I can understand what percentage of my deployments caused issues.

**As a developer**, I want to see my Mean Time to Restore so that I know how quickly my team recovers from incidents.

**As a developer**, I want each metric labeled Elite / High / Medium / Low so that I can immediately understand how my team compares to DORA benchmarks.

**As a developer**, I want to choose a 30, 90, or 180 day window so that I can see short-term and long-term trends.

## Demoable Units of Work

### Unit 1: Deployment Frequency Calculation

**Purpose:** Calculates how often the team deploys to production and expresses it as deployments per day/week.

**Functional Requirements:**
- The system shall count successful GitHub Deployments (status: `success`) in the selected time window.
- The system shall calculate deployments per day as `total_deployments / window_days`.
- The system shall bucket deployments by week for time-series output (one data point per calendar week).
- The system shall classify the result against DORA bands:
  - Elite: ≥ 1 per day
  - High: 1 per week – 1 per day
  - Medium: 1 per month – 1 per week
  - Low: < 1 per month
- If no deployment data exists for the repo, the system shall return a `dataAvailable: false` flag with a helpful message.

**Proof Artifacts:**
- Unit test: `DeploymentFrequencyCalculatorTest` with multiple scenarios (elite, low, no data) asserts correct rate and band classification.
- Unit test: time-series bucketing produces one entry per week with the correct deployment count.

---

### Unit 2: Lead Time for Changes Calculation

**Purpose:** Measures the time from a PR's first commit to deployment, representing how quickly changes reach production.

**Functional Requirements:**
- The system shall calculate Lead Time as: `deployment.created_at - pull_request.first_commit_at` for each PR merged to `main` that was followed by a deployment within the time window.
- The system shall compute the median lead time (not average, to reduce outlier skew) across all qualifying PRs.
- The system shall express lead time in hours for display.
- The system shall bucket median lead time by week for time-series output.
- The system shall classify the result against DORA bands:
  - Elite: < 1 hour
  - High: 1 hour – 1 day
  - Medium: 1 day – 1 week
  - Low: > 1 week
- The system shall use PR `merge_commit_sha` to correlate a PR to a deployment where possible; fall back to chronological proximity (merged PR immediately preceding a deployment).

**Proof Artifacts:**
- Unit test: `LeadTimeCalculatorTest` with controlled datasets asserts correct median calculation and band classification.
- Unit test: asserts that outlier PRs (e.g., a PR merged 6 months before a deployment) are excluded from correlation.

---

### Unit 3: Change Failure Rate Calculation

**Purpose:** Measures what percentage of deployments introduced a failure, using available GitHub signals.

**Functional Requirements:**
- The system shall identify "failure deployments" using three signals (all within the time window):
  1. PRs merged to `main` with labels `hotfix`, `revert`, or `bug`
  2. GitHub Issues labeled `incident` or `outage` that were opened within 24 hours of a deployment
  3. GitHub Actions workflow runs with names matching the pattern `rollback*` or `revert*`
- The system shall calculate CFR as: `failure_deployments / total_deployments * 100` (as a percentage).
- A single deployment may be counted as a failure if it matches any one of the three signals.
- The system shall bucket CFR by week for time-series output.
- The system shall classify the result against DORA bands:
  - Elite: 0–5%
  - High: 5–10%
  - Medium: 10–15%
  - Low: > 15%
- If no deployments exist in the window, the system shall return `dataAvailable: false`.

**Proof Artifacts:**
- Unit test: `ChangeFailureRateCalculatorTest` tests each of the three failure signals independently and in combination.
- Unit test: asserts that a deployment matched by multiple signals is only counted once.

---

### Unit 4: Mean Time to Restore Calculation

**Purpose:** Measures how quickly the team recovers from production incidents, using the best available signal.

**Functional Requirements:**
- The system shall calculate MTTR using two signals (best available, preferred in order):
  1. GitHub Issue open time → close time for issues labeled `incident` (within the time window)
  2. Failed deployment timestamp → next successful deployment timestamp
- The system shall compute the median MTTR in hours across all qualifying incidents.
- The system shall bucket median MTTR by week for time-series output.
- The system shall classify the result against DORA bands:
  - Elite: < 1 hour
  - High: 1–24 hours
  - Medium: 24 hours – 1 week
  - Low: > 1 week
- If fewer than 3 incidents are found in the window, the system shall return `dataAvailable: false` and surface a message: "Not enough incident data — label GitHub Issues with 'incident' to improve this metric."

**Proof Artifacts:**
- Unit test: `MttrCalculatorTest` tests both signals (Issues and failed deployments) independently.
- Unit test: asserts that `dataAvailable: false` is returned when fewer than 3 incidents exist.

---

### Unit 5: Metrics REST API

**Purpose:** Exposes all four metrics in a single endpoint that the dashboard and AI panel consume.

**Functional Requirements:**
- The system shall expose `GET /api/metrics` with required query parameters: `owner`, `repo`, `token`, and optional `days` (default: 30; allowed: 30, 90, 180).
- The system shall trigger GitHub data ingestion (via Spec 01 client/cache) if fresh data is not already cached.
- The system shall return a single JSON response containing all four metrics, each with: `value`, `unit`, `band` (Elite/High/Medium/Low), `dataAvailable` (boolean), `timeSeries` (array of `{ weekStart, value }` objects), and `message` (optional explanatory string).
- The system shall return HTTP 200 on success, 429 on rate limit exhaustion (from Spec 01), and 400 for invalid parameters.
- The response shall include a top-level `meta` object with: `owner`, `repo`, `windowDays`, `generatedAt` (ISO timestamp).

**Example response shape:**
```json
{
  "meta": { "owner": "liatrio", "repo": "liatrio", "windowDays": 30, "generatedAt": "2026-03-09T12:00:00Z" },
  "deploymentFrequency": {
    "value": 2.3, "unit": "deploys/day", "band": "ELITE", "dataAvailable": true,
    "timeSeries": [{ "weekStart": "2026-02-09", "value": 16 }, ...]
  },
  "leadTime": { "value": 4.2, "unit": "hours", "band": "HIGH", "dataAvailable": true, "timeSeries": [...] },
  "changeFailureRate": { "value": 8.5, "unit": "%", "band": "HIGH", "dataAvailable": true, "timeSeries": [...] },
  "mttr": { "value": null, "unit": "hours", "band": null, "dataAvailable": false, "message": "Not enough incident data..." }
}
```

**Proof Artifacts:**
- Integration test: `MetricsControllerIntegrationTest` using `@SpringBootTest` + `MockMvc` with stubbed `GitHubApiClient` asserts correct JSON shape, status codes, and DORA band values.
- Screenshot: `curl "http://localhost:8080/api/metrics?owner=liatrio&repo=liatrio&token=<PAT>&days=30"` returns well-formed JSON with all four metrics.

## Non-Goals (Out of Scope)

1. **GitHub Organization aggregation**: Metrics are per-repository only.
2. **Custom metric thresholds**: DORA band thresholds are hardcoded per DORA research standards; user-configurable thresholds are out of scope.
3. **Metric history persistence**: Calculated metrics are not stored in the database; they are recomputed from cached raw data on each request.
4. **Sprint or Jira correlation**: Lead time uses Git/GitHub data only; no integration with external project management tools.
5. **Comparative benchmarking across multiple repos**: No cross-repo comparison in this spec.

## Design Considerations

No specific UI in this spec. The JSON response shape in Unit 5 is a contract that Specs 03 and 04 depend on — any changes to field names or nesting must be coordinated with those specs.

## Repository Standards

- Spring Boot 3 + Java 17; follow `CLAUDE.md` patterns
- `MetricsService` for calculation logic, `MetricsController` for REST layer
- Constructor injection; SLF4J logging
- `@RestController` + `@GetMapping` with `@RequestParam`
- `@ControllerAdvice` for exception-to-HTTP mapping
- TDD: RED → GREEN → REFACTOR for every calculator class
- Test class naming: `[ClassName]Test`; use `@WebMvcTest` for controller, plain JUnit for calculators

## Technical Considerations

- Each metric is implemented as a separate `@Component` calculator class (`DeploymentFrequencyCalculator`, `LeadTimeCalculator`, etc.) injected into `MetricsService`. This makes each calculator independently testable.
- Week bucketing uses ISO week start (Monday) via `java.time.temporal.WeekFields.ISO`.
- The `timeSeries` array should always have exactly `windowDays / 7` entries (rounded up), filling weeks with zero values where no data exists — this ensures charts render consistently.
- Median calculation uses `java.util.Collections.sort` + middle-index selection; do not use average.
- The `token` parameter is passed through to the GitHub client and is never logged.

## Security Considerations

- The `token` query parameter must never be logged at any log level.
- Add a Servlet filter or Spring interceptor that redacts the `token` value from access logs.
- No metrics data is persisted; raw GitHub data in the DB contains no credentials.

## Success Metrics

1. **Correctness**: All four metric calculators pass unit tests with at least 3 scenario variations each (normal data, edge case, no data).
2. **API contract**: The `/api/metrics` endpoint returns valid JSON matching the documented schema for a real public GitHub repo.
3. **Performance**: The endpoint responds within 3 seconds when data is cached (cache hit path).
4. **Band accuracy**: Deployment Frequency and Lead Time band classifications match DORA 2023 benchmark thresholds.

## Open Questions

No open questions at this time.
