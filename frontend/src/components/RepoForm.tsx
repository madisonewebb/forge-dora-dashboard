import { useState } from 'react'

interface RepoFormProps {
  onSubmit: (owner: string, repo: string, token: string, days: number) => void
  loading?: boolean
}

const OWNER_REPO_PATTERN = /^[^/]+\/[^/]+$/

function RepoForm({ onSubmit, loading = false }: RepoFormProps) {
  const [ownerRepo, setOwnerRepo] = useState('')
  const [token, setToken] = useState('')
  const [days, setDays] = useState<30 | 90 | 180>(30)
  const [error, setError] = useState<string | null>(null)

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!OWNER_REPO_PATTERN.test(ownerRepo.trim())) {
      setError('Format must be owner/repo')
      return
    }
    setError(null)
    const [owner, repo] = ownerRepo.trim().split('/')
    onSubmit(owner, repo, token, days)
  }

  return (
    <div style={{ maxWidth: 480, margin: '4rem auto', padding: '2rem', background: 'white', borderRadius: 8, boxShadow: '0 1px 3px rgba(0,0,0,0.12)' }}>
      <h1 style={{ marginBottom: '1.5rem', fontSize: '1.25rem' }}>DORA Metrics Dashboard</h1>
      <form onSubmit={handleSubmit} noValidate>
        <div style={{ marginBottom: '1rem' }}>
          <label htmlFor="ownerRepo" style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>
            Owner/Repo
          </label>
          <input
            id="ownerRepo"
            type="text"
            placeholder="liatrio/liatrio"
            value={ownerRepo}
            onChange={e => setOwnerRepo(e.target.value)}
            style={{ width: '100%', padding: '0.5rem', border: '1px solid #d1d5db', borderRadius: 6, boxSizing: 'border-box' }}
          />
          {error && (
            <p role="alert" style={{ color: '#ef4444', fontSize: '0.875rem', marginTop: 4 }}>{error}</p>
          )}
        </div>

        <div style={{ marginBottom: '1rem' }}>
          <label htmlFor="token" style={{ display: 'block', marginBottom: 4, fontWeight: 500 }}>
            Personal Access Token
          </label>
          <input
            id="token"
            type="password"
            placeholder="ghp_…"
            value={token}
            onChange={e => setToken(e.target.value)}
            style={{ width: '100%', padding: '0.5rem', border: '1px solid #d1d5db', borderRadius: 6, boxSizing: 'border-box' }}
          />
        </div>

        <fieldset style={{ border: 'none', padding: 0, marginBottom: '1.5rem' }}>
          <legend style={{ fontWeight: 500, marginBottom: 8 }}>Time Window</legend>
          <div style={{ display: 'flex', gap: '1rem' }}>
            {([30, 90, 180] as const).map(d => (
              <label key={d} style={{ display: 'flex', alignItems: 'center', gap: 4, cursor: 'pointer' }}>
                <input
                  type="radio"
                  name="days"
                  value={d}
                  checked={days === d}
                  onChange={() => setDays(d)}
                />
                {d} days
              </label>
            ))}
          </div>
        </fieldset>

        <button
          type="submit"
          disabled={loading}
          style={{
            width: '100%',
            padding: '0.625rem',
            background: loading ? '#9ca3af' : '#3b82f6',
            color: 'white',
            border: 'none',
            borderRadius: 6,
            fontWeight: 600,
            cursor: loading ? 'not-allowed' : 'pointer',
          }}
        >
          {loading ? 'Loading…' : 'Load Metrics'}
        </button>
      </form>
    </div>
  )
}

export default RepoForm
