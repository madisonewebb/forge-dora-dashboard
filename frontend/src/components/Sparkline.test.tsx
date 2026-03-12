import { render } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import Sparkline from './Sparkline'

describe('Sparkline', () => {
  it('renders an SVG with a polyline when data has 2 or more points', () => {
    const { container } = render(
      <Sparkline data={[1, 3, 2, 5, 4]} color="#A8FF35" />
    )
    const svg = container.querySelector('svg')
    expect(svg).not.toBeNull()
    const polyline = container.querySelector('polyline')
    expect(polyline).not.toBeNull()
  })

  it('renders nothing when data has fewer than 2 points — empty array', () => {
    const { container } = render(
      <Sparkline data={[]} color="#A8FF35" />
    )
    expect(container.querySelector('svg')).toBeNull()
  })

  it('renders nothing when data has fewer than 2 points — single value', () => {
    const { container } = render(
      <Sparkline data={[42]} color="#A8FF35" />
    )
    expect(container.querySelector('svg')).toBeNull()
  })

  it('polyline has the correct number of coordinate pairs (one per data point)', () => {
    const data = [1, 4, 2, 6, 3]
    const { container } = render(
      <Sparkline data={data} color="#00D4A8" width={64} height={20} />
    )
    const polyline = container.querySelector('polyline')
    expect(polyline).not.toBeNull()
    // points attribute contains space-separated "x,y" pairs — one per data point
    const pointsAttr = polyline!.getAttribute('points') ?? ''
    const pairs = pointsAttr.trim().split(/\s+/)
    expect(pairs).toHaveLength(data.length)
  })

  it('applies the provided color as the stroke', () => {
    const color = '#FFB547'
    const { container } = render(
      <Sparkline data={[1, 2, 3]} color={color} />
    )
    const polyline = container.querySelector('polyline')
    expect(polyline).not.toBeNull()
    expect(polyline!.getAttribute('stroke')).toBe(color)
  })

  it('respects custom width and height via the SVG viewBox', () => {
    const { container } = render(
      <Sparkline data={[1, 2, 3]} color="#FF5B5B" width={100} height={40} />
    )
    const svg = container.querySelector('svg')
    expect(svg).not.toBeNull()
    expect(svg!.getAttribute('width')).toBe('100')
    expect(svg!.getAttribute('height')).toBe('40')
    expect(svg!.getAttribute('viewBox')).toBe('0 0 100 40')
  })
})
