import { useState, useEffect } from 'react'
import RepoForm from './components/RepoForm'
import Dashboard from './components/Dashboard'
import GitHubLogin from './components/GitHubLogin'

type View = 'form' | 'dashboard'

const TOKEN_KEY = 'gh_token'

interface DashboardParams {
  owner: string
  repo: string
  days: 30 | 90 | 180
}

function App() {
  // NOTE: Storing the OAuth token in localStorage is convenient but exposes it
  // to any JavaScript running on the page (XSS risk). A more secure alternative
  // would be an httpOnly cookie session managed entirely server-side, but that
  // requires a significant backend refactor. Acceptable for an internal tool with
  // a trusted deployment environment; revisit if this is exposed publicly.
  const [token, setToken] = useState<string>(() => localStorage.getItem(TOKEN_KEY) ?? '')
  const [view, setView] = useState<View>('form')
  const [params, setParams] = useState<DashboardParams | null>(null)

  // Pre-fill form from URL query params on load
  useEffect(() => {
    const sp = new URLSearchParams(window.location.search)
    const owner = sp.get('owner')
    const repo = sp.get('repo')
    const days = parseInt(sp.get('days') ?? '30') as 30 | 90 | 180
    if (owner && repo) {
      setParams({ owner, repo, days })
    }
  }, [])

  function handleToken(t: string) {
    localStorage.setItem(TOKEN_KEY, t)
    setToken(t)
  }

  function handleLogout() {
    localStorage.removeItem(TOKEN_KEY)
    setToken('')
    setView('form')
    setParams(null)
  }

  function handleFormSubmit(owner: string, repo: string, days: number) {
    const sp = new URLSearchParams({ owner, repo, days: String(days) })
    window.history.pushState({}, '', `?${sp.toString()}`)
    setParams({ owner, repo, days: days as 30 | 90 | 180 })
    setView('dashboard')
  }

  if (!token) {
    return <GitHubLogin onToken={handleToken} />
  }

  if (view === 'dashboard' && params) {
    return (
      <Dashboard
        owner={params.owner}
        repo={params.repo}
        token={token}
        initialDays={params.days}
        onBack={() => setView('form')}
        onLogout={handleLogout}
      />
    )
  }

  return <RepoForm onSubmit={handleFormSubmit} onLogout={handleLogout} />
}

export default App
