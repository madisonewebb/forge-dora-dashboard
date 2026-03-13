import { useState } from 'react'

interface CompareBarProps {
  onCompare: (owner: string, repo: string) => void
  onClear: () => void
  activeRepo: string | null
}

export default function CompareBar({ onCompare, onClear, activeRepo }: CompareBarProps) {
  const [input, setInput] = useState('')

  function handleLoad() {
    const parts = input.trim().split('/')
    if (parts.length === 2 && parts[0] && parts[1]) {
      onCompare(parts[0], parts[1])
    }
  }

  return (
    <div style={{
      background: 'rgba(0,212,168,0.05)',
      border: '1px solid rgba(0,212,168,0.2)',
      borderRadius: 8,
      padding: '0.625rem 1rem',
      display: 'flex',
      alignItems: 'center',
      gap: '0.75rem',
      flexWrap: 'wrap',
    }}>
      <span style={{
        fontFamily: 'var(--font-head)',
        fontSize: '0.6875rem',
        fontWeight: 700,
        letterSpacing: '0.12em',
        textTransform: 'uppercase',
        color: '#00D4A8',
        flexShrink: 0,
      }}>
        Compare
      </span>

      {activeRepo ? (
        <>
          <span style={{
            fontFamily: 'var(--font-mono)',
            fontSize: '0.8125rem',
            color: '#00D4A8',
          }}>
            {activeRepo}
          </span>
          <button
            onClick={onClear}
            style={{
              background: 'none',
              border: '1px solid rgba(0,212,168,0.3)',
              borderRadius: 4,
              color: '#00D4A8',
              fontFamily: 'var(--font-mono)',
              fontSize: '0.6875rem',
              padding: '0.2rem 0.5rem',
              cursor: 'pointer',
            }}
          >
            ✕ Clear
          </button>
        </>
      ) : (
        <>
          <input
            value={input}
            onChange={e => setInput(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleLoad()}
            placeholder="owner/repo"
            style={{
              background: 'var(--surface)',
              border: '1px solid var(--border)',
              borderRadius: 6,
              color: 'var(--text)',
              fontFamily: 'var(--font-mono)',
              fontSize: '0.8125rem',
              padding: '0.3rem 0.625rem',
              outline: 'none',
              width: 180,
            }}
          />
          <button
            onClick={handleLoad}
            style={{
              background: 'rgba(0,212,168,0.1)',
              border: '1px solid rgba(0,212,168,0.3)',
              borderRadius: 6,
              color: '#00D4A8',
              fontFamily: 'var(--font-head)',
              fontSize: '0.6875rem',
              fontWeight: 700,
              letterSpacing: '0.08em',
              padding: '0.3rem 0.75rem',
              cursor: 'pointer',
            }}
          >
            Load
          </button>
        </>
      )}
    </div>
  )
}
