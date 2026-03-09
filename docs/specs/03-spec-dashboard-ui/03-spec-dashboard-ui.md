# 03-spec-dashboard-ui

## Introduction/Overview

This spec covers the React single-page application (SPA) that is the user-facing dashboard. Users enter a GitHub repository and PAT, select a time window, and see all four DORA metrics displayed as color-coded cards with trend charts. The dashboard consumes the `/api/metrics` endpoint from Spec 02. This is the primary deliverable for the capstone demo.

## Goals

- Provide a clean, professional UI where any developer can enter a GitHub repo + PAT and immediately see DORA metrics
- Display all four metrics as cards with value, unit, and color-coded DORA performance band badge (Elite / High / Medium / Low)
- Render trend charts: line charts for frequency metrics (Deployment Frequency, CFR), bar charts for time-based metrics (Lead Time, MTTR)
- Support a time window selector (30 / 90 / 180 days) that re-fetches metrics on change
- Show loading states and handle errors (rate limit, invalid token, no data) gracefully
- Serve the React SPA from a separate nginx container, communicating with the Spring Boot backend via REST

## User Stories

**As a developer**, I want to enter my GitHub repo and PAT in a form so that I can see my team's DORA metrics without any backend configuration.

**As a developer**, I want to see four metric cards with current values and DORA band badges so that I can immediately understand how my team compares to industry benchmarks.

**As a developer**, I want to see trend charts under each metric so that I can understand whether performance is improving or degrading over time.

**As a developer**, I want to select a 30, 90, or 180 day window so that I can view both recent and longer-term trends.

**As a developer**, I want to see a clear error message when my PAT is invalid or rate-limited so that I know what went wrong and what to do next.

**As a developer**, I want the dashboard to show a skeleton loading state while metrics are fetching so that the page feels responsive and professional.

## Demoable Units of Work

### Unit 1: Repo Input Form

**Purpose:** Gives users a way to submit a GitHub repository and PAT to trigger metric loading.

**Functional Requirements:**
- The user shall see a form with three fields: `GitHub Owner/Repo` (e.g., `liatrio/liatrio`), `GitHub Personal Access Token`, and a `Time Window` selector (30 / 90 / 180 days, default 30).
- The form shall validate that `owner/repo` matches the pattern `<owner>/<repo>` (two non-empty segments separated by `/`) before submission.
- The PAT field shall be a password-type input (characters masked).
- On form submit, the app shall call `GET /api/metrics?owner=<>&repo=<>&token=<>&days=<>` and transition to the dashboard view.
- The form shall disable the submit button and show a spinner while a request is in flight.
- The URL shall update to include the `owner`, `repo`, and `days` as query parameters on submit (token is excluded from the URL for security).

**Proof Artifacts:**
- Screenshot: the landing form rendered in a browser with all three fields visible.
- Screenshot: the form in a loading/disabled state after submission.
- Test: `RepoForm.test.tsx` (React Testing Library) asserts validation error for invalid `owner/repo` format and correct API call on valid submission.

---

### Unit 2: Metric Cards with DORA Band Badges

**Purpose:** Displays the current value of all four DORA metrics with visual performance classification.

**Functional Requirements:**
- The system shall render four metric cards: Deployment Frequency, Lead Time for Changes, Change Failure Rate, and MTTR.
- Each card shall display: metric name, current value + unit, and a DORA band badge.
- Badge colors shall be:
  - Elite: green (`#22c55e`)
  - High: blue (`#3b82f6`)
  - Medium: yellow (`#f59e0b`)
  - Low: red (`#ef4444`)
- Cards where `dataAvailable: false` shall display "Not enough data" with a neutral gray badge and the explanatory `message` from the API.
- The cards shall render in a 2×2 responsive grid (2 columns on ≥ 768px, 1 column on mobile).
- The system shall render skeleton placeholder cards (pulsing gray boxes) while the API request is in flight.

**Proof Artifacts:**
- Screenshot: all four metric cards rendered with real data from a public GitHub repo, showing different band colors.
- Screenshot: one card in "Not enough data" state (e.g., MTTR).
- Test: `MetricCard.test.tsx` asserts correct badge color class for each band value and correct "not enough data" rendering.

---

### Unit 3: Trend Charts

**Purpose:** Shows historical trend data for each metric so users can see trajectory over the selected time window.

**Functional Requirements:**
- The system shall render a Chart.js chart below each metric card using the `timeSeries` array from the API response.
- Deployment Frequency: line chart, Y-axis = deployments per week, X-axis = week start date.
- Lead Time for Changes: bar chart, Y-axis = median lead time in hours, X-axis = week start date.
- Change Failure Rate: line chart, Y-axis = CFR percentage, X-axis = week start date.
- MTTR: bar chart, Y-axis = median MTTR in hours, X-axis = week start date.
- Chart colors shall match the metric's current band badge color (e.g., green line for Elite Deployment Frequency).
- Charts for metrics with `dataAvailable: false` shall not render; the card body shall show only the "Not enough data" message.
- All charts shall be responsive (resize with the browser window).
- X-axis labels shall display as `MMM D` formatted dates (e.g., "Feb 10").

