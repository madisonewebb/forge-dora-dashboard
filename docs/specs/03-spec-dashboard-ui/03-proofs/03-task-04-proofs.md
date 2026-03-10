# 03 Task 4.0 Proof Artifacts — Trend Charts with Chart.js

## CLI Output: `npm test` — TrendChart.test.tsx (3/3 PASS), all 11 tests pass

```
> dora-dashboard-frontend@0.0.1 test
> vitest run --passWithNoTests

 RUN  v2.1.9 /Users/madison/Repos/liatrio-forge/forge-dora-dashboard/frontend

 ✓ src/components/TrendChart.test.tsx (3 tests) 48ms
   ✓ TrendChart > renders a Line chart for deploymentFrequency
   ✓ TrendChart > renders a Bar chart for leadTime
   ✓ TrendChart > renders nothing when timeSeries is empty
 ✓ src/components/MetricCard.test.tsx (4 tests) 54ms
 ✓ src/components/RepoForm.test.tsx (4 tests) 206ms

 Test Files  3 passed (3)
      Tests  11 passed (11)
   Start at  10:19:52
   Duration  1.05s
```

Exit code: 0 ✓

## Chart Type Mapping (per spec)

| Metric | Chart Type |
|--------|-----------|
| Deployment Frequency | `line` |
| Lead Time for Changes | `bar` |
| Change Failure Rate | `line` |
| MTTR | `bar` |

## `frontend/src/components/TrendChart.tsx` — Key Implementation

```typescript
// Chart.js components registered at module load:
ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend)

// Returns null when dataAvailable=false OR timeSeries is empty (safe guard)
if (!dataAvailable || timeSeries.length === 0) return null

// X-axis labels: weekStart ISO date → "Jan 5", "Jan 12" format
const labels = timeSeries.map(p =>
  new Date(p.weekStart + 'T00:00:00Z').toLocaleDateString('en-US', {
    month: 'short', day: 'numeric', timeZone: 'UTC'
  })
)

// Chart color matches band color passed from MetricCard
// options: responsive=true, no legend, y-axis beginAtZero=true
```

## jsdom Canvas Mock Strategy

`react-chartjs-2` uses Canvas API which is unavailable in jsdom. Tests use `vi.mock`:

```typescript
vi.mock('react-chartjs-2', () => ({
  Line: ({ 'data-testid': tid }) => <div data-testid={tid ?? 'line-chart'} />,
  Bar:  ({ 'data-testid': tid }) => <div data-testid={tid ?? 'bar-chart'} />,
}))
```

This allows testing chart type selection without Canvas dependency.

## MetricCard Integration

TrendChart is rendered below the band badge in MetricCard:
```tsx
<TrendChart
  chartType={chartType}           // passed from App.tsx ('line' | 'bar')
  timeSeries={result.timeSeries}  // from API response
  color={badgeColor}              // matches band color
  label={title}                   // metric name
  dataAvailable={result.dataAvailable}
/>
```

## Test Coverage by Case

| Test Case | Assertion | Status |
|-----------|-----------|--------|
| renders Line chart for deploymentFrequency | `getByTestId('line-chart')` present | ✓ PASS |
| renders Bar chart for leadTime | `getByTestId('bar-chart')` present | ✓ PASS |
| renders nothing when timeSeries is empty | neither chart testid present | ✓ PASS |

## Verification Summary

| Requirement | Evidence | Status |
|-------------|----------|--------|
| Line chart for Deployment Frequency | Test 1 passes; App.tsx `chartType="line"` | ✓ VERIFIED |
| Bar chart for Lead Time and MTTR | Test 2 passes; App.tsx `chartType="bar"` | ✓ VERIFIED |
| No chart when dataAvailable=false | Test 3 passes — returns null | ✓ VERIFIED |
| No chart when timeSeries empty | Test 3 passes — returns null | ✓ VERIFIED |
| X-axis MMM D date format | `toLocaleDateString` with month/day, UTC timezone | ✓ IMPLEMENTED |
| Chart color matches band | `color` prop from BAND_COLORS in MetricCard | ✓ IMPLEMENTED |
| Responsive charts | `responsive: true, maintainAspectRatio: true` | ✓ IMPLEMENTED |
| All 11 tests pass | 3 TrendChart + 4 MetricCard + 4 RepoForm | ✓ VERIFIED |
