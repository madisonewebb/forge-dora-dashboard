import { useState } from 'react'
import type { MetricResult, DoraPerformanceBand } from '../types/metrics'
import SkeletonCard from './SkeletonCard'
import TrendChart from './TrendChart'
import DrilldownPanel from './DrilldownPanel'
import Sparkline from './Sparkline'

interface MetricCardProps {
  title: string
  result: MetricResult
  chartType: 'line' | 'bar'
  loading?: boolean
  compareResult?: MetricResult
}

const BAND_COLORS: Record<DoraPerformanceBand, string> = {
  ELITE:  '#A8FF35',
  HIGH:   '#00D4A8',
  MEDIUM: '#FFB547',
  LOW:    '#FF5B5B',
}

const BAND_BG: Record<DoraPerformanceBand, string> = {
  ELITE:  'rgba(168,255,53,0.1)',
  HIGH:   'rgba(0,212,168,0.1)',
  MEDIUM: 'rgba(255,181,71,0.1)',
  LOW:    'rgba(255,91,91,0.1)',
}

export default function MetricCard({ title, result, chartType, loading, compareResult }: MetricCardProps) {
  const [expanded, setExpanded] = useState(false)
  if (loading) return <SkeletonCard />

  const accentColor = result.band ? BAND_COLORS[result.band] : 'var(--border2)'

  const cardStyle: React.CSSProperties = {
    background: 'var(--surface)',
    border: '1px solid var(--border)',
    borderLeft: `3px solid ${accentColor}`,
    borderRadius: 10,
    padding: '1.5rem',
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
    animation: 'fadeUp 0.4s ease both',
    transition: 'border-color 0.2s',
  }

  if (!result.dataAvailable) {
    return (
      <div style={{ ...cardStyle, borderLeftColor: 'var(--border)' }}>
        <h3 style={{
          fontFamily: 'var(--font-head)',
          fontSize: '0.75rem',
          fontWeight: 700,
          letterSpacing: '0.12em',
          textTransform: 'uppercase',
          color: 'var(--text-muted)',
        }}>
          {title}
        </h3>
        <p style={{
          fontFamily: 'var(--font-head)',
          fontSize: '1.125rem',
          fontWeight: 600,
          letterSpacing: '0.04em',
          color: 'var(--text-dim)',
          textTransform: 'uppercase',
        }}>
          No data
        </p>
        {result.message && (
          <p style={{
            fontSize: '0.75rem',
            color: 'var(--text-muted)',
            fontFamily: 'var(--font-mono)',
            lineHeight: 1.5,
          }}>
            {result.message}
          </p>
        )}
      </div>
    )
  }

  return (
    <div style={cardStyle}>
      {/* Title */}
      <h3 style={{
        fontFamily: 'var(--font-head)',
        fontSize: '0.75rem',
        fontWeight: 700,
        letterSpacing: '0.12em',
        textTransform: 'uppercase',
        color: 'var(--text-muted)',
        margin: 0,
      }}>
        {title}
      </h3>

      {/* Value */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '0.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: '0.5rem' }}>
          <span style={{
            fontFamily: 'var(--font-mono)',
            fontSize: '2rem',
            fontWeight: 600,
            color: accentColor,
            lineHeight: 1,
            letterSpacing: '-0.02em',
          }}>
            {result.value?.toFixed(1)}
          </span>
          <span style={{
            fontFamily: 'var(--font-mono)',
            fontSize: '0.75rem',
            color: 'var(--text-muted)',
            letterSpacing: '0.06em',
          }}>
            {result.unit}
          </span>
        </div>
        {result.dataAvailable && (result.timeSeries?.length ?? 0) >= 2 && (
          <Sparkline
            data={result.timeSeries.map(p => p.value)}
            color={accentColor}
            width={64}
            height={20}
          />
        )}
      </div>

      {/* Compare value */}
      {compareResult && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          borderTop: '1px solid var(--border)',
          paddingTop: '0.5rem',
        }}>
          <span style={{
            fontFamily: 'var(--font-mono)',
            fontSize: '0.6875rem',
            color: 'var(--text-muted)',
            letterSpacing: '0.06em',
          }}>vs</span>
          {compareResult.dataAvailable ? (() => {
              const cColor = compareResult.band ? BAND_COLORS[compareResult.band] : 'var(--border2)'
              const cBg    = compareResult.band ? BAND_BG[compareResult.band]     : 'transparent'
              return (
                <>
                  <span style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: '1.25rem',
                    fontWeight: 600,
                    color: cColor,
                    lineHeight: 1,
                    letterSpacing: '-0.02em',
                  }}>
                    {compareResult.value?.toFixed(1)}
                  </span>
                  <span style={{
                    fontFamily: 'var(--font-mono)',
                    fontSize: '0.6875rem',
                    color: 'var(--text-muted)',
                  }}>
                    {compareResult.unit}
                  </span>
                  {compareResult.band && (
                    <span style={{
                      display: 'inline-block',
                      background: cBg,
                      color: cColor,
                      border: `1px solid ${cColor}30`,
                      borderRadius: 4,
                      padding: '1px 6px',
                      fontFamily: 'var(--font-head)',
                      fontSize: '0.625rem',
                      fontWeight: 700,
                      letterSpacing: '0.15em',
                      textTransform: 'uppercase',
                    }}>
                      {compareResult.band}
                    </span>
                  )}
                </>
              )
            })() : (
            <span style={{
              fontFamily: 'var(--font-mono)',
              fontSize: '0.75rem',
              color: 'var(--text-muted)',
            }}>No data</span>
          )}
        </div>
      )}

      {/* Band badge */}
      {result.band && (
        <span style={{
          display: 'inline-block',
          alignSelf: 'flex-start',
          background: BAND_BG[result.band],
          color: accentColor,
          border: `1px solid ${accentColor}30`,
          borderRadius: 4,
          padding: '2px 8px',
          fontFamily: 'var(--font-head)',
          fontSize: '0.6875rem',
          fontWeight: 700,
          letterSpacing: '0.15em',
          textTransform: 'uppercase',
        }}>
          {result.band}
        </span>
      )}

      {/* Note */}
      {result.message && (
        <p style={{
          fontSize: '0.6875rem',
          color: 'var(--text-muted)',
          fontFamily: 'var(--font-mono)',
          lineHeight: 1.4,
          borderTop: '1px solid var(--border)',
          paddingTop: '0.625rem',
          margin: 0,
        }}>
          {result.message}
        </p>
      )}

      {/* Chart */}
      <div style={{ marginTop: '0.25rem' }}>
        <TrendChart
          chartType={chartType}
          timeSeries={result.timeSeries}
          color={accentColor}
          label={title}
          dataAvailable={result.dataAvailable}
        />
      </div>

      {/* Drilldown toggle */}
      {result.dataAvailable && result.timeSeries.length > 0 && (
        <>
          <button
            onClick={() => setExpanded(prev => !prev)}
            style={{
              alignSelf: 'flex-start',
              background: 'none',
              border: 'none',
              padding: 0,
              cursor: 'pointer',
              fontFamily: 'var(--font-mono)',
              fontSize: '0.6875rem',
              color: 'var(--text-muted)',
              letterSpacing: '0.06em',
            }}
          >
            {expanded ? '▲ Hide' : '▼ Details'}
          </button>
          {expanded && (
            <DrilldownPanel
              timeSeries={result.timeSeries}
              unit={result.unit ?? ''}
              metricName={title}
            />
          )}
        </>
      )}
    </div>
  )
}
