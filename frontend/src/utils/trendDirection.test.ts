import { describe, it, expect } from 'vitest'
import { computeTrend } from './trendDirection'

function pts(values: number[]) {
  return values.map(v => ({ value: v }))
}

describe('computeTrend', () => {
  describe('higherIsBetter = true (Deployment Frequency)', () => {
    it('returns IMPROVING when second half mean is >10% higher', () => {
      // first half avg: 2, second half avg: 2.4 (+20%)
      expect(computeTrend(pts([1, 3, 2, 2, 2.4, 2.4]), true)).toBe('IMPROVING')
    })

    it('returns DECLINING when second half mean is >10% lower', () => {
      // first half avg: 4, second half avg: 3.2 (-20%)
      expect(computeTrend(pts([4, 4, 4, 3.2, 3.2, 3.2]), true)).toBe('DECLINING')
    })

    it('returns STABLE when change is within ±10%', () => {
      // first half avg: 4, second half avg: 4.2 (+5%)
      expect(computeTrend(pts([4, 4, 4, 4.2, 4.2, 4.2]), true)).toBe('STABLE')
    })
  })

  describe('higherIsBetter = false (Lead Time, CFR, MTTR)', () => {
    it('returns IMPROVING when second half mean is >10% lower', () => {
      // first half avg: 10, second half avg: 8 (-20%)
      expect(computeTrend(pts([10, 10, 10, 8, 8, 8]), false)).toBe('IMPROVING')
    })

    it('returns DECLINING when second half mean is >10% higher', () => {
      // first half avg: 5, second half avg: 6 (+20%)
      expect(computeTrend(pts([5, 5, 5, 6, 6, 6]), false)).toBe('DECLINING')
    })

    it('returns STABLE when change is within ±10%', () => {
      // first half avg: 10, second half avg: 10.5 (+5%)
      expect(computeTrend(pts([10, 10, 10, 10.5, 10.5, 10.5]), false)).toBe('STABLE')
    })
  })

  describe('edge cases', () => {
    it('returns STABLE for a single data point', () => {
      expect(computeTrend(pts([5]), true)).toBe('STABLE')
    })

    it('returns STABLE for empty array', () => {
      expect(computeTrend([], true)).toBe('STABLE')
    })

    it('handles two-element array', () => {
      // first half [10], second half [20] — higher is better
      expect(computeTrend(pts([10, 20]), true)).toBe('IMPROVING')
    })

    it('returns IMPROVING when first half is zero and second is positive (higher is better)', () => {
      expect(computeTrend(pts([0, 0, 5, 5]), true)).toBe('IMPROVING')
    })

    it('returns DECLINING when first half is zero and second is positive (lower is better)', () => {
      expect(computeTrend(pts([0, 0, 5, 5]), false)).toBe('DECLINING')
    })

    it('returns STABLE when both halves are zero', () => {
      expect(computeTrend(pts([0, 0, 0, 0]), true)).toBe('STABLE')
    })
  })
})
