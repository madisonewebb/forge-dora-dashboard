import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js'
import { Line, Bar } from 'react-chartjs-2'
import type { WeekDataPoint } from '../types/metrics'

ChartJS.register(CategoryScale, LinearScale, PointElement, LineElement, BarElement, Title, Tooltip, Legend)

interface TrendChartProps {
  chartType: 'line' | 'bar'
  timeSeries: WeekDataPoint[]
  color: string
  label: string
  dataAvailable: boolean
}

function TrendChart({ chartType, timeSeries, color, label, dataAvailable }: TrendChartProps) {
  if (!dataAvailable || timeSeries.length === 0) {
    return null
  }

  const labels = timeSeries.map(p =>
    new Date(p.weekStart + 'T00:00:00Z').toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      timeZone: 'UTC',
    })
  )

  const chartData = {
    labels,
    datasets: [
      {
        label,
        data: timeSeries.map(p => p.value),
        backgroundColor: color,
        borderColor: color,
      },
    ],
  }

  const options = {
    responsive: true,
    maintainAspectRatio: true,
    plugins: {
      legend: { display: false },
    },
    scales: {
      y: { beginAtZero: true },
    },
  }

  if (chartType === 'bar') {
    return <Bar data={chartData} options={options} />
  }
  return <Line data={chartData} options={options} />
}

export default TrendChart
