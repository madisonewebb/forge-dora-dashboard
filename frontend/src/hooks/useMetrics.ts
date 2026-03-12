import { useState, useEffect } from 'react'
import type { MetricsResponse, RateLimitInfo } from '../types/metrics'

export interface MetricsError {
  status: number | null
  message: string
  resetsAt?: string
}

interface UseMetricsParams {
  owner: string
  repo: string
  token: string
  days: number
}

export function useMetrics({ owner, repo, token, days }: UseMetricsParams) {
  const [data, setData] = useState<MetricsResponse | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<MetricsError | null>(null)
  const [rateLimit, setRateLimit] = useState<RateLimitInfo | null>(null)

  useEffect(() => {
    let cancelled = false

    async function fetchMetrics() {
      setLoading(true)
      setError(null)
      try {
        const res = await fetch(
          `/api/metrics?owner=${encodeURIComponent(owner)}&repo=${encodeURIComponent(repo)}&days=${days}`,
          { headers: { Authorization: `Bearer ${token}` } }
        )
        if (cancelled) return

        const remainingHeader = res.headers.get('X-GitHub-RateLimit-Remaining')
        const limitHeader = res.headers.get('X-GitHub-RateLimit-Limit')
        const resetHeader = res.headers.get('X-GitHub-RateLimit-Reset')
        if (remainingHeader !== null && limitHeader !== null && resetHeader !== null) {
          const resetEpoch = parseInt(resetHeader, 10)
          setRateLimit({
            remaining: parseInt(remainingHeader, 10),
            limit: parseInt(limitHeader, 10),
            resetAt: isNaN(resetEpoch) ? resetHeader : new Date(resetEpoch * 1000).toISOString(),
          })
        }

        if (!res.ok) {
          let resetsAt: string | undefined
          let message = `Request failed: HTTP ${res.status}`
          try {
            const body = await res.json()
            if (body.error) message = body.error
            if (body.resetsAt) resetsAt = body.resetsAt
          } catch {
            // ignore parse errors
          }
          setError({ status: res.status, message, resetsAt })
          setLoading(false)
          return
        }

        const json: MetricsResponse = await res.json()
        if (!cancelled) {
          setData(json)
        }
      } catch {
        if (!cancelled) {
          setError({ status: null, message: 'Could not reach the server. Check your connection.' })
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    fetchMetrics()
    return () => { cancelled = true }
  }, [owner, repo, token, days])

  return { data, loading, error, rateLimit }
}