**Proof Artifacts:**
- Screenshot: all four charts rendered with real trend data, with the browser window at full width and at ~375px mobile width.
- Test: `TrendChart.test.tsx` asserts that the correct chart type (line vs bar) is rendered for each metric and that empty `timeSeries` data does not crash the chart.

---

### Unit 4: Time Window Selector and Error States

**Purpose:** Allows users to switch time windows and see clear feedback when something goes wrong.

**Functional Requirements:**
- The system shall show a time window selector (radio buttons or segmented control: 30 / 90 / 180 days) above the metric cards.
- Selecting a new window shall trigger a new API call and re-render all cards and charts with fresh data.
- During re-fetch, cards shall show the skeleton loading state; charts shall be hidden.
- The system shall handle the following error states from the API:
  - **HTTP 429 (rate limit)**: Display a banner: "GitHub rate limit exceeded. Resets at <time>. Try again later."
  - **HTTP 401/403 (bad token)**: Display a banner: "Invalid or expired GitHub token. Please re-enter your PAT."
  - **HTTP 404 (repo not found)**: Display a banner: "Repository not found. Check the owner/repo and try again."
  - **Network error**: Display a banner: "Could not reach the server. Check your connection."
- All error banners shall be dismissible.
- The user shall be able to navigate back to the input form (via a "Change Repository" link) without a page refresh.

**Proof Artifacts:**
- Screenshot: the 30/90/180 day selector visible above the metric grid.
- Screenshot: an error banner rendered for the rate-limit case (can be simulated with a mock).
- Test: `Dashboard.test.tsx` asserts that switching the time window triggers a new API call with the updated `days` parameter and that each error response renders the correct banner message.

## Non-Goals (Out of Scope)

1. **User accounts or saved repositories**: No login, no persistence of previously viewed repos in the UI.
2. **Exporting metrics**: No PDF/CSV export in this spec.
3. **Dark mode**: A single light theme only.
4. **Accessibility (WCAG AA)**: Best-effort semantic HTML; full WCAG audit is out of scope.
5. **Comparing multiple repositories side-by-side**: One repository at a time.
6. **Custom chart configurations**: Chart type and colors are fixed per metric type.

## Design Considerations

- Visual style: clean and professional, inspired by modern developer dashboards (e.g., Vercel, Linear). Use a neutral off-white background with white cards and subtle box shadows.
- Typography: system font stack; metric values in large bold numerals; band badges as small rounded pill labels.
- Layout: header with app name ("DORA Metrics Dashboard") and repo name once loaded; 2×2 metric card grid; each card contains: title, value+unit, badge, chart.
- Color palette for DORA bands:
  - Elite: `#22c55e` (green)
  - High: `#3b82f6` (blue)
  - Medium: `#f59e0b` (amber)
  - Low: `#ef4444` (red)
- No external UI component library required; plain CSS modules or Tailwind CSS is acceptable.
- The form and dashboard are different views within the same SPA — no full-page reload between them.

## Repository Standards

- React 18 with TypeScript
- Vite as the build tool
- React Testing Library + Vitest for component tests
- Chart.js with the `react-chartjs-2` wrapper
- `fetch` API for HTTP calls (no Axios unless already present)
- File structure: `src/components/`, `src/pages/`, `src/hooks/`, `src/types/`
- Component test files colocated: `MetricCard.test.tsx` next to `MetricCard.tsx`
- No `any` TypeScript types; define interfaces for all API response shapes

## Technical Considerations

- The React SPA is built with `vite build` and served via an nginx container. The nginx config should proxy `/api/*` requests to the Spring Boot backend container, enabling the frontend to use relative API URLs.
- The GitHub PAT is stored in React component state only (never in `localStorage` or cookies) and is sent only as a query parameter to the backend.
- Environment variable `VITE_API_BASE_URL` controls the backend URL for local dev vs ECS deployment.
- In Tilt (local dev), the frontend container hot-reloads via Vite dev server; in production, it serves the static build.
- The `timeSeries` array from the API always has `windowDays / 7` entries — the chart can assume a consistent array length and use the `weekStart` field for labels.

## Security Considerations

- The PAT lives in React state only and is never written to `localStorage`, `sessionStorage`, or the URL.
- The URL query parameters (owner, repo, days) are safe to share; the token is not included in the URL.
- The nginx container should set `X-Content-Type-Options: nosniff` and `X-Frame-Options: DENY` response headers.
- No sensitive data is rendered in the DOM in a way that could be extracted by third-party scripts.

## Success Metrics

1. **End-to-end demo**: A real public GitHub repo can be entered, and all four metric cards render with real data within 5 seconds on a warm cache.
2. **Responsive layout**: The dashboard is usable on a 375px mobile viewport (single-column cards) and a 1280px desktop (2×2 grid).
3. **Error coverage**: All four defined error states (429, 401/403, 404, network) render the correct banner message.
4. **Test coverage**: `MetricCard`, `TrendChart`, `RepoForm`, and `Dashboard` components each have at least one passing test.

## Open Questions

No open questions at this time.
