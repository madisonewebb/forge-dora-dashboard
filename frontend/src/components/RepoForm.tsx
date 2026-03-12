import { useState } from 'react'

interface RepoFormProps {
  onSubmit: (owner: string, repo: string, days: number) => void
  onLogout: () => void
  loading?: boolean
}

const OWNER_REPO_PATTERN = /^[^/]+\/[^/]+$/

export default function RepoForm({ onSubmit, onLogout, loading = false }: RepoFormProps) {
  const [ownerRepo, setOwnerRepo] = useState('')
  const [days, setDays] = useState<30 | 90 | 180>(30)
  const [error, setError] = useState<string | null>(null)
  const [focused, setFocused] = useState(false)

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!OWNER_REPO_PATTERN.test(ownerRepo.trim())) {
      setError('Format must be owner/repo')
      return
    }
    setError(null)
    const [owner, repo] = ownerRepo.trim().split('/')
    onSubmit(owner, repo, days)
  }

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '2rem',
      animation: 'fadeUp 0.4s ease both',
    }}>
      {/* Header */}
      <div style={{ marginBottom: '2.5rem', textAlign: 'center' }}>
        <div style={{
          fontFamily: 'var(--font-head)',
          fontSize: '2rem',
          fontWeight: 800,
          letterSpacing: '0.06em',
          textTransform: 'uppercase',
          color: 'var(--text)',
          lineHeight: 1,
        }}>
          DORA METRICS
        </div>
      </div>

      {/* Card */}
      <div style={{
        width: '100%',
        maxWidth: 440,
        background: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 12,
        padding: '2rem',
        boxShadow: '0 0 40px rgba(168,255,53,0.04), 0 20px 60px rgba(0,0,0,0.4)',
      }}>
        {/* Card header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem' }}>
          <span style={{
            fontFamily: 'var(--font-head)',
            fontSize: '0.75rem',
            fontWeight: 700,
            letterSpacing: '0.15em',
            textTransform: 'uppercase',
            color: 'var(--text-muted)',
          }}>
            Select Repository
          </span>
          <button
            type="button"
            onClick={onLogout}
            style={{
              background: 'none',
              border: '1px solid var(--border)',
              borderRadius: 6,
              color: 'var(--text-muted)',
              fontFamily: 'var(--font-ui)',
              fontSize: '0.75rem',
              padding: '0.25rem 0.625rem',
              cursor: 'pointer',
              transition: 'border-color 0.15s, color 0.15s',
            }}
            onMouseEnter={e => {
              const el = e.currentTarget
              el.style.borderColor = 'var(--border2)'
              el.style.color = 'var(--text)'
            }}
            onMouseLeave={e => {
              const el = e.currentTarget
              el.style.borderColor = 'var(--border)'
              el.style.color = 'var(--text-muted)'
            }}
          >
            Logout
          </button>
        </div>

        <form onSubmit={handleSubmit} noValidate>
          {/* Owner/Repo input */}
          <div style={{ marginBottom: '1.25rem' }}>
            <label htmlFor="ownerRepo" style={{
              display: 'block',
              fontFamily: 'var(--font-mono)',
              fontSize: '0.6875rem',
              fontWeight: 500,
              letterSpacing: '0.12em',
              textTransform: 'uppercase',
              color: 'var(--text-muted)',
              marginBottom: '0.5rem',
            }}>
              owner/repo
            </label>
            <input
              id="ownerRepo"
              type="text"
              placeholder="liatrio/liatrio"
              value={ownerRepo}
              onChange={e => setOwnerRepo(e.target.value)}
              onFocus={() => setFocused(true)}
              onBlur={() => setFocused(false)}
              autoComplete="off"
              spellCheck={false}
              style={{
                width: '100%',
                padding: '0.6875rem 0.875rem',
                background: 'var(--surface2)',
                border: `1px solid ${focused ? 'var(--lime-dim)' : error ? 'var(--red)' : 'var(--border)'}`,
                borderRadius: 8,
                fontFamily: 'var(--font-mono)',
                fontSize: '0.9375rem',
                color: 'var(--text)',
                outline: 'none',
                transition: 'border-color 0.15s',
                boxSizing: 'border-box',
              }}
            />
            {error && (
              <p role="alert" style={{
                marginTop: '0.375rem',
                fontSize: '0.75rem',
                color: 'var(--red)',
                fontFamily: 'var(--font-mono)',
              }}>
                ✗ {error}
              </p>
            )}
          </div>

          {/* Time window */}
          <div style={{ marginBottom: '1.75rem' }}>
            <div style={{
              fontFamily: 'var(--font-mono)',
              fontSize: '0.6875rem',
              fontWeight: 500,
              letterSpacing: '0.12em',
              textTransform: 'uppercase',
              color: 'var(--text-muted)',
              marginBottom: '0.625rem',
            }}>
              Time Window
            </div>
            <div style={{ display: 'flex', gap: '0.5rem' }}>
              {([30, 90, 180] as const).map(d => (
                <button
                  key={d}
                  type="button"
                  onClick={() => setDays(d)}
                  style={{
                    flex: 1,
                    padding: '0.5rem',
                    background: days === d ? 'rgba(168,255,53,0.12)' : 'var(--surface2)',
                    border: `1px solid ${days === d ? 'var(--lime-dim)' : 'var(--border)'}`,
                    borderRadius: 6,
                    fontFamily: 'var(--font-head)',
                    fontSize: '0.875rem',
                    fontWeight: days === d ? 700 : 400,
                    letterSpacing: '0.04em',
                    color: days === d ? 'var(--lime)' : 'var(--text-muted)',
                    cursor: 'pointer',
                    transition: 'all 0.15s',
                  }}
                >
                  {d}d
                </button>
              ))}
            </div>
          </div>

          {/* Submit */}
          <button
            type="submit"
            disabled={loading}
            style={{
              width: '100%',
              padding: '0.75rem',
              background: loading ? 'var(--border)' : 'var(--lime)',
              color: loading ? 'var(--text-muted)' : '#070C1B',
              border: 'none',
              borderRadius: 8,
              fontFamily: 'var(--font-head)',
              fontSize: '1rem',
              fontWeight: 700,
              letterSpacing: '0.1em',
              textTransform: 'uppercase',
              cursor: loading ? 'not-allowed' : 'pointer',
              transition: 'opacity 0.15s, transform 0.15s',
            }}
            onMouseEnter={e => { if (!loading) (e.currentTarget.style.opacity = '0.9') }}
            onMouseLeave={e => { (e.currentTarget.style.opacity = '1') }}
          >
            {loading ? 'Loading…' : 'Analyze →'}
          </button>
        </form>
      </div>
    </div>
  )
}
