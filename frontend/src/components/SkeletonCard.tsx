function SkeletonCard() {
  return (
    <div
      style={{
        background: '#e5e7eb',
        borderRadius: 8,
        padding: '1.5rem',
        height: 140,
        animation: 'pulse 1.5s ease-in-out infinite',
      }}
    >
      <style>{`
        @keyframes pulse {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.4; }
        }
      `}</style>
    </div>
  )
}

export default SkeletonCard
