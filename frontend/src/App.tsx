import { useState, useEffect } from 'react'
import RepoForm from './components/RepoForm'
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
  const [_data, setData] = useState<MetricsResponse | null>(null)
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
    return (
      <div style={{ padding: '2rem' }}>
        <p>Dashboard — {params.owner}/{params.repo} ({params.days} days)</p>
        <button onClick={() => setView('form')}>Change Repository</button>
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
