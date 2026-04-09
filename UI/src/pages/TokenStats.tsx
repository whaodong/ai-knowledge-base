import { useState } from 'react'
import type { TokenStatsSummary, TokenTrendData, ServiceTokenStats, TopTokenConsumer, TimeRange } from '@/types/token'

// 模拟数据
const mockSummary: TokenStatsSummary = {
  totalTokens: 1256890,
  inputTokens: 856234,
  outputTokens: 400656,
  totalCost: 23.45,
  avgDailyTokens: 179555,
  peakDailyTokens: 256780,
  totalRequests: 3456
}

const mockTrendData: TokenTrendData[] = [
  { date: '2024-01-01', totalTokens: 120000, inputTokens: 82000, outputTokens: 38000, requestCount: 456 },
  { date: '2024-01-02', totalTokens: 135000, inputTokens: 92000, outputTokens: 43000, requestCount: 512 },
  { date: '2024-01-03', totalTokens: 156000, inputTokens: 105000, outputTokens: 51000, requestCount: 589 },
  { date: '2024-01-04', totalTokens: 178000, inputTokens: 120000, outputTokens: 58000, requestCount: 634 },
  { date: '2024-01-05', totalTokens: 145000, inputTokens: 98000, outputTokens: 47000, requestCount: 523 },
  { date: '2024-01-06', totalTokens: 98000, inputTokens: 67000, outputTokens: 31000, requestCount: 378 },
  { date: '2024-01-07', totalTokens: 85000, inputTokens: 58000, outputTokens: 27000, requestCount: 298 }
]

const mockServiceStats: ServiceTokenStats[] = [
  { serviceType: 'RAG', totalTokens: 567890, requestCount: 1234, avgTokensPerRequest: 460, cost: 11.35 },
  { serviceType: 'EMBEDDING', totalTokens: 456789, requestCount: 1567, avgTokensPerRequest: 291, cost: 6.85 },
  { serviceType: 'SEARCH', totalTokens: 189012, requestCount: 678, avgTokensPerRequest: 279, cost: 4.15 },
  { serviceType: 'DOCUMENT', totalTokens: 43199, requestCount: 977, avgTokensPerRequest: 44, cost: 1.10 }
]

const mockTopConsumers: TopTokenConsumer[] = [
  { username: 'admin', totalTokens: 256780, requestCount: 890, avgTokensPerRequest: 288 },
  { username: 'user001', totalTokens: 198234, requestCount: 654, avgTokensPerRequest: 303 },
  { username: 'user002', totalTokens: 167890, requestCount: 523, avgTokensPerRequest: 321 },
  { username: 'user003', totalTokens: 145678, requestCount: 456, avgTokensPerRequest: 319 },
  { username: 'user004', totalTokens: 123456, requestCount: 389, avgTokensPerRequest: 317 }
]

const TokenStats = () => {
  const [timeRange] = useState<TimeRange>('week')
  const [summary] = useState<TokenStatsSummary>(mockSummary)
  const [trendData] = useState<TokenTrendData[]>(mockTrendData)
  const [serviceStats] = useState<ServiceTokenStats[]>(mockServiceStats)
  const [topConsumers] = useState<TopTokenConsumer[]>(mockTopConsumers)

  // Simple bar chart component
  const SimpleBarChart = ({ data, maxValue, color }: { data: number[]; maxValue: number; color: string }) => (
    <div className="flex items-end gap-1 h-48">
      {data.map((value, i) => (
        <div
          key={i}
          className="flex-1 rounded-t transition-all hover:opacity-80"
          style={{ height: `${(value / maxValue) * 100}%`, backgroundColor: color }}
          title={value.toLocaleString()}
        />
      ))}
    </div>
  )

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-800 dark:text-white">Token 统计</h1>
        <select
          value={timeRange}
          className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg
                     bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
        >
          <option value="today">今日</option>
          <option value="week">本周</option>
          <option value="month">本月</option>
          <option value="custom">自定义</option>
        </select>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">总Token数</p>
          <p className="text-2xl font-bold text-blue-500 mt-1">{summary.totalTokens.toLocaleString()}</p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">日均消耗</p>
          <p className="text-2xl font-bold text-green-500 mt-1">{summary.avgDailyTokens.toLocaleString()}</p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">峰值消耗</p>
          <p className="text-2xl font-bold text-yellow-500 mt-1">{summary.peakDailyTokens.toLocaleString()}</p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">总费用</p>
          <p className="text-2xl font-bold text-purple-500 mt-1">${summary.totalCost.toFixed(2)}</p>
        </div>
      </div>

      {/* Trend chart */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
        <h2 className="text-lg font-medium text-gray-800 dark:text-white mb-4">Token 使用量趋势</h2>
        <SimpleBarChart
          data={trendData.map(d => d.totalTokens)}
          maxValue={Math.max(...trendData.map(d => d.totalTokens))}
          color="#3b82f6"
        />
        <div className="flex justify-between mt-2 text-xs text-gray-500 dark:text-gray-400">
          {trendData.map(d => (
            <span key={d.date}>{d.date.split('-')[2]}</span>
          ))}
        </div>
      </div>

      {/* Service stats */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <h2 className="text-lg font-medium text-gray-800 dark:text-white mb-4">服务类型统计</h2>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-gray-500 dark:text-gray-400 border-b border-gray-200 dark:border-gray-700">
                <th className="pb-2">服务类型</th>
                <th className="pb-2">Token</th>
                <th className="pb-2">请求次数</th>
                <th className="pb-2">费用</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {serviceStats.map(stat => (
                <tr key={stat.serviceType}>
                  <td className="py-2 text-gray-800 dark:text-gray-200">{stat.serviceType}</td>
                  <td className="py-2 text-gray-600 dark:text-gray-400">{stat.totalTokens.toLocaleString()}</td>
                  <td className="py-2 text-gray-600 dark:text-gray-400">{stat.requestCount.toLocaleString()}</td>
                  <td className="py-2 text-gray-600 dark:text-gray-400">${stat.cost.toFixed(2)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <h2 className="text-lg font-medium text-gray-800 dark:text-white mb-4">Token 消耗 Top 10</h2>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-gray-500 dark:text-gray-400 border-b border-gray-200 dark:border-gray-700">
                <th className="pb-2 w-8">#</th>
                <th className="pb-2">用户名</th>
                <th className="pb-2">总Token</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {topConsumers.map((consumer, i) => (
                <tr key={consumer.username}>
                  <td className="py-2 text-gray-500">{i + 1}</td>
                  <td className="py-2 text-gray-800 dark:text-gray-200">{consumer.username}</td>
                  <td className="py-2 text-gray-600 dark:text-gray-400">{consumer.totalTokens.toLocaleString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

export default TokenStats
