# 03-tasks-dashboard-ui

## Relevant Files

### Frontend Source
- `frontend/package.json` — npm manifest with all dependencies (React 18, TypeScript, Vite, Chart.js, react-chartjs-2, React Testing Library, Vitest, jsdom)
- `frontend/tsconfig.json` — TypeScript compiler config targeting ESNext with React JSX
- `frontend/vite.config.ts` — Vite config with Vitest test environment (jsdom) and setupFiles
- `frontend/index.html` — HTML entry point for the SPA
- `frontend/src/main.tsx` — React 18 root render entry point
- `frontend/src/App.tsx` — Top-level app component managing form ↔ dashboard view state and URL params
- `frontend/src/setupTests.ts` — Vitest global test setup (jest-dom matchers)
- `frontend/src/types/metrics.ts` — TypeScript interfaces mirroring the Spec 02 JSON contract (`MetricsResponse`, `MetricResult`, `MetricsMeta`, `WeekDataPoint`, `DoraPerformanceBand`)
- `frontend/src/hooks/useMetrics.ts` — Custom React hook that calls `GET /api/metrics` and returns `{ data, loading, error }`
- `frontend/src/components/RepoForm.tsx` — Landing page form: owner/repo input, masked PAT input, time window radio group, submit button with spinner
- `frontend/src/components/RepoForm.test.tsx` — React Testing Library tests for form validation and API call
- `frontend/src/components/MetricCard.tsx` — Single metric card: title, value+unit, DORA band badge, "not enough data" state
- `frontend/src/components/MetricCard.test.tsx` — Tests for badge colors and not-enough-data rendering
- `frontend/src/components/SkeletonCard.tsx` — Pulsing gray placeholder card shown during loading
- `frontend/src/components/TrendChart.tsx` — Chart.js line/bar chart wrapper using the `timeSeries` array; hides when `dataAvailable: false`
- `frontend/src/components/TrendChart.test.tsx` — Tests for correct chart type per metric and empty-data safety
- `frontend/src/components/Dashboard.tsx` — Main dashboard view: time window selector, 2×2 metric grid, error banners, "Change Repository" link
- `frontend/src/components/Dashboard.test.tsx` — Tests for time window re-fetch, all four error banner states, and view navigation
- `frontend/src/components/ErrorBanner.tsx` — Dismissible error banner component

### Infrastructure
- `frontend/nginx/nginx.conf` — nginx config: serves static `/usr/share/nginx/html`, proxies `/api/*` to `http://backend:8080`, SPA fallback route, security headers
- `frontend/Dockerfile` — Multi-stage Docker build: Node 20 Alpine build stage → nginx Alpine serve stage
- `Dockerfile.backend` — Docker build for the Spring Boot backend jar
- `docker-compose.yml` — Orchestrates `backend` (`:8080`) and `frontend` (`:3000`) containers
- `Tiltfile` — Updated for multi-container local dev: backend via `local_resource` (unchanged), frontend via `local_resource` running Vite dev server

### Notes

- All frontend tests run with `cd frontend && npm test`. To run a single file: `cd frontend && npm test -- RepoForm`.
- Follow TDD: write the failing test first (RED), implement minimum to pass (GREEN), clean up (REFACTOR).
- Use `vi.fn()` on `globalThis.fetch` (or `vi.spyOn`) to mock API responses in tests — no extra dependencies needed.
- Mock `react-chartjs-2` in tests with `vi.mock('react-chartjs-2', ...)` — Canvas is not available in jsdom.
- TypeScript strict mode is on: no `any` types. Define interfaces for all API shapes before writing components.
- PAT must never be written to `localStorage`, `sessionStorage`, or URL query params — keep it in React state only.
- The nginx proxy target (`http://backend:8080`) uses the Docker Compose service name `backend`. In local Vite dev, set `VITE_API_BASE_URL=http://localhost:8080` and configure a Vite proxy instead.
- Backend tests still run with `./mvnw test` from the project root. These two test suites are independent.

