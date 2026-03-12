import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Dashboard from './Dashboard'
import type { MetricsResponse } from '../types/metrics'

Object.defineProperty(navigator, 'clipboard', {
  value: { writeText: vi.fn().mockResolvedValue(undefined) },
  writable: true,
  configurable: true,
})

vi.mock('react-chartjs-2', () => ({
  Line: () => <div data-testid="line-chart" />,
  Bar: () => <div data-testid="bar-chart" />,
}))

vi.mock('./InsightsPanel', () => ({
  default: () => <div data-testid="insights-panel" />,
}))

function makeMetricResult(overrides = {}) {
  return {
    value: 1.5,
    unit: 'deploys/day',
    band: 'HIGH' as const,
    dataAvailable: true,
    timeSeries: [],
    message: null,
    ...overrides,
  }
}

const mockData: MetricsResponse = {
  meta: { owner: 'liatrio', repo: 'liatrio', windowDays: 30, generatedAt: '2026-03-10T00:00:00Z' },
  deploymentFrequency: makeMetricResult(),
  leadTime: makeMetricResult({ unit: 'hours' }),
  changeFailureRate: makeMetricResult({ unit: '%' }),
  mttr: makeMetricResult({ unit: 'hours' }),
}

function makeHeaders(overrides: Record<string, string> = {}) {
  return { get: (name: string) => overrides[name] ?? null }
}

function mockFetchOk(rateLimitHeaders: Record<string, string> = {}) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: true,
    headers: makeHeaders(rateLimitHeaders),
    json: async () => mockData,
  }))
}

describe('Dashboard', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('renders the 30/90/180 day selector', async () => {
    mockFetchOk()
    render(
      <Dashboard owner="liatrio" repo="liatrio" token="tok" initialDays={30} onBack={vi.fn()} onLogout={vi.fn()} />
    )
    expect(screen.getByRole('button', { name: '30d' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '90d' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '180d' })).toBeInTheDocument()
  })

  it('selecting 90 days triggers a new fetch with days=90', async () => {
    mockFetchOk()
    render(
      <Dashboard owner="liatrio" repo="liatrio" token="tok" initialDays={30} onBack={vi.fn()} onLogout={vi.fn()} />
    )
    await userEvent.click(screen.getByRole('button', { name: '90d' }))
    await waitFor(() => {
      const calls = (fetch as ReturnType<typeof vi.fn>).mock.calls
      expect(calls.some((args: unknown[]) => String(args[0]).includes('days=90'))).toBe(true)
    })
  })

  it('renders rate-limit banner on HTTP 429', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 429,
      headers: makeHeaders(),
      json: async () => ({ error: 'Rate limit exceeded', resetsAt: '2026-03-10T12:00:00Z' }),
    }))
    render(
      <Dashboard owner="liatrio" repo="liatrio" token="tok" initialDays={30} onBack={vi.fn()} onLogout={vi.fn()} />
    )
    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument()
    })
    const alert = screen.getByRole('alert')
    expect(alert).toHaveTextContent(/rate limit/i)
    expect(alert).toHaveTextContent('2026-03-10')
  })

  it('renders auth error banner on HTTP 401', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      headers: makeHeaders(),
      json: async () => ({ error: 'Unauthorized' }),
    }))
    render(
      <Dashboard owner="liatrio" repo="liatrio" token="bad" initialDays={30} onBack={vi.fn()} onLogout={vi.fn()} />
    )
    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument()
    })
    expect(screen.getByRole('alert')).toHaveTextContent(/your github session has expired/i)
  })

  it('shows rate limit indicator when headers are present', async () => {
    mockFetchOk({
      'X-GitHub-RateLimit-Remaining': '4231',
      'X-GitHub-RateLimit-Limit': '5000',
      'X-GitHub-RateLimit-Reset': '1741824000',
    })
    render(
      <Dashboard owner="liatrio" repo="liatrio" token="tok" initialDays={30} onBack={vi.fn()} onLogout={vi.fn()} />
    )
    await waitFor(() => {
      expect(screen.getByText(/4,231\s*\/\s*5,000/)).toBeInTheDocument()
    })
  })

  it('does not show rate limit indicator when headers are absent', async () => {
    mockFetchOk()
    render(
      <Dashboard owner="liatrio" repo="liatrio" token="tok" initialDays={30} onBack={vi.fn()} onLogout={vi.fn()} />
    )
    await waitFor(() => {
      expect(screen.queryAllByText(/\/ 5,000/)).toHaveLength(0)
    })
  })

  it('Change Repository link returns to the input form', async () => {
    mockFetchOk()
    const onBack = vi.fn()
    render(
      <Dashboard owner="liatrio" repo="liatrio" token="tok" initialDays={30} onBack={onBack} onLogout={vi.fn()} />
    )
    await userEvent.click(screen.getByRole('button', { name: '← Change' }))
    expect(onBack).toHaveBeenCalledOnce()
  })

  it('renders the Share button in the header', async () => {
    mockFetchOk()
    render(
      <Dashboard owner="liatrio" repo="liatrio" token="tok" initialDays={30} onBack={vi.fn()} onLogout={vi.fn()} />
    )
    expect(screen.getByRole('button', { name: '⎘ Share' })).toBeInTheDocument()
  })

  it('clicking Share copies the URL to clipboard', async () => {
    mockFetchOk()
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText },
      writable: true,
      configurable: true,
    })
    render(
      <Dashboard owner="liatrio" repo="liatrio" token="tok" initialDays={30} onBack={vi.fn()} onLogout={vi.fn()} />
    )
    await userEvent.click(screen.getByRole('button', { name: '⎘ Share' }))
    expect(writeText).toHaveBeenCalledWith(window.location.href)
  })
})
