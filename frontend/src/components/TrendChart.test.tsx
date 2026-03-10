import { render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import TrendChart from './TrendChart'
import type { WeekDataPoint } from '../types/metrics'

vi.mock('react-chartjs-2', () => ({
  Line: ({ 'data-testid': tid }: { 'data-testid'?: string }) => (
    <div data-testid={tid ?? 'line-chart'} />
  ),
  Bar: ({ 'data-testid': tid }: { 'data-testid'?: string }) => (
    <div data-testid={tid ?? 'bar-chart'} />
  ),
}))

const sampleTimeSeries: WeekDataPoint[] = [
  { weekStart: '2026-01-05', value: 2.3 },
  { weekStart: '2026-01-12', value: 1.8 },
]

describe('TrendChart', () => {
  it('renders a Line chart for deploymentFrequency', () => {
    render(
      <TrendChart
        chartType="line"
        timeSeries={sampleTimeSeries}
        color="#22c55e"
        label="Deployment Frequency"
        dataAvailable={true}
      />
    )
    expect(screen.getByTestId('line-chart')).toBeInTheDocument()
  })

  it('renders a Bar chart for leadTime', () => {
    render(
      <TrendChart
        chartType="bar"
        timeSeries={sampleTimeSeries}
        color="#3b82f6"
        label="Lead Time"
        dataAvailable={true}
      />
    )
    expect(screen.getByTestId('bar-chart')).toBeInTheDocument()
  })

  it('renders nothing when timeSeries is empty', () => {
    render(
      <TrendChart
        chartType="line"
        timeSeries={[]}
        color="#22c55e"
        label="Deployment Frequency"
        dataAvailable={false}
      />
    )
    expect(screen.queryByTestId('line-chart')).not.toBeInTheDocument()
    expect(screen.queryByTestId('bar-chart')).not.toBeInTheDocument()
  })
})
