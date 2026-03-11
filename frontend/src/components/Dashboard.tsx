import { useState } from 'react'
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
}

const WINDOWS = [30, 90, 180] as const

function Dashboard({ owner, repo, token, initialDays, onBack }: DashboardProps) {
  const [days, setDays] = useState<30 | 90 | 180>(initialDays)
  const [dismissed, setDismissed] = useState(false)
  const { data, loading, error } = useMetrics({ owner, repo, token, days })

  function getErrorMessage(): string {
    if (!error) return ''
    if (error.status === 429) {
      return `GitHub rate limit exceeded. Resets at ${error.resetsAt ?? 'unknown'}. Try again later.`
    }
    if (error.status === 401 || error.status === 403) {
      return 'Invalid or expired GitHub token. Please re-enter your PAT.'
    }
    if (error.status === 404) {
      return 'Repository not found. Check the owner/repo and try again.'
    }
    return 'Could not reach the server. Check your connection.'
  }

  return (
    <div style={{ background: '#f9fafb', minHeight: '100vh', padding: '2rem' }}>
      <div style={{ maxWidth: 960, margin: '0 auto' }}>
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
          <h1 style={{ margin: 0, fontSize: '1.25rem' }}>
            DORA Metrics — {owner}/{repo}
          </h1>
          <button
            onClick={onBack}
            style={{
              background: 'none',
              border: '1px solid #d1d5db',
              borderRadius: 6,
              padding: '0.375rem 0.75rem',
              cursor: 'pointer',
            }}
          >
            Change Repository
          </button>
        </div>

        {/* Time window selector */}
        <div style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
          {WINDOWS.map(w => (
            <button
              key={w}
              onClick={() => { setDays(w); setDismissed(false) }}
              style={{
                padding: '0.375rem 0.875rem',
                border: '1px solid #d1d5db',
                borderRadius: 6,
                cursor: 'pointer',
                fontWeight: days === w ? 600 : 400,
                background: days === w ? '#3b82f6' : 'white',
                color: days === w ? 'white' : '#374151',
              }}
            >
              {w} days
            </button>
          ))}
        </div>

        {/* Error banner */}
        {error && !dismissed && (
          <ErrorBanner message={getErrorMessage()} onDismiss={() => setDismissed(true)} />
        )}

        {/* Metric grid */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(2, 1fr)',
            gap: '1rem',
          }}
        >
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

        {/* AI Insights Panel — only shown when data is available */}
        {data && !loading && (
          <InsightsPanel owner={owner} repo={repo} token={token} days={days} />
        )}
      </div>
    </div>
  )
}

export default Dashboard