---

## Tasks

### [x] 1.0 Scaffold React/Vite/TypeScript Frontend with nginx and Docker Compose

Create the `frontend/` project directory with React 18 + TypeScript + Vite, install all required dependencies (Chart.js, react-chartjs-2, React Testing Library, Vitest), define TypeScript API types from the Spec 02 contract, configure nginx to serve the static build and proxy `/api/*` to the backend, add `docker-compose.yml` for local dev, and expand the Tiltfile for multi-container hot-reload development.

#### 1.0 Proof Artifact(s)

- CLI: `cd frontend && npm run dev` starts the Vite dev server and logs a local URL with no errors
- CLI: `cd frontend && npm test` runs Vitest and exits 0 (even with zero tests — confirms the test harness works)
- CLI: `docker compose up --build` starts both the backend (`:8080`) and nginx frontend (`:3000`) containers with no errors
- Screenshot: browser at `http://localhost:3000` shows the Vite default or a blank React page served through nginx (confirms the build pipeline works end-to-end)

#### 1.0 Tasks

- [x] 1.1 Create the `frontend/` directory at the project root. Inside it, create `package.json` with the following dependencies: `react@^18`, `react-dom@^18`, `react-chartjs-2@^5`, `chart.js@^4`. Dev dependencies: `typescript@^5`, `vite@^5`, `@vitejs/plugin-react@^4`, `vitest@^1`, `@vitest/coverage-v8`, `jsdom@^24`, `@testing-library/react@^14`, `@testing-library/jest-dom@^6`, `@testing-library/user-event@^14`, `@types/react@^18`, `@types/react-dom@^18`. Scripts: `"dev": "vite"`, `"build": "tsc && vite build"`, `"test": "vitest run"`, `"test:watch": "vitest"`. Run `cd frontend && npm install` to install.
- [ ] 1.2 Create `frontend/tsconfig.json` with `"target": "ESNext"`, `"lib": ["ESNext", "DOM"]`, `"jsx": "react-jsx"`, `"strict": true`, `"moduleResolution": "bundler"`, `"include": ["src"]`.
- [ ] 1.3 Create `frontend/vite.config.ts`:
  ```ts
  import { defineConfig } from 'vite'
  import react from '@vitejs/plugin-react'

  export default defineConfig({
    plugins: [react()],
    server: {
      proxy: { '/api': 'http://localhost:8080' }
    },
    test: {
      globals: true,
      environment: 'jsdom',
      setupFiles: ['./src/setupTests.ts'],
    },
  })
  ```
- [ ] 1.4 Create `frontend/src/setupTests.ts` with a single line: `import '@testing-library/jest-dom'`. This loads the custom matchers (e.g., `toBeInTheDocument`) for all tests.
- [ ] 1.5 Create `frontend/index.html` with a minimal HTML5 document: `<div id="root"></div>` and `<script type="module" src="/src/main.tsx"></script>`.
- [ ] 1.6 Create `frontend/src/main.tsx` that renders `<App />` into `document.getElementById('root')` using `ReactDOM.createRoot`.
- [ ] 1.7 Create a minimal `frontend/src/App.tsx` that returns `<h1>DORA Metrics Dashboard</h1>`. Run `cd frontend && npm run dev` — confirm the Vite dev server starts at `http://localhost:5173` with no TypeScript errors.
- [ ] 1.8 Create `frontend/src/types/metrics.ts` with the following TypeScript interfaces matching the Spec 02 JSON contract exactly:
  ```ts
  export type DoraPerformanceBand = 'ELITE' | 'HIGH' | 'MEDIUM' | 'LOW'

  export interface WeekDataPoint {
    weekStart: string   // ISO date string e.g. "2026-02-10"
    value: number
  }

  export interface MetricResult {
    value: number | null
    unit: string | null
    band: DoraPerformanceBand | null
    dataAvailable: boolean
    timeSeries: WeekDataPoint[]
    message: string | null
  }

  export interface MetricsMeta {
    owner: string
    repo: string
    windowDays: number
    generatedAt: string
  }

  export interface MetricsResponse {
    meta: MetricsMeta
    deploymentFrequency: MetricResult
    leadTime: MetricResult
    changeFailureRate: MetricResult
    mttr: MetricResult
  }
  ```
