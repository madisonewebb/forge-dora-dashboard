export type DoraPerformanceBand = 'ELITE' | 'HIGH' | 'MEDIUM' | 'LOW'

export interface RateLimitInfo {
  remaining: number
  limit: number
  resetAt: string
}

export interface WeekDataPoint {
  weekStart: string // ISO date string e.g. "2026-02-10"
  value: number
}

export interface MetricResult {
  value: number | null
  unit: string | null
  band: DoraPerformanceBand | null
  dataAvailable: boolean
  timeSeries: WeekDataPoint[]
  message: string | null
}

export interface MetricsMeta {
  owner: string
  repo: string
  windowDays: number
  generatedAt: string
}

export interface MetricsResponse {
  meta: MetricsMeta
  deploymentFrequency: MetricResult
  leadTime: MetricResult
  changeFailureRate: MetricResult
  mttr: MetricResult
}
