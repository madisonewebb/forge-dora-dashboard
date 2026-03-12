export type TrendDirection = 'IMPROVING' | 'STABLE' | 'DECLINING'

export function computeTrend(
  timeSeries: { value: number }[],
  higherIsBetter: boolean
): TrendDirection {
  if (timeSeries.length < 2) return 'STABLE'

  const mid = Math.floor(timeSeries.length / 2)
  const firstHalf = timeSeries.slice(0, mid)
  const secondHalf = timeSeries.slice(mid)

  const avg = (arr: { value: number }[]) =>
    arr.reduce((sum, p) => sum + p.value, 0) / arr.length

  const firstMean = avg(firstHalf)
  const secondMean = avg(secondHalf)

  if (firstMean === 0) {
    // Avoid division by zero — treat as stable unless clearly non-zero
    if (secondMean === 0) return 'STABLE'
    return higherIsBetter ? 'IMPROVING' : 'DECLINING'
  }

  const ratio = secondMean / firstMean

  if (higherIsBetter) {
    if (ratio > 1.1) return 'IMPROVING'
    if (ratio < 0.9) return 'DECLINING'
    return 'STABLE'
  } else {
    // Lower is better
    if (ratio < 0.9) return 'IMPROVING'
    if (ratio > 1.1) return 'DECLINING'
    return 'STABLE'
  }
}
