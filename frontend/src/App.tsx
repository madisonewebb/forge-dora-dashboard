import { useState, useEffect } from 'react'
import RepoForm from './components/RepoForm'
import Dashboard from './components/Dashboard'

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

  function handleFormSubmit(owner: string, repo: string, token: string, days: number) {
    // Update URL (no token)
    const sp = new URLSearchParams({ owner, repo, days: String(days) })
    window.history.pushState({}, '', `?${sp.toString()}`)

    setParams({ owner, repo, token, days: days as 30 | 90 | 180 })
    setView('dashboard')
  }

  if (view === 'dashboard' && params) {
    return (
      <Dashboard
        owner={params.owner}
        repo={params.repo}
        token={params.token}
        initialDays={params.days}
        onBack={() => setView('form')}
      />
    )
  }

  return (
    <div style={{ background: '#f9fafb', minHeight: '100vh' }}>
      <RepoForm onSubmit={handleFormSubmit} loading={false} />
    </div>
  )
}

export default App