- [ ] 1.9 Run `cd frontend && npm test` — it should exit 0 with "No test files found" or equivalent. Fix any configuration errors (missing dependencies, wrong setupFiles path) before continuing.
- [ ] 1.10 Create `frontend/nginx/nginx.conf`:
  ```nginx
  server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    add_header X-Content-Type-Options nosniff;
    add_header X-Frame-Options DENY;

    location /api/ {
      proxy_pass http://backend:8080;
      proxy_set_header Host $host;
    }

    location / {
      try_files $uri $uri/ /index.html;
    }
  }
  ```
- [ ] 1.11 Create `frontend/Dockerfile` as a two-stage build:
  - **Stage 1** (`builder`): `FROM node:20-alpine` → `WORKDIR /app` → copy `package*.json` → `npm ci` → copy `src/`, `index.html`, `tsconfig.json`, `vite.config.ts` → `npm run build`
  - **Stage 2**: `FROM nginx:alpine` → copy `nginx/nginx.conf` to `/etc/nginx/conf.d/default.conf` → copy `--from=builder /app/dist` to `/usr/share/nginx/html` → `EXPOSE 80`
- [ ] 1.12 Create `Dockerfile.backend` at the project root as a two-stage build:
  - **Stage 1** (`builder`): `FROM eclipse-temurin:17-jdk-alpine` → `WORKDIR /app` → copy `mvnw`, `.mvn/`, `pom.xml` → `./mvnw dependency:go-offline -q` → copy `src/` → `./mvnw package -DskipTests -q`
  - **Stage 2**: `FROM eclipse-temurin:17-jre-alpine` → `WORKDIR /app` → copy `--from=builder /app/target/*.jar app.jar` → `EXPOSE 8080` → `ENTRYPOINT ["java", "-jar", "app.jar"]`
- [ ] 1.13 Create `docker-compose.yml` at the project root:
  ```yaml
  services:
    backend:
      build:
        context: .
        dockerfile: Dockerfile.backend
      ports:
        - "8080:8080"
      environment:
        - SPRING_PROFILES_ACTIVE=dev

    frontend:
      build:
        context: ./frontend
        dockerfile: Dockerfile
      ports:
        - "3000:80"
      depends_on:
        - backend
  ```
- [ ] 1.14 Update `Tiltfile` to add a second `local_resource` for the frontend Vite dev server:
  ```python
  local_resource(
      name = "frontend",
      serve_cmd = "cd frontend && npm run dev -- --port 5173",
      deps = ["frontend/src/", "frontend/package.json", "frontend/vite.config.ts"],
      labels = ["frontend"],
  )
  ```
- [ ] 1.15 Run `docker compose build` to confirm both images build without errors. Then run `docker compose up` and open `http://localhost:3000` — confirm the nginx container serves the React app and the page title shows "DORA Metrics Dashboard".

---

### [x] 2.0 Build the Repo Input Form

Implement the landing page form where users enter GitHub `owner/repo`, a PAT, and a time window (30/90/180 days). On submit, call `GET /api/metrics`, show a spinner while in-flight, and transition to the dashboard view. Update the URL with `owner`, `repo`, and `days` query parameters (token excluded).

#### 2.0 Proof Artifact(s)

- Screenshot: landing form rendered in the browser with all three fields (`owner/repo`, PAT masked, time window selector) and a "Load Metrics" submit button
- Screenshot: form in disabled/spinner state immediately after clicking submit
- Test: `RepoForm.test.tsx` passes — asserts validation error shown for invalid `owner/repo` format (e.g. `"liatrio"` with no `/`) and that the API is not called; asserts correct `fetch` call on valid submission

