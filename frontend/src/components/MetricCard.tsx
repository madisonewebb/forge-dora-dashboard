import type { MetricResult, DoraPerformanceBand } from '../types/metrics'
import SkeletonCard from './SkeletonCard'
import TrendChart from './TrendChart'

interface MetricCardProps {
  title: string
  result: MetricResult
  chartType: 'line' | 'bar'
  loading?: boolean
}

const BAND_COLORS: Record<DoraPerformanceBand, string> = {
  ELITE: '#22c55e',
  HIGH: '#3b82f6',
  MEDIUM: '#f59e0b',
  LOW: '#ef4444',
}

function MetricCard({ title, result, chartType, loading }: MetricCardProps) {
  if (loading) {
    return <SkeletonCard />
  }

  const cardStyle = {
    background: 'white',
    borderRadius: 8,
    boxShadow: '0 1px 3px rgba(0,0,0,0.12)',
    padding: '1.5rem',
  }

  if (!result.dataAvailable) {
    return (
      <div style={cardStyle}>
        <h3 style={{ margin: '0 0 0.75rem', fontSize: '0.875rem', fontWeight: 600, color: '#6b7280' }}>
          {title}
        </h3>
        <p style={{ margin: '0 0 0.5rem', fontWeight: 600, color: '#374151' }}>Not enough data</p>
        {result.message && (
          <p style={{ margin: 0, fontSize: '0.75rem', color: '#6b7280' }}>{result.message}</p>
        )}
      </div>
    )
  }

  const badgeColor = result.band ? BAND_COLORS[result.band] : '#9ca3af'

  return (
    <div style={cardStyle}>
      <h3 style={{ margin: '0 0 0.75rem', fontSize: '0.875rem', fontWeight: 600, color: '#6b7280' }}>
        {title}
      </h3>
      <p style={{ margin: '0 0 0.75rem', fontSize: '1.5rem', fontWeight: 700, color: '#111827' }}>
        {result.value?.toFixed(1)} {result.unit}
      </p>
      {result.band && (
        <span
          style={{
            display: 'inline-block',
            backgroundColor: badgeColor,
            color: 'white',
            borderRadius: 9999,
            padding: '2px 10px',
            fontSize: '0.75rem',
            fontWeight: 600,
          }}
        >
          {result.band}
        </span>
      )}
      <div style={{ marginTop: '1rem' }}>
        <TrendChart
          chartType={chartType}
          timeSeries={result.timeSeries}
          color={badgeColor}
          label={title}
          dataAvailable={result.dataAvailable}
        />
      </div>
    </div>
  )
}

export default MetricCard
