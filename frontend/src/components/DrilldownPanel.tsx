import type { WeekDataPoint } from '../types/metrics'

interface DrilldownPanelProps {
  timeSeries: WeekDataPoint[]
  unit: string
  metricName: string
}

function formatWeekRange(weekStart: string): string {
  const start = new Date(weekStart + 'T00:00:00Z')
  const end = new Date(start)
  end.setUTCDate(end.getUTCDate() + 6)

  const startMonth = start.toLocaleDateString('en-US', { month: 'short', timeZone: 'UTC' })
  const startDay = start.toLocaleDateString('en-US', { day: 'numeric', timeZone: 'UTC' })
  const endMonth = end.toLocaleDateString('en-US', { month: 'short', timeZone: 'UTC' })
  const endDay = end.toLocaleDateString('en-US', { day: 'numeric', timeZone: 'UTC' })

  if (startMonth === endMonth) {
    return `${startMonth} ${startDay}–${endDay}`
  }
  return `${startMonth} ${startDay}–${endMonth} ${endDay}`
}

export default function DrilldownPanel({ timeSeries, unit, metricName: _metricName }: DrilldownPanelProps) {
  if (!timeSeries || timeSeries.length === 0) {
    return (
      <div style={{
        fontFamily: 'var(--font-mono)',
        fontSize: '0.6875rem',
        color: 'var(--text-muted)',
        padding: '0.5rem 0',
      }}>
        No weekly data available
      </div>
    )
  }

  // Sort descending (most recent first)
  const sorted = [...timeSeries].sort((a, b) => b.weekStart.localeCompare(a.weekStart))

  return (
    <div style={{
      marginTop: '0.5rem',
      borderTop: '1px solid var(--border)',
      paddingTop: '0.5rem',
    }}>
      <table style={{
        width: '100%',
        borderCollapse: 'collapse',
        fontFamily: 'var(--font-mono)',
        fontSize: '0.6875rem',
      }}>
        <thead>
          <tr>
            <th style={{
              textAlign: 'left',
              color: 'var(--text-muted)',
              fontWeight: 600,
              letterSpacing: '0.08em',
              paddingBottom: '0.375rem',
              borderBottom: '1px solid var(--border)',
            }}>
              Week
            </th>
            <th style={{
              textAlign: 'right',
              color: 'var(--text-muted)',
              fontWeight: 600,
              letterSpacing: '0.08em',
              paddingBottom: '0.375rem',
              borderBottom: '1px solid var(--border)',
            }}>
              Value
            </th>
            <th style={{
              textAlign: 'right',
              color: 'var(--text-muted)',
              fontWeight: 600,
              letterSpacing: '0.08em',
              paddingBottom: '0.375rem',
              borderBottom: '1px solid var(--border)',
            }}>
              vs Prior
            </th>
          </tr>
        </thead>
        <tbody>
          {sorted.map((point, idx) => {
            const prior = sorted[idx + 1]
            let trendCell: React.ReactNode

            if (!prior) {
              trendCell = (
                <span style={{ color: 'var(--text-muted)' }}>—</span>
              )
            } else {
              const delta = point.value - prior.value
              if (delta > 0) {
                trendCell = (
                  <span style={{ color: '#A8FF35' }}>↑ +{delta.toFixed(1)}</span>
                )
              } else if (delta < 0) {
                trendCell = (
                  <span style={{ color: '#FF5B5B' }}>↓ {delta.toFixed(1)}</span>
                )
              } else {
                trendCell = (
                  <span style={{ color: 'var(--text-muted)' }}>— 0.0</span>
                )
              }
            }

            return (
              <tr
                key={point.weekStart}
                style={{ borderBottom: idx < sorted.length - 1 ? '1px solid var(--border)' : 'none' }}
              >
                <td style={{
                  color: 'var(--text)',
                  padding: '0.375rem 0',
                }}>
                  {formatWeekRange(point.weekStart)}
                </td>
                <td style={{
                  color: 'var(--text)',
                  textAlign: 'right',
                  padding: '0.375rem 0',
                }}>
                  {point.value.toFixed(1)}{unit ? ` ${unit}` : ''}
                </td>
                <td style={{
                  textAlign: 'right',
                  padding: '0.375rem 0',
                }}>
                  {trendCell}
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
