import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import MetricCard from './MetricCard'
import type { MetricResult } from '../types/metrics'

vi.mock('react-chartjs-2', () => ({
  Line: () => <div data-testid="line-chart" />,
  Bar: () => <div data-testid="bar-chart" />,
}))

function buildMetricResult(overrides: Partial<MetricResult> = {}): MetricResult {
  return {
    value: 2.3,
    unit: 'deploys/day',
    band: 'ELITE',
    dataAvailable: true,
    timeSeries: [],
    message: null,
    ...overrides,
  }
}

describe('MetricCard', () => {
  it('renders metric name, value, and unit', () => {
    render(
      <MetricCard
        title="Deployment Frequency"
        result={buildMetricResult({ value: 2.3, unit: 'deploys/day', band: 'ELITE' })}
        chartType="line"
      />
    )
    expect(screen.getByText('Deployment Frequency')).toBeInTheDocument()
    expect(screen.getByText(/2\.3/)).toBeInTheDocument()
    expect(screen.getByText(/deploys\/day/)).toBeInTheDocument()
  })

  it('renders ELITE badge with correct color', () => {
    render(
      <MetricCard
        title="Deployment Frequency"
        result={buildMetricResult({ band: 'ELITE' })}
        chartType="line"
      />
    )
    const badge = screen.getByText('ELITE')
    expect(badge).toBeInTheDocument()
    expect(badge).toHaveStyle({ color: '#A8FF35' })
  })

  it('renders LOW badge with red color #FF5B5B', () => {
    render(
      <MetricCard
        title="Change Failure Rate"
        result={buildMetricResult({ band: 'LOW', value: 42.0, unit: '%' })}
        chartType="line"
      />
    )
    const badge = screen.getByText('LOW')
    expect(badge).toBeInTheDocument()
    expect(badge).toHaveStyle({ color: '#FF5B5B' })
  })

  it('renders not-enough-data state when dataAvailable is false', () => {
    render(
      <MetricCard
        title="MTTR"
        result={buildMetricResult({
          dataAvailable: false,
          value: null,
          unit: null,
          band: null,
          message: 'No deployment data found',
        })}
        chartType="bar"
      />
    )
    expect(screen.getByText('MTTR')).toBeInTheDocument()
    expect(screen.getByText(/no data/i)).toBeInTheDocument()
    expect(screen.getByText('No deployment data found')).toBeInTheDocument()
    expect(screen.queryByText(/deploys\/day/)).not.toBeInTheDocument()
  })
})
