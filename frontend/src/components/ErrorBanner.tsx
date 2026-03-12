interface ErrorBannerProps {
  message: string
  onDismiss: () => void
}

export default function ErrorBanner({ message, onDismiss }: ErrorBannerProps) {
  return (
    <div
      role="alert"
      style={{
        background: 'rgba(255,91,91,0.06)',
        border: '1px solid rgba(255,91,91,0.25)',
        borderLeft: '3px solid var(--red)',
        borderRadius: 8,
        padding: '0.875rem 1rem',
        marginBottom: '1rem',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        gap: '0.75rem',
        animation: 'fadeUp 0.2s ease both',
      }}
    >
      <span style={{
        color: '#FF8B8B',
        fontSize: '0.8125rem',
        fontFamily: 'var(--font-mono)',
        lineHeight: 1.5,
      }}>
        ✗ {message}
      </span>
      <button
        onClick={onDismiss}
        aria-label="Dismiss"
        style={{
          background: 'none',
          border: 'none',
          cursor: 'pointer',
          fontSize: '1rem',
          color: 'var(--red)',
          lineHeight: 1,
          padding: 0,
          flexShrink: 0,
          opacity: 0.7,
          transition: 'opacity 0.15s',
        }}
        onMouseEnter={e => { (e.currentTarget.style.opacity = '1') }}
        onMouseLeave={e => { (e.currentTarget.style.opacity = '0.7') }}
      >
        ×
      </button>
    </div>
  )
}
