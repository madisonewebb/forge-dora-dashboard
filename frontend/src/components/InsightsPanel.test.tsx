import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import InsightsPanel from './InsightsPanel'
import type { MetricsResponse, MetricResult } from '../types/metrics'

function makeStreamResponse(sseText: string) {
  const encoded = new TextEncoder().encode(sseText)
  return new Response(
    new ReadableStream({
      start(controller) {
        controller.enqueue(encoded)
        controller.close()
      },
    }),
    { status: 200 }
  )
}

function buildMetricResult(
  timeSeries: { weekStart: string; value: number }[],
  overrides: Partial<MetricResult> = {}
): MetricResult {
  return {
    value: 1,
    unit: 'test',
    band: 'ELITE',
    dataAvailable: true,
    timeSeries,
    message: null,
    ...overrides,
  }
}

function buildMetrics(
  dfSeries: { weekStart: string; value: number }[],
  ltSeries: { weekStart: string; value: number }[],
  cfrSeries: { weekStart: string; value: number }[],
  mttrSeries: { weekStart: string; value: number }[]
): MetricsResponse {
  return {
    meta: { owner: 'liatrio', repo: 'test', windowDays: 30, generatedAt: '2026-01-01T00:00:00Z' },
    deploymentFrequency: buildMetricResult(dfSeries),
    leadTime: buildMetricResult(ltSeries),
    changeFailureRate: buildMetricResult(cfrSeries),
    mttr: buildMetricResult(mttrSeries),
  }
}

// Helper to build a repeating weekly series
function weeks(values: number[]): { weekStart: string; value: number }[] {
  return values.map((v, i) => ({ weekStart: `2026-0${i + 1}-01`, value: v }))
}

