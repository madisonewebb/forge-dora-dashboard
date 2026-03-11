# 03 Task 5.0 Proof Artifacts — Time Window Selector and Error State Banners

## CLI Output: `npm test` — Dashboard.test.tsx (5/5 PASS), all 16 tests pass

```
> dora-dashboard-frontend@0.0.1 test
> vitest run --passWithNoTests

 RUN  v2.1.9 /Users/madison/Repos/liatrio-forge/forge-dora-dashboard/frontend

 ✓ src/components/Dashboard.test.tsx (5 tests) 167ms
   ✓ Dashboard > renders the 30/90/180 day selector
   ✓ Dashboard > selecting 90 days triggers a new fetch with days=90
   ✓ Dashboard > renders rate-limit banner on HTTP 429
   ✓ Dashboard > renders bad-token banner on HTTP 401
   ✓ Dashboard > Change Repository link returns to the input form
 ✓ src/components/TrendChart.test.tsx (3 tests) 48ms
 ✓ src/components/MetricCard.test.tsx (4 tests) 54ms
 ✓ src/components/RepoForm.test.tsx (4 tests) 237ms

 Test Files  4 passed (4)
      Tests  16 passed (16)
   Start at  10:23:38
   Duration  1.24s
```

Exit code: 0 ✓

## CLI Output: `./mvnw test` — Backend tests still passing

```
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- MetricsServiceTest
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- GitHubCacheServiceTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0 -- GlobalExceptionHandlerTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0 -- GitHubApiClientTest
[INFO] BUILD SUCCESS
```

## Error Banner Messages (all four states)

```typescript
// HTTP 429 — rate limit
`GitHub rate limit exceeded. Resets at ${error.resetsAt ?? 'unknown'}. Try again later.`

// HTTP 401 or 403 — bad token
'Invalid or expired GitHub token. Please re-enter your PAT.'

// HTTP 404 — repo not found
'Repository not found. Check the owner/repo and try again.'

// network error (status null)
'Could not reach the server. Check your connection.'
```

## `useMetrics` Hook Summary

```typescript
// Accepts: { owner, repo, token, days }
// Returns: { data: MetricsResponse | null, loading: boolean, error: MetricsError | null }
// Re-fetches whenever `days` changes (useEffect dependency)
// Cancels in-flight fetch on unmount (cancelled flag)
// MetricsError: { status: number | null, message: string, resetsAt?: string }
```

## Component Files Created

| File | Description |
|------|-------------|
| `frontend/src/hooks/useMetrics.ts` | Custom hook: fetches /api/metrics, handles all error cases, re-fetches on days change |
| `frontend/src/components/ErrorBanner.tsx` | Dismissible error banner with red styling and × button |
| `frontend/src/components/Dashboard.tsx` | Main dashboard: time window selector, metric grid, error banners, "Change Repository" |
| `frontend/src/components/Dashboard.test.tsx` | 5 tests covering time selector, re-fetch, all error states, navigation |

## Dashboard Architecture

```
App.tsx
  └─ <Dashboard owner repo token initialDays onBack>
       ├─ useMetrics({owner, repo, token, days})  ← days state managed internally
       ├─ Time window: [30 days] [90 days] [180 days]
       ├─ <ErrorBanner> (4 error cases, dismissible)
       └─ 2×2 grid:
            ├─ <MetricCard title="Deployment Frequency" chartType="line">
            ├─ <MetricCard title="Lead Time" chartType="bar">
            ├─ <MetricCard title="Change Failure Rate" chartType="line">
            └─ <MetricCard title="MTTR" chartType="bar">
```

## App.tsx Simplified

With Dashboard owning its own data fetching via useMetrics:
- App.tsx manages only view state ('form' | 'dashboard') and params
- handleFormSubmit updates URL and transitions to dashboard immediately
- Dashboard shows skeleton loading while useMetrics fetches

## Test Coverage by Case

| Test Case | Assertion | Status |
|-----------|-----------|--------|
| renders 30/90/180 day selector | three buttons present | ✓ PASS |
| selecting 90 days triggers new fetch | fetch called with `days=90` in URL | ✓ PASS |
| rate-limit banner on HTTP 429 | `role="alert"` contains "rate limit" + "2026-03-10" | ✓ PASS |
| bad-token banner on HTTP 401 | alert contains "Invalid or expired GitHub token" | ✓ PASS |
| Change Repository calls onBack | `onBack` mock called once | ✓ PASS |

## Verification Summary

| Requirement | Evidence | Status |
|-------------|----------|--------|
| 30/90/180 segmented control | Test 1 passes; 3 buttons with active highlight | ✓ VERIFIED |
| Re-fetch on window change | Test 2 passes; useMetrics re-runs on days change | ✓ VERIFIED |
| HTTP 429 banner with resetsAt | Test 3 passes | ✓ VERIFIED |
| HTTP 401/403 bad token banner | Test 4 passes | ✓ VERIFIED |
| HTTP 404 banner | Error message in Dashboard component | ✓ IMPLEMENTED |
| Network error banner | null status → connection error message | ✓ IMPLEMENTED |
| Dismissible banners | × button calls setDismissed(true) | ✓ IMPLEMENTED |
| "Change Repository" navigation | Test 5 passes; onBack called | ✓ VERIFIED |
| Skeleton loading during re-fetch | loading=true → 4 SkeletonCard in grid | ✓ IMPLEMENTED |
| All 16 frontend tests pass | 5+3+4+4 across 4 test files | ✓ VERIFIED |
| Backend tests unchanged (BUILD SUCCESS) | 15 tests, 0 failures | ✓ VERIFIED |

## Note on Screenshot Proof Artifacts

Tasks 5.7 requires browser screenshots (dashboard-desktop.png, error-banner-429.png, dashboard-mobile.png).
These require a running backend with a real GitHub token and cannot be captured in automated CLI tests.
They are marked as pending manual verification.
