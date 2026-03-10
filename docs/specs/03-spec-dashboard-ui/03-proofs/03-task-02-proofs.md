# 03 Task 2.0 Proof Artifacts — Build the Repo Input Form

## CLI Output: `npm test` — RepoForm.test.tsx (4/4 PASS)

```
> dora-dashboard-frontend@0.0.1 test
> vitest run --passWithNoTests

 RUN  v2.1.9 /Users/madison/Repos/liatrio-forge/forge-dora-dashboard/frontend

 ✓ src/components/RepoForm.test.tsx (4 tests) 204ms
   ✓ RepoForm > renders all three form fields
   ✓ RepoForm > shows validation error for invalid owner/repo format
   ✓ RepoForm > calls onSubmit with correct args on valid submission
   ✓ RepoForm > disables submit button when loading

 Test Files  1 passed (1)
      Tests  4 passed (4)
   Start at  09:55:13
   Duration  1.02s (transform 46ms, setup 117ms, collect 109ms, tests 204ms, environment 367ms, prepare 72ms)
```

Exit code: 0 ✓

## `frontend/src/components/RepoForm.tsx` — Form implementation

```tsx
// Key implementation highlights:
// - Props: { onSubmit: (owner, repo, token, days) => void; loading: boolean }
// - Validates ownerRepo against /^[^/]+\/[^/]+$/ — rejects "liatrio" (no slash)
// - Sets error "Format must be owner/repo" on invalid input
// - Password input type for PAT (masked by browser)
// - Radio group: 30 / 90 / 180 days, default 30
// - Submit button: disabled + "Loading…" text when loading prop is true
```

## `frontend/src/App.tsx` — View state management and URL sync

```tsx
// Key implementation highlights:
// - State: view ('form'|'dashboard'), params, loading, fetchError
// - On page load: reads owner/repo/days from URL query params → pre-fills form
// - handleFormSubmit: sets loading=true, updates URL (?owner=&repo=&days= — no token),
//   calls GET /api/metrics, on success transitions to 'dashboard' view
// - fetchError displayed as <div role="alert"> above the form
// - Dashboard view: placeholder showing owner/repo params (replaced in Task 5)
```

## Validation: Token never in URL

The `handleFormSubmit` function in `App.tsx` explicitly excludes the token:

```ts
const sp = new URLSearchParams({ owner, repo, days: String(days) })
window.history.pushState({}, '', `?${sp.toString()}`)
```

Token stays in React state only — never written to URL, `localStorage`, or `sessionStorage`. ✓

## Test Coverage by Case

| Test Case | Assertion | Status |
|-----------|-----------|--------|
| renders all three form fields | `getByLabelText(/owner\/repo/i)`, PAT input, radio group present | ✓ PASS |
| shows validation error for invalid format | "Format must be owner/repo" shown; `fetch` not called | ✓ PASS |
| calls onSubmit with correct args | `mockSubmit` called with `('liatrio', 'liatrio', 'fake-token', 30)` | ✓ PASS |
| disables submit button when loading | button has `disabled` attribute; shows "Loading…" | ✓ PASS |

## Verification Summary

| Requirement | Evidence | Status |
|-------------|----------|--------|
| Landing form with owner/repo, PAT, time window | RepoForm.tsx renders all 3 fields | ✓ VERIFIED |
| Validation: rejects input without slash | Test 2 passes — error message shown, no fetch call | ✓ VERIFIED |
| PAT masked in browser | `type="password"` on PAT input | ✓ VERIFIED |
| Submit disabled during loading | Test 4 passes — button disabled, shows "Loading…" | ✓ VERIFIED |
| URL updated with owner/repo/days (no token) | URLSearchParams in App.tsx excludes token key | ✓ VERIFIED |
| URL params pre-fill form on page load | useEffect reads owner/repo/days from URLSearchParams | ✓ VERIFIED |
| View transitions: form → dashboard | setView('dashboard') on successful API response | ✓ VERIFIED |