#### 2.0 Tasks

- [x] 2.1 Write `frontend/src/components/RepoForm.test.tsx` (RED) using React Testing Library. Write three test cases:
  - `renders all three form fields`: render `<RepoForm />` and assert `getByLabelText('Owner/Repo')`, `getByLabelText('Personal Access Token')`, and `getByRole('group', { name: /time window/i })` are present.
  - `shows validation error for invalid owner/repo format`: type `"liatrio"` (no `/`) into the owner/repo field, click submit, assert an error message like `"Format must be owner/repo"` appears and `fetch` was not called.
  - `calls fetch with correct URL on valid submission`: mock `globalThis.fetch` with `vi.fn()` returning a pending promise (to keep it loading), type `"liatrio/liatrio"` and a fake token, click submit, assert `fetch` was called with a URL matching `/api/metrics?owner=liatrio&repo=liatrio&token=.*&days=30`.
  The tests should fail with a module resolution error because `RepoForm.tsx` does not exist yet.
- [x] 2.2 Create `frontend/src/components/RepoForm.tsx` as a minimal stub that returns `<form></form>`. Run the tests — the "renders all three form fields" test should still fail (fields missing), but compilation should succeed.
- [x] 2.3 Implement `RepoForm.tsx` fully:
  - Props interface: `interface RepoFormProps { onSubmit: (owner: string, repo: string, token: string, days: number) => void }`
  - State: `ownerRepo` (string), `token` (string), `days` (30 | 90 | 180, default 30), `error` (string | null), `loading` (boolean passed in as prop or managed by parent)
  - Render: a labeled text input for `owner/repo` (placeholder: `liatrio/liatrio`), a labeled password input for PAT, a radio group for 30/90/180 days, a submit button that is disabled when `loading` is true and shows a spinner SVG or "Loading…" text
  - Validation: on submit, check that the `ownerRepo` value matches `/^[^/]+\/[^/]+$/`; if not, set `error` and return without calling `fetch`
  - On valid submit: call `props.onSubmit(owner, repo, token, days)`
  Run the tests — all three should go GREEN.
- [x] 2.4 Update `frontend/src/App.tsx` to manage two views:
  - State: `view: 'form' | 'dashboard'`, `params: { owner, repo, token, days } | null`, `loading: boolean`
  - When view is `'form'`: render `<RepoForm onSubmit={handleFormSubmit} loading={loading} />`
  - `handleFormSubmit`: set `loading = true`, update URL query params (`?owner=&repo=&days=` — no token), call `GET /api/metrics`, on response set `loading = false` and `view = 'dashboard'` (pass response data down). On error, stay on form and show error.
  - When view is `'dashboard'`: render a placeholder `<div>Dashboard coming soon</div>` (will be replaced in Task 5)
  - On page load, read `owner`, `repo`, `days` from URL query params and pre-fill the form fields.
- [x] 2.5 Run `cd frontend && npm test` — all tests should be GREEN. Take a screenshot of the form in the browser at `http://localhost:5173`.

---

### [x] 3.0 Build Metric Cards with DORA Band Badges

Implement the `MetricCard` component that displays a metric's name, value + unit, and a color-coded DORA band badge. Render four cards in a responsive 2×2 grid. Show skeleton placeholders while loading. Show "Not enough data" state with the API message when `dataAvailable: false`.

#### 3.0 Proof Artifact(s)

- Screenshot: all four metric cards rendered with live data from a public GitHub repo, showing at least two different badge colors
- Screenshot: one card in "Not enough data" state (e.g., MTTR on a repo with few incidents) showing the gray badge and API message
- Screenshot: skeleton loading state visible immediately after form submit (pulsing gray placeholder cards)
- Test: `MetricCard.test.tsx` passes — asserts correct badge color (`#22c55e` for Elite, `#ef4444` for Low) and correct "Not enough data" rendering when `dataAvailable: false`

