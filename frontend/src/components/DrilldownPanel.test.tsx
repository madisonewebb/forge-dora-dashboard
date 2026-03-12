import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import DrilldownPanel from './DrilldownPanel'
import type { WeekDataPoint } from '../types/metrics'

const sampleTimeSeries: WeekDataPoint[] = [
  { weekStart: '2026-02-17', value: 2.5 },
  { weekStart: '2026-02-24', value: 1.9 },
  { weekStart: '2026-03-03', value: 2.3 },
]

describe('DrilldownPanel', () => {
  it('renders the correct number of rows for a given timeSeries', () => {
    render(
      <DrilldownPanel
        timeSeries={sampleTimeSeries}
        unit="deploys/day"
        metricName="Deployment Frequency"
      />
    )
    // 3 data rows + 1 header row = 4 rows total
    const rows = screen.getAllByRole('row')
    expect(rows).toHaveLength(4) // header + 3 data rows
  })

  it('shows ↑ indicator when current week value is greater than prior week', () => {
    render(
      <DrilldownPanel
        timeSeries={sampleTimeSeries}
        unit="deploys/day"
        metricName="Deployment Frequency"
      />
    )
    // Most recent week (Mar 3): 2.3 vs prior (Feb 24): 1.9 → delta +0.4, should show ↑
    expect(screen.getByText(/↑/)).toBeInTheDocument()
  })

  it('shows ↓ indicator when current week value is less than prior week', () => {
    render(
      <DrilldownPanel
        timeSeries={sampleTimeSeries}
        unit="deploys/day"
        metricName="Deployment Frequency"
      />
    )
    // Feb 24 week: 1.9 vs prior (Feb 17): 2.5 → delta -0.6, should show ↓
    expect(screen.getByText(/↓/)).toBeInTheDocument()
  })

  it('shows — for the oldest week (no prior to compare)', () => {
    render(
      <DrilldownPanel
        timeSeries={sampleTimeSeries}
        unit="deploys/day"
        metricName="Deployment Frequency"
      />
    )
    // The oldest week has no prior week, so it renders the muted — dash
    // We check via the em-dash character in the "vs Prior" column
    // The last row (oldest) should have a standalone — (not — 0.0)
    const dashes = screen.getAllByText('—')
    expect(dashes.length).toBeGreaterThanOrEqual(1)
  })

  it('shows empty state when timeSeries is empty', () => {
    render(
      <DrilldownPanel
        timeSeries={[]}
        unit="deploys/day"
        metricName="Deployment Frequency"
      />
    )
    expect(screen.getByText(/no weekly data available/i)).toBeInTheDocument()
  })

  it('shows empty state when timeSeries is a single point', () => {
    render(
      <DrilldownPanel
        timeSeries={[{ weekStart: '2026-03-03', value: 2.3 }]}
        unit="deploys/day"
        metricName="Deployment Frequency"
      />
    )
    // 1 data row + 1 header row
    const rows = screen.getAllByRole('row')
    expect(rows).toHaveLength(2)
    // The single row has no prior, shows —
    expect(screen.getByText('—')).toBeInTheDocument()
  })

  it('displays values with the provided unit', () => {
    render(
      <DrilldownPanel
        timeSeries={[{ weekStart: '2026-03-03', value: 2.3 }]}
        unit="deploys/day"
        metricName="Deployment Frequency"
      />
    )
    expect(screen.getByText(/2\.3 deploys\/day/)).toBeInTheDocument()
  })
})
