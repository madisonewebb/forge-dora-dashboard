import { useState } from 'react'
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js'
import { Line } from 'react-chartjs-2'
import type { MetricHistoryMap } from '../hooks/useMetricHistory'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, Title, Tooltip, Legend)

const BAND_ORDER = { LOW: 0, MEDIUM: 1, HIGH: 2, ELITE: 3 } as const

const METRIC_CONFIG: Record<string, { label: string; color: string }> = {
  deploymentFrequency: { label: 'Deploy Freq', color: '#A8FF35' },
  leadTime:            { label: 'Lead Time',   color: '#00D4A8' },
  changeFailureRate:   { label: 'CFR',          color: '#FFB547' },
  mttr:                { label: 'MTTR',         color: '#FF5B5B' },
}

interface TrendsPanelProps {
  history: MetricHistoryMap
}

export default function TrendsPanel({ history }: TrendsPanelProps) {
  const [expanded, setExpanded] = useState(false)

  // Check if we have ≥2 snapshots for at least one metric
  const hasEnoughData = Object.values(history).some(snapshots => (snapshots ?? []).length >= 2)
  if (!hasEnoughData) return null

  // Build a union of all dates across metrics for the x-axis
  const allDates = Array.from(
    new Set(
      Object.values(history)
        .flat()
        .map(s => s.snapshotAt.slice(0, 10))
    )
  ).sort()

  const datasets = Object.entries(METRIC_CONFIG).map(([key, config]) => {
    const snapshots = history[key] ?? []
    const snapshotByDate: Record<string, number | null> = {}
    for (const s of snapshots) {
      const date = s.snapshotAt.slice(0, 10)
      snapshotByDate[date] = s.band != null ? BAND_ORDER[s.band] : null
    }
    return {
      label: config.label,
      data: allDates.map(d => snapshotByDate[d] ?? null),
      borderColor: config.color,
      backgroundColor: config.color + '33',
      tension: 0.3,
      spanGaps: true,
    }
  })

  const labels = allDates.map(d =>
    new Date(d + 'T00:00:00Z').toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      timeZone: 'UTC',
    })
  )

  const chartData = { labels, datasets }

  const options = {
    responsive: true,
    maintainAspectRatio: true,
    plugins: {
      legend: {
        display: true,
        labels: { color: '#9aa5b4', font: { size: 11 } },
      },
      tooltip: {
        callbacks: {
          label: (ctx: { dataset: { label?: string }; parsed: { y: number | null } }) => {
            const bandNames = ['LOW', 'MEDIUM', 'HIGH', 'ELITE']
            const band = ctx.parsed.y != null ? (bandNames[ctx.parsed.y] ?? '') : ''
            return `${ctx.dataset.label}: ${band}`
          },
        },
      },
    },
    scales: {
      y: {
        min: 0,
        max: 3,
        ticks: {
          stepSize: 1,
          color: '#9aa5b4',
          callback: (value: string | number) => {
            const bandNames = ['LOW', 'MEDIUM', 'HIGH', 'ELITE']
            return bandNames[Number(value)] ?? ''
          },
        },
        grid: { color: 'rgba(255,255,255,0.05)' },
      },
      x: {
        ticks: { color: '#9aa5b4', maxTicksLimit: 12 },
        grid: { color: 'rgba(255,255,255,0.05)' },
      },
    },
  }

  return (
    <div style={{
      background: 'var(--surface)',
      border: '1px solid var(--border)',
      borderRadius: 10,
      padding: '1.25rem 1.5rem',
      marginTop: '1rem',
      animation: 'fadeUp 0.4s ease both',
    }}>
      <button
        onClick={() => setExpanded(prev => !prev)}
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          background: 'none',
          border: 'none',
          padding: 0,
          cursor: 'pointer',
          fontFamily: 'var(--font-head)',
          fontSize: '0.75rem',
          fontWeight: 700,
          letterSpacing: '0.12em',
          textTransform: 'uppercase',
          color: 'var(--text-muted)',
          width: '100%',
          textAlign: 'left',
        }}
      >
        <span>{expanded ? '▲' : '▼'}</span>
        <span>Historical DORA Trends</span>
      </button>

      {expanded && (
        <div style={{ marginTop: '1rem' }}>
          <Line data={chartData} options={options} />
        </div>
      )}
    </div>
  )
}
