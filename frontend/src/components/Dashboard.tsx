import { useState, useCallback } from 'react'
import { useMetrics } from '../hooks/useMetrics'
import MetricCard from './MetricCard'
import SkeletonCard from './SkeletonCard'
import ErrorBanner from './ErrorBanner'
import InsightsPanel from './InsightsPanel'

interface DashboardProps {
  owner: string
  repo: string
  token: string
  initialDays: 30 | 90 | 180
  onBack: () => void
  onLogout: () => void
  onDaysChange?: (days: 30 | 90 | 180) => void
}

const WINDOWS = [30, 90, 180] as const

export default function Dashboard({ owner, repo, token, initialDays, onBack, onLogout, onDaysChange }: DashboardProps) {
  const [days, setDays] = useState<30 | 90 | 180>(initialDays)
  const [dismissed, setDismissed] = useState(false)
  const [copied, setCopied] = useState(false)

  const handleCopyLink = useCallback(() => {
    navigator.clipboard.writeText(window.location.href).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
  }, [])
  const { data, loading, error } = useMetrics({ owner, repo, token, days })

  function getErrorMessage(): string {
    if (!error) return ''
    if (error.status === 429) return `GitHub rate limit exceeded. Resets at ${error.resetsAt ?? 'unknown'}.`
    if (error.status === 401 || error.status === 403) return 'Your GitHub session has expired. Please log in again.'
    if (error.status === 404) return 'Repository not found. Check the owner/repo and try again.'
    if (error.status === 502) return 'Unexpected response from GitHub. The repository may have too many workflow runs, or Actions may not be enabled.'
    return 'Could not reach the server. Check your connection.'
  }

  return (
    <div style={{ minHeight: '100vh', padding: '0 0 4rem' }}>
      {/* Top bar */}
      <header style={{
        borderBottom: '1px solid var(--border)',
        background: 'rgba(7,12,27,0.9)',
        backdropFilter: 'blur(12px)',
        position: 'sticky',
        top: 0,
        zIndex: 10,
      }}>
        <div style={{
          maxWidth: 1100,
          margin: '0 auto',
          padding: '0 2rem',
          height: 56,
          display: 'flex',
          alignItems: 'center',
          gap: '1.5rem',
        }}>
          {/* Repo */}
          <div style={{
            fontFamily: 'var(--font-mono)',
            fontSize: '0.875rem',
            color: 'var(--text)',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap',
            flex: 1,
          }}>
            <span style={{ color: 'var(--text-muted)' }}>{owner}</span>
            <span style={{ color: 'var(--border2)', margin: '0 0.125rem' }}>/</span>
            <span style={{ color: 'var(--text)' }}>{repo}</span>
          </div>

          {/* Actions */}
          <div style={{ display: 'flex', gap: '0.5rem', flexShrink: 0 }}>
            <button
              onClick={onBack}
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
              onMouseEnter={e => { e.currentTarget.style.borderColor = 'var(--border2)'; e.currentTarget.style.color = 'var(--text)' }}
              onMouseLeave={e => { e.currentTarget.style.borderColor = 'var(--border)'; e.currentTarget.style.color = 'var(--text-muted)' }}
            >
              ← Change
            </button>
            <button
              onClick={handleCopyLink}
              style={{
                background: 'none',
                border: '1px solid var(--border)',
                borderRadius: 6,
                color: copied ? 'var(--lime)' : 'var(--text-muted)',
                fontFamily: 'var(--font-mono)',
                fontSize: '0.6875rem',
                letterSpacing: '0.06em',
                padding: '0.25rem 0.625rem',
                cursor: 'pointer',
                transition: 'border-color 0.15s, color 0.15s',
              }}
              onMouseEnter={e => { if (!copied) { e.currentTarget.style.borderColor = 'var(--border2)'; e.currentTarget.style.color = 'var(--text)' } }}
              onMouseLeave={e => { if (!copied) { e.currentTarget.style.borderColor = 'var(--border)'; e.currentTarget.style.color = 'var(--text-muted)' } }}
            >
              {copied ? '✓ Copied' : '⎘ Share'}
            </button>
            <button
              onClick={onLogout}
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
              onMouseEnter={e => { e.currentTarget.style.borderColor = 'var(--border2)'; e.currentTarget.style.color = 'var(--text)' }}
              onMouseLeave={e => { e.currentTarget.style.borderColor = 'var(--border)'; e.currentTarget.style.color = 'var(--text-muted)' }}
            >
              Logout
            </button>
          </div>
        </div>
      </header>

      <main style={{ maxWidth: 1100, margin: '0 auto', padding: '2rem' }}>
        {/* Page header */}
        <div style={{
          display: 'flex',
          alignItems: 'flex-end',
          justifyContent: 'space-between',
          marginBottom: '1.5rem',
          flexWrap: 'wrap',
          gap: '1rem',
          animation: 'fadeUp 0.3s ease both',
        }}>
          <div>
            <div style={{
              fontFamily: 'var(--font-head)',
              fontSize: '0.6875rem',
              fontWeight: 700,
              letterSpacing: '0.2em',
              textTransform: 'uppercase',
              color: 'var(--text-muted)',
              marginBottom: '0.25rem',
            }}>
              DORA Metrics
            </div>
            <h1 style={{
              fontFamily: 'var(--font-head)',
              fontSize: '1.75rem',
              fontWeight: 800,
              letterSpacing: '0.04em',
              textTransform: 'uppercase',
              color: 'var(--text)',
              lineHeight: 1,
            }}>
              Engineering Performance
            </h1>
          </div>

          {/* Time window selector */}
          <div style={{
            display: 'flex',
            gap: '0.375rem',
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 8,
            padding: '0.25rem',
          }}>
            {WINDOWS.map(w => (
              <button
                key={w}
                onClick={() => { setDays(w); setDismissed(false); onDaysChange?.(w) }}
                style={{
                  padding: '0.375rem 0.875rem',
                  borderRadius: 6,
                  fontFamily: 'var(--font-head)',
                  fontSize: '0.8125rem',
                  fontWeight: 700,
                  letterSpacing: '0.06em',
                  cursor: 'pointer',
                  background: days === w ? 'rgba(168,255,53,0.12)' : 'transparent',
                  color: days === w ? 'var(--lime)' : 'var(--text-muted)',
                  border: days === w ? '1px solid rgba(168,255,53,0.2)' : '1px solid transparent',
                  transition: 'all 0.15s',
                }}
              >
                {w}d
              </button>
            ))}
          </div>
        </div>

        {/* Error banner */}
        {error && !dismissed && (
          <ErrorBanner message={getErrorMessage()} onDismiss={() => setDismissed(true)} />
        )}

        {/* Metric grid */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(2, 1fr)',
          gap: '1rem',
        }}>
          {loading || !data ? (
            <>
              <SkeletonCard />
              <SkeletonCard />
              <SkeletonCard />
              <SkeletonCard />
            </>
          ) : (
            <>
              <MetricCard title="Deployment Frequency" result={data.deploymentFrequency} chartType="line" />
              <MetricCard title="Lead Time for Changes" result={data.leadTime} chartType="bar" />
              <MetricCard title="Change Failure Rate" result={data.changeFailureRate} chartType="line" />
              <MetricCard title="MTTR" result={data.mttr} chartType="bar" />
            </>
          )}
        </div>

        {/* AI Insights */}
        {data && !loading && (
          <InsightsPanel owner={owner} repo={repo} token={token} days={days} metrics={data} />
        )}
      </main>
    </div>
  )
}