#### 3.0 Tasks

- [x] 3.1 Write `frontend/src/components/MetricCard.test.tsx` (RED). Define a `buildMetricResult` helper that creates a `MetricResult` fixture with sensible defaults. Write four test cases:
  - `renders metric name, value, and unit`: render `<MetricCard title="Deployment Frequency" result={...} />` with a result that has `value: 2.3`, `unit: "deploys/day"`, `band: "ELITE"`, `dataAvailable: true`; assert all three pieces of text are visible.
  - `renders ELITE badge with correct color`: use the ELITE fixture; assert the badge element has `backgroundColor` of `#22c55e` or a CSS class that maps to it.
  - `renders LOW badge with red color #ef4444`: use a LOW fixture; assert the badge color is `#ef4444`.
  - `renders not-enough-data state when dataAvailable is false`: pass a result with `dataAvailable: false` and `message: "No deployment data found"`; assert "Not enough data" heading is visible and the message text is visible; assert the metric value is NOT rendered.
  Tests should fail with a compilation error.
- [x] 3.2 Create `frontend/src/components/MetricCard.tsx` as a stub returning `<div></div>`. Run tests — compilation should pass but all assertions fail.
- [x] 3.3 Implement `MetricCard.tsx` fully:
  - Props: `{ title: string; result: MetricResult; loading?: boolean }`
  - If `loading` is true: render a `<SkeletonCard />` (created in 3.4)
  - If `result.dataAvailable` is false: render the title, a gray "Not enough data" badge, and `result.message` text
  - Otherwise: render title, `result.value?.toFixed(1) + " " + result.unit`, and a colored band badge with inline `backgroundColor` matching the band:
    - `ELITE`: `#22c55e`
    - `HIGH`: `#3b82f6`
    - `MEDIUM`: `#f59e0b`
    - `LOW`: `#ef4444`
  - Badge shape: `border-radius: 9999px`, `padding: 2px 10px`, `font-size: 0.75rem`, white text
  Run `npm test -- MetricCard` — all four tests should go GREEN.
- [x] 3.4 Create `frontend/src/components/SkeletonCard.tsx` — a `<div>` with a CSS animation (`@keyframes pulse` that oscillates opacity between 0.4 and 1.0) and a fixed height matching a metric card. No tests needed for the skeleton itself.
- [x] 3.5 Create CSS (either a `.module.css` file or inline styles) for the 2×2 responsive grid:
  - Container: `display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem;`
  - Responsive: `@media (max-width: 768px) { grid-template-columns: 1fr; }`
  - Card: `background: white; border-radius: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.12); padding: 1.5rem;`
- [x] 3.6 Update `App.tsx` dashboard view to render four `<MetricCard>` components in the grid using the `MetricsResponse` data from the API, passing the appropriate `MetricResult` for each metric. Pass `loading={true}` to all four cards during the initial fetch (shows skeletons).
- [x] 3.7 Run `cd frontend && npm test` — all tests GREEN. Take a screenshot of the four metric cards in the browser.

---

### [ ] 4.0 Render Trend Charts with Chart.js

Add a Chart.js chart below each metric card using the `timeSeries` array from the API. Deployment Frequency and CFR render as line charts; Lead Time and MTTR render as bar charts. Chart color matches the metric's band color. Charts are hidden when `dataAvailable: false`. X-axis shows `MMM D` formatted dates. Charts are responsive.

#### 4.0 Proof Artifact(s)

- Screenshot: all four charts rendered with real trend data at full desktop width (≥1280px)
- Screenshot: same dashboard at ~375px mobile width — cards stack to single column, charts resize correctly
- Test: `TrendChart.test.tsx` passes — asserts line chart rendered for Deployment Frequency, bar chart rendered for Lead Time, and no crash when `timeSeries` is an empty array

#### 4.0 Tasks

