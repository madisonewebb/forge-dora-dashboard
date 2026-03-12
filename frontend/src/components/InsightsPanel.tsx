import { useEffect, useRef, useState } from 'react'
import ReactMarkdown from 'react-markdown'
import type { MetricsResponse } from '../types/metrics'
import { computeTrend, type TrendDirection } from '../utils/trendDirection'

interface InsightsPanelProps {
  owner: string
  repo: string
  token: string
  days: number
  metrics?: MetricsResponse | null
}

type StreamStatus = 'analyzing' | 'streaming' | 'complete' | 'error'

interface TrendChipConfig {
  key: keyof Pick<MetricsResponse, 'deploymentFrequency' | 'leadTime' | 'changeFailureRate' | 'mttr'>
  label: string
  higherIsBetter: boolean
}

const CHIP_CONFIGS: TrendChipConfig[] = [
  { key: 'deploymentFrequency', label: 'Dep. Freq', higherIsBetter: true },
  { key: 'leadTime', label: 'Lead Time', higherIsBetter: false },
  { key: 'changeFailureRate', label: 'CFR', higherIsBetter: false },
  { key: 'mttr', label: 'MTTR', higherIsBetter: false },
]

function directionIcon(dir: TrendDirection): string {
  if (dir === 'IMPROVING') return '↑'
  if (dir === 'DECLINING') return '↓'
  return '→'
}

interface ChipStyle {
  background: string
  border: string
  color: string
}

function chipStyle(dir: TrendDirection): ChipStyle {
  if (dir === 'IMPROVING') {
    return {
      background: 'rgba(168,255,53,0.10)',
      border: '1px solid rgba(168,255,53,0.30)',
      color: 'var(--lime)',
    }
  }
  if (dir === 'DECLINING') {
    return {
      background: 'rgba(255,91,91,0.10)',
      border: '1px solid rgba(255,91,91,0.30)',
      color: '#FF5B5B',
    }
  }
  return {
    background: 'transparent',
    border: '1px solid var(--border)',
    color: 'var(--text-muted)',
  }
}

function TrendChips({ metrics }: { metrics: MetricsResponse }) {
  const chips = CHIP_CONFIGS.map(cfg => {
    const result = metrics[cfg.key]
    if (!result.dataAvailable || result.timeSeries.length < 2) return null
    const dir = computeTrend(result.timeSeries, cfg.higherIsBetter)
    const style = chipStyle(dir)
    return (
      <span
        key={cfg.key}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: '0.3rem',
          padding: '0.2rem 0.55rem',
          borderRadius: 20,
          fontSize: '0.6875rem',
          fontFamily: 'var(--font-mono)',
          letterSpacing: '0.04em',
          fontWeight: 600,
          ...style,
        }}
        data-testid={`trend-chip-${cfg.key}`}
      >
        <span aria-hidden="true">{directionIcon(dir)}</span>
        {cfg.label}
        <span style={{ fontWeight: 400, opacity: 0.85 }}>{dir}</span>
      </span>
    )
  }).filter(Boolean)

  if (chips.length === 0) return null

  return (
    <div
      style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: '0.4rem',
        marginBottom: '1rem',
      }}
      data-testid="trend-chips-row"
    >
      {chips}
    </div>
  )
}

