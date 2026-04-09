import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { monitoringApi } from '@/api/monitoring'
import dayjs from 'dayjs'

interface MetricPoint {
  timestamp: string
  value: number
}

const toSeries = (data: unknown): MetricPoint[] => {
  if (!Array.isArray(data)) return []
  return data
    .map((item) => {
      if (Array.isArray(item) && item.length >= 2) {
        return { timestamp: String(item[0]), value: Number(item[1]) || 0 }
      }
      if (item && typeof item === 'object') {
        const raw = item as Record<string, unknown>
        return {
          timestamp: String(raw.timestamp ?? raw.time ?? ''),
          value: Number(raw.value ?? raw.qps ?? raw.latency ?? 0) || 0
        }
      }
      return null
    })
    .filter((item): item is MetricPoint => Boolean(item && item.timestamp))
}

const Dashboard = () => {
  const now = dayjs()
  const oneHourAgo = now.subtract(1, 'hour')
  const start = oneHourAgo.toISOString()
  const end = now.toISOString()

  const { data: healthData } = useQuery({
    queryKey: ['monitoring', 'health'],
    queryFn: () => monitoringApi.getServiceHealth(),
    refetchInterval: 30000
  })

  const { data: qpsData } = useQuery({
    queryKey: ['monitoring', 'qps', start, end],
    queryFn: () => monitoringApi.getQpsMetrics(start, end),
    refetchInterval: 15000
  })

  const { data: latencyData } = useQuery({
    queryKey: ['monitoring', 'latency', start, end],
    queryFn: () => monitoringApi.getLatencyMetrics(start, end),
    refetchInterval: 15000
  })

  const { data: tokenData } = useQuery({
    queryKey: ['monitoring', 'tokens', start, end],
    queryFn: () => monitoringApi.getTokenMetrics(start, end),
    refetchInterval: 30000
  })

  const qpsSeries = useMemo(
    () => toSeries((qpsData as Record<string, unknown> | undefined)?.points ?? qpsData),
    [qpsData]
  )

  const latencySeries = useMemo(
    () => toSeries((latencyData as Record<string, unknown> | undefined)?.points ?? latencyData),
    [latencyData]
  )

  const latestQps = qpsSeries[qpsSeries.length - 1]?.value ?? 0
  const avgLatency = latencySeries.length
    ? Math.round(latencySeries.reduce((acc, cur) => acc + cur.value, 0) / latencySeries.length)
    : 0
  const cacheHitRate = Number((tokenData as Record<string, unknown> | undefined)?.cache_hit_rate ?? 0)
  const healthStatus = String((healthData as Record<string, unknown> | undefined)?.status ?? '未知')

  // Simple chart component using div bars
  const SimpleLineChart = ({ data, color }: { data: MetricPoint[]; color: string }) => {
    const max = Math.max(...data.map(d => d.value), 1)
    return (
      <div className="h-48 flex items-end gap-1">
        {data.map((point, i) => (
          <div
            key={i}
            className="flex-1 rounded-t transition-all hover:opacity-80"
            style={{
              height: `${(point.value / max) * 100}%`,
              backgroundColor: color
            }}
            title={`${point.timestamp}: ${point.value}`}
          />
        ))}
      </div>
    )
  }

  const SimpleBarChart = ({ data, color }: { data: MetricPoint[]; color: string }) => {
    const max = Math.max(...data.map(d => d.value), 1)
    return (
      <div className="h-48 flex items-end gap-1">
        {data.map((point, i) => (
          <div
            key={i}
            className="flex-1 rounded-t transition-all hover:opacity-80"
            style={{
              height: `${(point.value / max) * 100}%`,
              backgroundColor: color
            }}
            title={`${point.timestamp}: ${point.value}ms`}
          />
        ))}
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Stats cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">服务状态</p>
              <p className={`text-2xl font-bold mt-1 ${healthStatus === 'UP' || healthStatus === '正常' ? 'text-green-500' : 'text-yellow-500'}`}>
                {healthStatus}
              </p>
            </div>
            <div className="p-3 bg-green-100 dark:bg-green-900/30 rounded-full">
              <svg className="w-6 h-6 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
        </div>

        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">QPS</p>
              <p className="text-2xl font-bold mt-1 text-blue-500">{latestQps.toFixed(1)}</p>
            </div>
            <div className="p-3 bg-blue-100 dark:bg-blue-900/30 rounded-full">
              <svg className="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
          </div>
        </div>

        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">平均延迟</p>
              <p className="text-2xl font-bold mt-1 text-orange-500">{avgLatency}<span className="text-sm ml-1">ms</span></p>
            </div>
            <div className="p-3 bg-orange-100 dark:bg-orange-900/30 rounded-full">
              <svg className="w-6 h-6 text-orange-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
        </div>

        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">缓存命中率</p>
              <p className="text-2xl font-bold mt-1 text-purple-500">{cacheHitRate.toFixed(1)}<span className="text-sm ml-1">%</span></p>
            </div>
            <div className="p-3 bg-purple-100 dark:bg-purple-900/30 rounded-full">
              <svg className="w-6 h-6 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
            </div>
          </div>
        </div>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-medium text-gray-800 dark:text-white mb-4">QPS 实时监控</h3>
          {qpsSeries.length > 0 ? (
            <SimpleLineChart data={qpsSeries} color="#3b82f6" />
          ) : (
            <div className="h-48 flex items-center justify-center text-gray-400">
              暂无数据
            </div>
          )}
          <div className="mt-2 text-xs text-gray-500 dark:text-gray-400">
            时间范围: {oneHourAgo.format('HH:mm')} - {now.format('HH:mm')}
          </div>
        </div>

        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-medium text-gray-800 dark:text-white mb-4">延迟分布</h3>
          {latencySeries.length > 0 ? (
            <SimpleBarChart data={latencySeries} color="#f97316" />
          ) : (
            <div className="h-48 flex items-center justify-center text-gray-400">
              暂无数据
            </div>
          )}
          <div className="mt-2 text-xs text-gray-500 dark:text-gray-400">
            平均: {avgLatency}ms
          </div>
        </div>
      </div>
    </div>
  )
}

export default Dashboard