- [ ] 4.1 Write `frontend/src/components/TrendChart.test.tsx` (RED). First, add a module mock at the top of the file:
  ```ts
  vi.mock('react-chartjs-2', () => ({
    Line: ({ 'data-testid': tid }: { 'data-testid'?: string }) => <div data-testid={tid ?? 'line-chart'} />,
    Bar:  ({ 'data-testid': tid }: { 'data-testid'?: string }) => <div data-testid={tid ?? 'bar-chart'} />,
  }))
  ```
  Write three test cases:
  - `renders a Line chart for deploymentFrequency`: pass `chartType="line"` prop with non-empty timeSeries; assert `getByTestId('line-chart')` is in the document.
  - `renders a Bar chart for leadTime`: pass `chartType="bar"` prop; assert `getByTestId('bar-chart')` is in the document.
  - `renders nothing when timeSeries is empty`: pass an empty `timeSeries` array and `dataAvailable={false}`; assert neither `line-chart` nor `bar-chart` is present.
  Tests should fail with a compilation error.
- [ ] 4.2 Create `frontend/src/components/TrendChart.tsx` as a minimal stub returning `null`. Run the tests — the "renders nothing" test should pass; the others fail.
- [ ] 4.3 Implement `TrendChart.tsx` fully:
  - Props: `{ chartType: 'line' | 'bar'; timeSeries: WeekDataPoint[]; color: string; label: string; dataAvailable: boolean }`
  - If `dataAvailable` is false or `timeSeries` is empty: return `null`
  - Register required Chart.js components at the top of the file:
    ```ts
    import { Chart as ChartJS, CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend } from 'chart.js'
    ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend)
    ```
  - Transform `timeSeries` into Chart.js format:
    - Labels: format each `weekStart` (ISO date string) as `MMM D` using `new Date(ws + 'T00:00:00Z').toLocaleDateString('en-US', { month: 'short', day: 'numeric', timeZone: 'UTC' })`
    - Dataset: `{ label, data: timeSeries.map(p => p.value), backgroundColor: color, borderColor: color }`
  - Options: `responsive: true`, `maintainAspectRatio: true`, no legend, Y-axis `beginAtZero: true`
  - Render `<Line>` or `<Bar>` from `react-chartjs-2` based on `chartType`
  Run `npm test -- TrendChart` — all three tests should go GREEN.
- [ ] 4.4 Update `MetricCard.tsx` to render a `<TrendChart>` below the value/badge section, passing `chartType` (line for Deployment Frequency and CFR, bar for Lead Time and MTTR), `timeSeries` from `result.timeSeries`, `color` from the band color mapping, `label` as the metric name, and `dataAvailable` from the result.
- [ ] 4.5 Run `cd frontend && npm test` — all tests GREEN. Take screenshots at desktop width (≥1280px) and mobile width (~375px using browser DevTools device emulation).

---

### [ ] 5.0 Implement Time Window Selector and Error State Banners

Add a 30/90/180-day segmented control above the metric grid that re-fetches and re-renders on change (with skeleton loading during re-fetch). Implement dismissible error banners for all four API error states: HTTP 429 (rate limit + reset time), HTTP 401/403 (bad token), HTTP 404 (repo not found), and network error. Add a "Change Repository" link that returns the user to the input form.

#### 5.0 Proof Artifact(s)

- Screenshot: the 30/90/180 day segmented control visible above the metric grid with the active window highlighted
- Screenshot: rate-limit error banner rendered (simulated with a mocked `fetch` response) showing the "resetsAt" time
- Test: `Dashboard.test.tsx` passes — asserts switching time window triggers a new `fetch` call with updated `days` parameter; asserts each of the four error conditions renders the correct banner message; asserts the "Change Repository" link renders the input form

#### 5.0 Tasks

