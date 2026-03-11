import { render, screen, act } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import InsightsPanel from './InsightsPanel'

// Class-based EventSource mock
class MockEventSource {
  static instance: MockEventSource
  onmessage: ((e: MessageEvent) => void) | null = null
  onerror: ((e: Event) => void) | null = null
  close = vi.fn()

  constructor(public url: string) {
    MockEventSource.instance = this
  }
}

vi.stubGlobal('EventSource', MockEventSource)

describe('InsightsPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders analyzing placeholder on mount', () => {
    render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} />)
    expect(screen.getByText(/analyzing your metrics/i)).toBeInTheDocument()
  })

  it('appends tokens progressively and hides placeholder', async () => {
    render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} />)

    act(() => {
      MockEventSource.instance.onmessage?.(
        new MessageEvent('message', { data: 'Hello' })
      )
    })

    act(() => {
      MockEventSource.instance.onmessage?.(
        new MessageEvent('message', { data: ' world' })
      )
    })

    expect(screen.getByText(/Hello world/)).toBeInTheDocument()
    expect(screen.queryByText(/analyzing your metrics/i)).not.toBeInTheDocument()
  })

  it('renders unavailability message on error', async () => {
    render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} />)

    act(() => {
      MockEventSource.instance.onerror?.(new Event('error'))
    })

    expect(screen.getByText(/AI insights are currently unavailable/i)).toBeInTheDocument()
  })

  it('Regenerate button resets and reopens connection', async () => {
    const user = userEvent.setup()
    render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} />)

    const firstInstance = MockEventSource.instance

    // Fire some tokens
    act(() => {
      MockEventSource.instance.onmessage?.(new MessageEvent('message', { data: 'Some content' }))
    })

    // Click Regenerate
    await user.click(screen.getByRole('button', { name: /regenerate/i }))

    expect(firstInstance.close).toHaveBeenCalled()
    expect(MockEventSource.instance).not.toBe(firstInstance)
    expect(screen.getByText(/analyzing your metrics/i)).toBeInTheDocument()
  })
})
