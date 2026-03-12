export default function SkeletonCard() {
  return (
    <div style={{
      background: 'var(--surface)',
      border: '1px solid var(--border)',
      borderRadius: 10,
      padding: '1.5rem',
      height: 200,
      overflow: 'hidden',
      position: 'relative',
    }}>
      <style>{`
        @keyframes skeleton-shimmer {
          0% { transform: translateX(-100%); }
          100% { transform: translateX(200%); }
        }
      `}</style>
      {/* shimmer overlay */}
      <div style={{
        position: 'absolute',
        inset: 0,
        background: 'linear-gradient(90deg, transparent 0%, rgba(168,255,53,0.04) 50%, transparent 100%)',
        animation: 'skeleton-shimmer 1.8s ease-in-out infinite',
      }} />
      {/* fake content lines */}
      <div style={{ width: '45%', height: 10, background: 'var(--surface2)', borderRadius: 4, marginBottom: '1rem' }} />
      <div style={{ width: '60%', height: 32, background: 'var(--surface2)', borderRadius: 4, marginBottom: '0.75rem' }} />
      <div style={{ width: '30%', height: 20, background: 'var(--surface2)', borderRadius: 99 }} />
    </div>
  )
}