describe('InsightsPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders analyzing placeholder on mount', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(() => {})))
    render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} />)
    expect(screen.getByText(/analyzing metrics/i)).toBeInTheDocument()
  })

  it('appends tokens progressively and hides placeholder', async () => {
    vi.stubGlobal('fetch', vi.fn(() =>
      Promise.resolve(makeStreamResponse('data: Hello\n\ndata:  world\n\n'))
    ))
    render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} />)

    await waitFor(() =>
      expect(screen.queryByText(/analyzing metrics/i)).not.toBeInTheDocument()
    )
    expect(screen.getByText(/Hello world/)).toBeInTheDocument()
  })

  it('renders unavailability message on error response', async () => {
    vi.stubGlobal('fetch', vi.fn(() =>
      Promise.resolve(new Response('', { status: 503 }))
    ))
    render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} />)

    await waitFor(() =>
      expect(screen.getByText(/AI insights unavailable/i)).toBeInTheDocument()
    )
  })

  it('Regenerate button resets and reopens connection', async () => {
    const user = userEvent.setup()
    const mockFetch = vi.fn(() => new Promise<Response>(() => {}))
    vi.stubGlobal('fetch', mockFetch)

    render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} />)

    await user.click(screen.getByRole('button', { name: /regenerate/i }))

    expect(mockFetch).toHaveBeenCalledTimes(2)
    expect(screen.getByText(/analyzing metrics/i)).toBeInTheDocument()
  })

  describe('trend chips', () => {
    beforeEach(() => {
      vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(() => {})))
    })

    it('does not render chip row when metrics prop is absent', () => {
      render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} />)
      expect(screen.queryByTestId('trend-chips-row')).not.toBeInTheDocument()
    })

    it('renders all four chips when metrics are provided with sufficient data', () => {
      // DF: IMPROVING (higher is better, second half higher)
      // LT: DECLINING (lower is better, second half higher)
      // CFR: STABLE
      // MTTR: IMPROVING (lower is better, second half lower)
      const metrics = buildMetrics(
        weeks([1, 1, 1, 2, 2, 2]),   // DF improving
        weeks([5, 5, 5, 8, 8, 8]),   // LT declining
        weeks([3, 3, 3, 3, 3, 3]),   // CFR stable
        weeks([10, 10, 10, 7, 7, 7]) // MTTR improving
      )
      render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} metrics={metrics} />)

      expect(screen.getByTestId('trend-chips-row')).toBeInTheDocument()
      expect(screen.getByTestId('trend-chip-deploymentFrequency')).toBeInTheDocument()
      expect(screen.getByTestId('trend-chip-leadTime')).toBeInTheDocument()
      expect(screen.getByTestId('trend-chip-changeFailureRate')).toBeInTheDocument()
      expect(screen.getByTestId('trend-chip-mttr')).toBeInTheDocument()
    })

    it('shows correct direction labels for IMPROVING / DECLINING / STABLE', () => {
      const metrics = buildMetrics(
        weeks([1, 1, 1, 2, 2, 2]),   // DF IMPROVING
        weeks([5, 5, 5, 8, 8, 8]),   // LT DECLINING
        weeks([3, 3, 3, 3, 3, 3]),   // CFR STABLE
        weeks([10, 10, 10, 7, 7, 7]) // MTTR IMPROVING
      )
      render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} metrics={metrics} />)

      const dfChip = screen.getByTestId('trend-chip-deploymentFrequency')
      expect(dfChip).toHaveTextContent('IMPROVING')

      const ltChip = screen.getByTestId('trend-chip-leadTime')
      expect(ltChip).toHaveTextContent('DECLINING')

      const cfrChip = screen.getByTestId('trend-chip-changeFailureRate')
      expect(cfrChip).toHaveTextContent('STABLE')

      const mttrChip = screen.getByTestId('trend-chip-mttr')
      expect(mttrChip).toHaveTextContent('IMPROVING')
    })

    it('shows abbreviated labels for each metric', () => {
      const metrics = buildMetrics(
        weeks([1, 1, 2, 2]),
        weeks([5, 5, 4, 4]),
        weeks([3, 3, 3, 3]),
        weeks([10, 10, 8, 8])
      )
      render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} metrics={metrics} />)

      expect(screen.getByText(/Dep\. Freq/)).toBeInTheDocument()
      expect(screen.getByText(/Lead Time/)).toBeInTheDocument()
      expect(screen.getByText(/CFR/)).toBeInTheDocument()
      expect(screen.getByText(/MTTR/)).toBeInTheDocument()
    })

    it('omits a chip when a metric has dataAvailable=false', () => {
      const metrics: MetricsResponse = {
        meta: { owner: 'liatrio', repo: 'test', windowDays: 30, generatedAt: '2026-01-01T00:00:00Z' },
        deploymentFrequency: buildMetricResult(weeks([1, 1, 2, 2])),
        leadTime: buildMetricResult(weeks([5, 5, 4, 4])),
        changeFailureRate: buildMetricResult([], { dataAvailable: false, value: null, band: null, unit: null }),
        mttr: buildMetricResult(weeks([10, 10, 8, 8])),
      }
      render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} metrics={metrics} />)

      expect(screen.queryByTestId('trend-chip-changeFailureRate')).not.toBeInTheDocument()
      expect(screen.getByTestId('trend-chip-deploymentFrequency')).toBeInTheDocument()
    })

    it('omits a chip when timeSeries has fewer than 2 points', () => {
      const metrics: MetricsResponse = {
        meta: { owner: 'liatrio', repo: 'test', windowDays: 30, generatedAt: '2026-01-01T00:00:00Z' },
        deploymentFrequency: buildMetricResult(weeks([1, 1, 2, 2])),
        leadTime: buildMetricResult(weeks([5])),  // only 1 point
        changeFailureRate: buildMetricResult(weeks([3, 3, 3, 3])),
        mttr: buildMetricResult(weeks([10, 10, 8, 8])),
      }
      render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} metrics={metrics} />)

      expect(screen.queryByTestId('trend-chip-leadTime')).not.toBeInTheDocument()
      expect(screen.getByTestId('trend-chip-deploymentFrequency')).toBeInTheDocument()
    })
  })
})
