import { useState, useEffect } from 'react'

export interface MetricSnapshot {
  id: number
  repoId: string
  metricName: string
  windowDays: number
  value: number | null
  unit: string | null
  band: 'ELITE' | 'HIGH' | 'MEDIUM' | 'LOW' | null
  dataAvailable: boolean
  snapshotAt: string
  windowStart: string | null
  windowEnd: string | null
}

interface MetricHistoryResponse {
  metricName: string
  repoId: string
  snapshots: MetricSnapshot[]
}

export type MetricHistoryMap = Record<string, MetricSnapshot[]>

export function useMetricHistory(owner: string, repo: string, token: string) {
  const [history, setHistory] = useState<MetricHistoryMap>({})
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!owner || !repo || !token) return
    let cancelled = false

    async function fetchAll() {
      setLoading(true)
      const metrics = ['deploymentFrequency', 'leadTime', 'changeFailureRate', 'mttr']
      try {
        const results = await Promise.all(
          metrics.map(metric =>
            fetch(
              `/api/metrics/history?owner=${encodeURIComponent(owner)}&repo=${encodeURIComponent(repo)}&metric=${metric}&lookbackDays=365`,
              { headers: { Authorization: `Bearer ${token}` } }
            ).then(r => r.ok ? r.json() as Promise<MetricHistoryResponse> : Promise.resolve({ metricName: metric, repoId: '', snapshots: [] }))
          )
        )
        if (!cancelled) {
          const map: MetricHistoryMap = {}
          for (const result of results) {
            map[result.metricName] = result.snapshots
          }
          setHistory(map)
        }
      } catch {
        // silently ignore — TrendsPanel just won't render
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    fetchAll()
    return () => { cancelled = true }
  }, [owner, repo, token])

  return { history, loading }
}
