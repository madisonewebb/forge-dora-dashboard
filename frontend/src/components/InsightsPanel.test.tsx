import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import InsightsPanel from './InsightsPanel'

function makeStreamResponse(sseText: string) {
  const encoded = new TextEncoder().encode(sseText)
  return new Response(
    new ReadableStream({
      start(controller) {
        controller.enqueue(encoded)
        controller.close()
      },
    }),
    { status: 200 }
  )
}

describe('InsightsPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders analyzing placeholder on mount', () => {
    vi.stubGlobal('fetch', vi.fn(() => new Promise<Response>(() => {})))
    render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} />)
    expect(screen.getByText(/analyzing metrics/i)).toBeInTheDocument()
  })

  it('appends tokens progressively and hides placeholder', async () => {
    vi.stubGlobal('fetch', vi.fn(() =>
      Promise.resolve(makeStreamResponse('data: Hello\n\ndata:  world\n\n'))
    ))
    render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} />)

    await waitFor(() =>
      expect(screen.queryByText(/analyzing metrics/i)).not.toBeInTheDocument()
    )
    expect(screen.getByText(/Hello world/)).toBeInTheDocument()
  })

  it('renders unavailability message on error response', async () => {
    vi.stubGlobal('fetch', vi.fn(() =>
      Promise.resolve(new Response('', { status: 503 }))
    ))
    render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} />)

    await waitFor(() =>
      expect(screen.getByText(/AI insights unavailable/i)).toBeInTheDocument()
    )
  })

  it('Regenerate button resets and reopens connection', async () => {
    const user = userEvent.setup()
    const mockFetch = vi.fn(() => new Promise<Response>(() => {}))
    vi.stubGlobal('fetch', mockFetch)

    render(<InsightsPanel owner="liatrio" repo="liatrio" token="abc" days={30} />)

    await user.click(screen.getByRole('button', { name: /regenerate/i }))

    expect(mockFetch).toHaveBeenCalledTimes(2)
    expect(screen.getByText(/analyzing metrics/i)).toBeInTheDocument()
  })
})