export default function InsightsPanel({ owner, repo, token, days, metrics }: InsightsPanelProps) {
  const [content, setContent] = useState('')
  const [status, setStatus] = useState<StreamStatus>('analyzing')
  const abortRef = useRef<AbortController | null>(null)

  async function openStream() {
    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller
    setContent('')
    setStatus('analyzing')

    try {
      const url = `/api/insights?owner=${encodeURIComponent(owner)}&repo=${encodeURIComponent(repo)}&days=${days}`
      const res = await fetch(url, {
        headers: { Authorization: `Bearer ${token}` },
        signal: controller.signal,
      })

      if (!res.ok || !res.body) { setStatus('error'); return }

      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      setStatus('streaming')

      let buffer = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const parts = buffer.split('\n\n')
        buffer = parts.pop() ?? ''
        for (const part of parts) {
          for (const line of part.split('\n')) {
            if (line.startsWith('data: ')) setContent(prev => prev + line.slice(6))
          }
        }
      }
      setStatus('complete')
    } catch (e) {
      if ((e as Error).name !== 'AbortError') {
        setStatus(prev => prev === 'streaming' ? 'complete' : 'error')
      }
    }
  }

  useEffect(() => {
    openStream()
    return () => { abortRef.current?.abort() }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [owner, repo, token, days])

  return (
    <div style={{
      background: 'var(--surface)',
      border: '1px solid var(--border)',
      borderLeft: '3px solid var(--teal)',
      borderRadius: 10,
      padding: '1.5rem',
      marginTop: '1rem',
      animation: 'fadeUp 0.4s ease both',
    }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <span style={{ color: 'var(--teal)', fontSize: '1rem' }}>✦</span>
          <h2 style={{
            fontFamily: 'var(--font-head)',
            fontSize: '0.75rem',
            fontWeight: 700,
            letterSpacing: '0.15em',
            textTransform: 'uppercase',
            color: 'var(--teal)',
            margin: 0,
          }}>
            AI Insights
          </h2>
          {(status === 'analyzing' || status === 'streaming') && (
            <span style={{
              width: 6, height: 6,
              borderRadius: '50%',
              background: 'var(--teal)',
              display: 'inline-block',
              animation: 'blink 1s step-end infinite',
            }} />
          )}
        </div>
        <button
          onClick={openStream}
          style={{
            background: 'none',
            border: '1px solid var(--border)',
            borderRadius: 6,
            color: 'var(--text-muted)',
            fontFamily: 'var(--font-mono)',
            fontSize: '0.6875rem',
            letterSpacing: '0.06em',
            padding: '0.25rem 0.625rem',
            cursor: 'pointer',
            transition: 'border-color 0.15s, color 0.15s',
          }}
          onMouseEnter={e => {
            e.currentTarget.style.borderColor = 'var(--teal)'
            e.currentTarget.style.color = 'var(--teal)'
          }}
          onMouseLeave={e => {
            e.currentTarget.style.borderColor = 'var(--border)'
            e.currentTarget.style.color = 'var(--text-muted)'
          }}
        >
          regenerate
        </button>
      </div>

      {/* Trend chips — shown as soon as metrics are available */}
      {metrics && <TrendChips metrics={metrics} />}

      {status === 'analyzing' && (
        <p style={{
          color: 'var(--text-muted)',
          fontFamily: 'var(--font-mono)',
          fontSize: '0.8125rem',
          fontStyle: 'italic',
        }}>
          Analyzing metrics
          <span style={{ animation: 'blink 1s step-end infinite' }}>_</span>
        </p>
      )}

      {(status === 'streaming' || status === 'complete') && (
        <div style={{
          color: 'var(--text)',
          fontSize: '0.875rem',
          lineHeight: 1.7,
          fontFamily: 'var(--font-ui)',
        }}>
          <style>{`
            .insights-content p { margin: 0 0 0.75rem; }
            .insights-content p:last-child { margin-bottom: 0; }
            .insights-content h1, .insights-content h2, .insights-content h3 {
              font-family: var(--font-head);
              font-weight: 700;
              letter-spacing: 0.06em;
              text-transform: uppercase;
              color: var(--teal);
              margin: 1rem 0 0.5rem;
              font-size: 0.75rem;
            }
            .insights-content strong { color: var(--text); }
            .insights-content ul, .insights-content ol { padding-left: 1.25rem; margin: 0 0 0.75rem; }
            .insights-content li { margin-bottom: 0.25rem; }
            .insights-content code {
              font-family: var(--font-mono);
              font-size: 0.8125rem;
              background: var(--surface2);
              border: 1px solid var(--border);
              border-radius: 3px;
              padding: 0.1em 0.35em;
              color: var(--lime);
            }
          `}</style>
          <div className="insights-content">
            <ReactMarkdown>{content}</ReactMarkdown>
          </div>
          {status === 'streaming' && (
            <span style={{
              display: 'inline-block',
              width: 8, height: 14,
              background: 'var(--teal)',
              marginLeft: 2,
              animation: 'blink 0.7s step-end infinite',
              verticalAlign: 'text-bottom',
            }} />
          )}
          {status === 'complete' && (
            <p style={{
              marginTop: '1rem',
              paddingTop: '0.75rem',
              borderTop: '1px solid var(--border)',
              fontSize: '0.6875rem',
              color: 'var(--text-dim)',
              fontFamily: 'var(--font-mono)',
            }}>
              Generated by Claude
            </p>
          )}
        </div>
      )}

      {status === 'error' && (
        <p style={{
          color: 'var(--text-muted)',
          fontFamily: 'var(--font-mono)',
          fontSize: '0.8125rem',
        }}>
          AI insights unavailable.
        </p>
      )}
    </div>
  )
}
