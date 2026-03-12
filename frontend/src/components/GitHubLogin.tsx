import { useState, useEffect, useRef } from 'react'

interface GitHubLoginProps {
  onToken: (token: string) => void
}

type LoginState = 'idle' | 'awaiting' | 'error'

export default function GitHubLogin({ onToken }: GitHubLoginProps) {
  const [state, setState] = useState<LoginState>('idle')
  const [userCode, setUserCode] = useState('')
  const [verificationUri, setVerificationUri] = useState('')
  const [deviceCode, setDeviceCode] = useState('')
  const [pollIntervalSecs, setPollIntervalSecs] = useState(5)
  const [error, setError] = useState<string | null>(null)
  const [copied, setCopied] = useState(false)
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null)

  async function startLogin() {
    setState('awaiting')
    setError(null)
    try {
      const res = await fetch('/api/auth/device/init', { method: 'POST' })
      const data = await res.json()
      if (data.error) {
        setError(data.error_description ?? data.error)
        setState('error')
        return
      }
      setUserCode(data.user_code)
      setVerificationUri(data.verification_uri)
      setDeviceCode(data.device_code)
      setPollIntervalSecs(data.interval ?? 5)
    } catch {
      setError('Could not reach the server. Is the backend running?')
      setState('error')
    }
  }

  useEffect(() => {
    if (state !== 'awaiting' || !deviceCode) return
    timerRef.current = setInterval(async () => {
      try {
        const res = await fetch('/api/auth/device/poll', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ deviceCode }),
        })
        const data = await res.json()
        if (data.access_token) {
          clearInterval(timerRef.current!)
          onToken(data.access_token)
        } else if (data.error === 'slow_down') {
          setPollIntervalSecs(p => p + 5)
        } else if (data.error === 'expired_token') {
          clearInterval(timerRef.current!)
          setError('Code expired. Please try again.')
          setState('error')
        } else if (data.error === 'access_denied') {
          clearInterval(timerRef.current!)
          setError('Access denied.')
          setState('error')
        }
      } catch { /* keep polling */ }
    }, pollIntervalSecs * 1000)
    return () => { if (timerRef.current) clearInterval(timerRef.current) }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [state, deviceCode, pollIntervalSecs])

  function copyCode() {
    navigator.clipboard.writeText(userCode).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
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
      {/* Logo / wordmark */}
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
        maxWidth: 420,
        background: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 12,
        padding: '2rem',
        boxShadow: '0 0 40px rgba(168,255,53,0.04), 0 20px 60px rgba(0,0,0,0.4)',
      }}>
        {state === 'idle' && (
          <>
            <p style={{
              fontSize: '0.875rem',
              color: 'var(--text-muted)',
              marginBottom: '1.5rem',
              lineHeight: 1.6,
            }}>
              Connect your GitHub account to start measuring engineering performance.
            </p>
            <button
              onClick={startLogin}
              style={{
                width: '100%',
                padding: '0.75rem 1rem',
                background: 'var(--lime)',
                color: '#070C1B',
                border: 'none',
                borderRadius: 8,
                fontFamily: 'var(--font-head)',
                fontSize: '1rem',
                fontWeight: 700,
                letterSpacing: '0.08em',
                textTransform: 'uppercase',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '0.625rem',
                transition: 'opacity 0.15s, transform 0.15s',
              }}
              onMouseEnter={e => { (e.target as HTMLElement).style.opacity = '0.9'; (e.target as HTMLElement).style.transform = 'translateY(-1px)' }}
              onMouseLeave={e => { (e.target as HTMLElement).style.opacity = '1'; (e.target as HTMLElement).style.transform = 'translateY(0)' }}
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 0C5.374 0 0 5.373 0 12c0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23A11.509 11.509 0 0112 5.803c1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576C20.566 21.797 24 17.3 24 12c0-6.627-5.373-12-12-12z"/>
              </svg>
              Login with GitHub
            </button>
          </>
        )}

        {state === 'awaiting' && (
          <>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-muted)', marginBottom: '1.25rem', lineHeight: 1.6 }}>
              Open{' '}
              <a href={verificationUri} target="_blank" rel="noreferrer"
                style={{ color: 'var(--lime)', textDecoration: 'none', borderBottom: '1px solid rgba(168,255,53,0.3)' }}>
                {verificationUri}
              </a>{' '}
              and enter this code:
            </p>

            {/* Code display */}
            <button
              onClick={copyCode}
              title="Click to copy"
              style={{
                width: '100%',
                padding: '1rem',
                background: 'var(--surface2)',
                border: '1px solid var(--border2)',
                borderRadius: 8,
                fontFamily: 'var(--font-mono)',
                fontSize: '1.75rem',
                fontWeight: 600,
                letterSpacing: '0.25em',
                color: 'var(--lime)',
                textAlign: 'center',
                cursor: 'pointer',
                marginBottom: '0.75rem',
                animation: 'pulse-lime 3s ease-in-out infinite',
                display: 'block',
                transition: 'border-color 0.15s',
              }}
            >
              {userCode}
            </button>

            {copied && (
              <p style={{ fontSize: '0.75rem', color: 'var(--lime)', textAlign: 'center', marginBottom: '0.75rem' }}>
                Copied to clipboard
              </p>
            )}

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem', color: 'var(--text-muted)', fontSize: '0.8125rem' }}>
              <div style={{
                width: 16, height: 16, borderRadius: '50%',
                border: '2px solid var(--lime)', borderTopColor: 'transparent',
                animation: 'spin 0.8s linear infinite',
                flexShrink: 0,
              }} />
              Waiting for authorization…
            </div>
          </>
        )}

        {state === 'error' && (
          <>
            <p style={{
              fontSize: '0.875rem',
              color: 'var(--red)',
              marginBottom: '1.25rem',
              padding: '0.75rem',
              background: 'rgba(255,91,91,0.08)',
              border: '1px solid rgba(255,91,91,0.2)',
              borderRadius: 6,
            }}>
              {error}
            </p>
            <button
              onClick={startLogin}
              style={{
                width: '100%',
                padding: '0.75rem',
                background: 'var(--lime)',
                color: '#070C1B',
                border: 'none',
                borderRadius: 8,
                fontFamily: 'var(--font-head)',
                fontSize: '1rem',
                fontWeight: 700,
                letterSpacing: '0.08em',
                textTransform: 'uppercase',
                cursor: 'pointer',
              }}
            >
              Try Again
            </button>
          </>
        )}
      </div>

      <p style={{ marginTop: '1.5rem', fontSize: '0.75rem', color: 'var(--text-dim)' }}>
        Powered by GitHub Device Authorization Grant
      </p>
    </div>
  )
}
