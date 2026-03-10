import { useState, useEffect } from 'react'
import RepoForm from './components/RepoForm'
import MetricCard from './components/MetricCard'
import SkeletonCard from './components/SkeletonCard'
import type { MetricsResponse } from './types/metrics'

type View = 'form' | 'dashboard'

interface DashboardParams {
  owner: string
  repo: string
  token: string
  days: 30 | 90 | 180
}

function App() {
  const [view, setView] = useState<View>('form')
  const [params, setParams] = useState<DashboardParams | null>(null)
  const [data, setData] = useState<MetricsResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [fetchError, setFetchError] = useState<string | null>(null)

  // Pre-fill form from URL query params on load
  useEffect(() => {
    const sp = new URLSearchParams(window.location.search)
    const owner = sp.get('owner')
    const repo = sp.get('repo')
    const days = parseInt(sp.get('days') ?? '30') as 30 | 90 | 180
    if (owner && repo) {
      setParams({ owner, repo, token: '', days })
    }
  }, [])

  async function handleFormSubmit(owner: string, repo: string, token: string, days: number) {
    setLoading(true)
    setFetchError(null)

    // Update URL (no token)
    const sp = new URLSearchParams({ owner, repo, days: String(days) })
    window.history.pushState({}, '', `?${sp.toString()}`)

    try {
      const res = await fetch(`/api/metrics?owner=${owner}&repo=${repo}&token=${token}&days=${days}`)
      if (!res.ok) {
        const body = await res.json().catch(() => ({}))
        setFetchError(body.error ?? `Request failed: HTTP ${res.status}`)
        setLoading(false)
        return
      }
      const json: MetricsResponse = await res.json()
      setData(json)
      setParams({ owner, repo, token, days: days as 30 | 90 | 180 })
      setView('dashboard')
    } catch {
      setFetchError('Could not reach the server. Check your connection.')
    } finally {
      setLoading(false)
    }
  }

  if (view === 'dashboard' && params) {
    const gridStyle = {
      display: 'grid',
      gridTemplateColumns: 'repeat(2, 1fr)',
      gap: '1rem',
    }
    return (
      <div style={{ background: '#f9fafb', minHeight: '100vh', padding: '2rem' }}>
        <div style={{ maxWidth: 960, margin: '0 auto' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
            <h1 style={{ margin: 0, fontSize: '1.25rem' }}>
              DORA Metrics — {params.owner}/{params.repo} ({params.days} days)
            </h1>
            <button
              onClick={() => setView('form')}
              style={{ background: 'none', border: '1px solid #d1d5db', borderRadius: 6, padding: '0.375rem 0.75rem', cursor: 'pointer' }}
            >
              Change Repository
            </button>
          </div>
          <div style={gridStyle}>
            {loading || !data ? (
              <>
                <SkeletonCard />
                <SkeletonCard />
                <SkeletonCard />
                <SkeletonCard />
              </>
            ) : (
              <>
                <MetricCard title="Deployment Frequency" result={data.deploymentFrequency} />
                <MetricCard title="Lead Time for Changes" result={data.leadTime} />
                <MetricCard title="Change Failure Rate" result={data.changeFailureRate} />
                <MetricCard title="MTTR" result={data.mttr} />
              </>
            )}
          </div>
        </div>
      </div>
    )
  }

  return (
    <div style={{ background: '#f9fafb', minHeight: '100vh' }}>
      {fetchError && (
        <div role="alert" style={{ background: '#fef2f2', border: '1px solid #ef4444', padding: '1rem', margin: '1rem' }}>
          {fetchError}
        </div>
      )}
      <RepoForm onSubmit={handleFormSubmit} loading={loading} />
    </div>
  )
}

export default App
