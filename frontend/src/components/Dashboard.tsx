import { useState, useCallback } from 'react'
import { useMetrics } from '../hooks/useMetrics'
import { useMetricHistory } from '../hooks/useMetricHistory'
import MetricCard from './MetricCard'
import SkeletonCard from './SkeletonCard'
import ErrorBanner from './ErrorBanner'
import InsightsPanel from './InsightsPanel'
import TrendsPanel from './TrendsPanel'
import CompareBar from './CompareBar'

interface DashboardProps {
  owner: string
  repo: string
  token: string
  initialDays: number
  onBack: () => void
  onLogout: () => void
  onDaysChange?: (days: number) => void
}

const WINDOWS = [30, 90, 180] as const

export default function Dashboard({ owner, repo, token, initialDays, onBack, onLogout, onDaysChange }: DashboardProps) {
  const [days, setDays] = useState<number>(initialDays)
  const [dismissed, setDismissed] = useState(false)
  const [copied, setCopied] = useState(false)
  const [downloading, setDownloading] = useState(false)
  const [compareActive, setCompareActive] = useState(false)
  const [compareOwner, setCompareOwner] = useState<string | null>(null)
  const [compareRepo, setCompareRepo] = useState<string | null>(null)

  const handleCopyLink = useCallback(() => {
    navigator.clipboard.writeText(window.location.href).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
  }, [])

  const handleDownloadCsv = useCallback(async () => {
    setDownloading(true)
    try {
      const res = await fetch(
        `/api/export/csv?owner=${encodeURIComponent(owner)}&repo=${encodeURIComponent(repo)}&days=${days}`,
        { headers: { Authorization: `Bearer ${token}` } }
      )
      if (!res.ok) return
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `dora-${owner}-${repo}-${new Date().toISOString().slice(0, 10)}.csv`
      a.click()
      URL.revokeObjectURL(url)
    } finally {
      setDownloading(false)
    }
  }, [owner, repo, token, days])

  const { data, loading, error, rateLimit } = useMetrics({ owner, repo, token, days })
  const { history } = useMetricHistory(owner, repo, token)
  const { data: compareData } = useMetrics({
    owner: compareOwner ?? '',
    repo: compareRepo ?? '',
    token,
    days,
  })

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

          {/* Rate limit indicator */}
          {rateLimit && (
            <div
              title={rateLimit.remaining < 100 ? 'API rate limit almost exhausted' : undefined}
              style={{
                fontFamily: 'var(--font-mono)',
                fontSize: '0.75rem',
                color: rateLimit.remaining < 100
                  ? 'var(--red, #f87171)'
                  : rateLimit.remaining < 500
                    ? '#FFB547'
                    : 'var(--text-muted)',
                flexShrink: 0,
                whiteSpace: 'nowrap',
              }}
            >
              ⚡ {rateLimit.remaining.toLocaleString()} / {rateLimit.limit.toLocaleString()}
            </div>
          )}

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
              aria-label="Download CSV"
              onClick={handleDownloadCsv}
              disabled={downloading || loading}
              style={{
                background: 'none',
                border: '1px solid var(--border)',
                borderRadius: 6,
                color: downloading ? 'var(--lime)' : 'var(--text-muted)',
                fontFamily: 'var(--font-mono)',
                fontSize: '0.6875rem',
                letterSpacing: '0.06em',
                padding: '0.25rem 0.625rem',
                cursor: 'pointer',
                transition: 'border-color 0.15s, color 0.15s',
                opacity: loading ? 0.5 : 1,
              }}
              onMouseEnter={e => { if (!downloading) { e.currentTarget.style.borderColor = 'var(--border2)'; e.currentTarget.style.color = 'var(--text)' } }}
              onMouseLeave={e => { if (!downloading) { e.currentTarget.style.borderColor = 'var(--border)'; e.currentTarget.style.color = 'var(--text-muted)' } }}
            >
              {downloading ? 'Downloading…' : '⬇ CSV'}
            </button>
            <button
              onClick={() => { setCompareActive(prev => !prev); if (compareActive) { setCompareOwner(null); setCompareRepo(null) } }}
              style={{
                background: compareActive ? 'rgba(0,212,168,0.1)' : 'none',
                border: `1px solid ${compareActive ? 'rgba(0,212,168,0.3)' : 'var(--border)'}`,
                borderRadius: 6,
                color: compareActive ? '#00D4A8' : 'var(--text-muted)',
                fontFamily: 'var(--font-mono)',
                fontSize: '0.6875rem',
                letterSpacing: '0.06em',
                padding: '0.25rem 0.625rem',
                cursor: 'pointer',
                transition: 'all 0.15s',
              }}
            >
              ⇄ Compare
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

        {/* Compare bar */}
        {compareActive && (
          <div style={{ marginBottom: '1rem' }}>
            <CompareBar
              onCompare={(o, r) => { setCompareOwner(o); setCompareRepo(r) }}
              onClear={() => { setCompareOwner(null); setCompareRepo(null) }}
              activeRepo={compareOwner && compareRepo ? `${compareOwner}/${compareRepo}` : null}
            />
          </div>
        )}

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
              <MetricCard title="Deployment Frequency" result={data.deploymentFrequency} chartType="line"
                compareResult={compareOwner && compareRepo && compareData ? compareData.deploymentFrequency : undefined} />
              <MetricCard title="Lead Time for Changes" result={data.leadTime} chartType="bar"
                compareResult={compareOwner && compareRepo && compareData ? compareData.leadTime : undefined} />
              <MetricCard title="Change Failure Rate" result={data.changeFailureRate} chartType="line"
                compareResult={compareOwner && compareRepo && compareData ? compareData.changeFailureRate : undefined} />
              <MetricCard title="MTTR" result={data.mttr} chartType="bar"
                compareResult={compareOwner && compareRepo && compareData ? compareData.mttr : undefined} />
            </>
          )}
        </div>

        {/* Historical trends */}
        <TrendsPanel history={history} />

        {/* AI Insights */}
        {data && !loading && (
          <InsightsPanel owner={owner} repo={repo} token={token} days={days} metrics={data} />
        )}
      </main>
    </div>
  )
}