- [ ] 5.1 Create `frontend/src/hooks/useMetrics.ts` — a custom hook that accepts `{ owner, repo, token, days }` and returns `{ data: MetricsResponse | null, loading: boolean, error: MetricsError | null }`. Define `interface MetricsError { status: number | null; message: string; resetsAt?: string }`. The hook should: call `fetch('/api/metrics?...')` on mount and whenever `days` changes, set `loading: true` at start of each call, parse error responses as JSON to extract `resetsAt` for 429s, and catch `TypeError` (network errors). No test needed for the hook directly — it will be tested via `Dashboard.test.tsx`.
- [ ] 5.2 Create `frontend/src/components/ErrorBanner.tsx`:
  - Props: `{ message: string; onDismiss: () => void }`
  - Render: a yellow/red banner `<div>` with the message text and an × dismiss button. Style with `background: #fef2f2`, `border: 1px solid #ef4444`, `border-radius: 6px`, `padding: 1rem`.
- [ ] 5.3 Write `frontend/src/components/Dashboard.test.tsx` (RED). Mock `globalThis.fetch` in `beforeEach`. Write five test cases:
  - `renders the 30/90/180 day selector`: render `<Dashboard ... />` with fixture data; assert three buttons/labels "30 days", "90 days", "180 days" are present.
  - `selecting 90 days triggers a new fetch with days=90`: click the "90 days" button; assert `fetch` was called with a URL containing `days=90`.
  - `renders rate-limit banner on HTTP 429`: mock `fetch` to return `{ status: 429, json: () => ({ error: '...', resetsAt: '2026-03-10T12:00:00Z' }) }`; assert banner text contains "rate limit" and "2026-03-10".
  - `renders bad-token banner on HTTP 401`: mock `fetch` to return `{ status: 401 }`; assert banner contains "Invalid or expired GitHub token".
  - `Change Repository link returns to the input form`: render `<Dashboard />` with an `onBack` prop; click "Change Repository"; assert `onBack` was called.
  Tests should fail with a compilation error.
- [ ] 5.4 Create `frontend/src/components/Dashboard.tsx`:
  - Props: `{ owner: string; repo: string; token: string; initialDays: 30 | 90 | 180; onBack: () => void }`
  - Use `useMetrics` hook with internal `days` state (default from `initialDays`)
  - Render:
    1. A header: "DORA Metrics Dashboard — `owner/repo`" and a "Change Repository" link (calls `onBack`)
    2. A time window segmented control: three buttons for 30, 90, 180. Active button has a distinct background (`#3b82f6` with white text); inactive buttons are white with border.
    3. If `loading`: render four `<SkeletonCard />` components in the grid
    4. If `error`: render `<ErrorBanner>` with the appropriate message:
       - `error.status === 429`: `"GitHub rate limit exceeded. Resets at ${error.resetsAt ?? 'unknown'}. Try again later."`
       - `error.status === 401 || error.status === 403`: `"Invalid or expired GitHub token. Please re-enter your PAT."`
       - `error.status === 404`: `"Repository not found. Check the owner/repo and try again."`
       - `error.status === null` (network): `"Could not reach the server. Check your connection."`
    5. If `data`: render the 2×2 `MetricCard` grid
  Run `npm test -- Dashboard` — all five tests should go GREEN.
- [ ] 5.5 Update `App.tsx` to replace the "Dashboard coming soon" placeholder with `<Dashboard owner={...} repo={...} token={...} initialDays={...} onBack={() => setView('form')} />`.
- [ ] 5.6 Run `cd frontend && npm test` — all tests across all five test files GREEN. Run `./mvnw test` from the project root — confirm backend tests are still GREEN (76 pass, 4 skipped).
- [ ] 5.7 Take the final proof screenshots:
  - Full dashboard with time window selector visible and data loaded (save to `docs/specs/03-spec-dashboard-ui/03-proofs/dashboard-desktop.png`)
  - Rate-limit error banner (save to `docs/specs/03-spec-dashboard-ui/03-proofs/error-banner-429.png`)
  - Dashboard at 375px mobile width (save to `docs/specs/03-spec-dashboard-ui/03-proofs/dashboard-mobile.png`)
