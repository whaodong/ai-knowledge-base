import { useState } from 'react'

interface ServiceHealth {
  service: string
  status: 'UP' | 'DOWN'
  port: number
  responseTime: number
}

const Monitoring = () => {
  const [services] = useState<ServiceHealth[]>([
    { service: 'document-service', status: 'UP', port: 8081, responseTime: 45 },
    { service: 'embedding-service', status: 'UP', port: 8082, responseTime: 120 },
    { service: 'rag-service', status: 'UP', port: 8083, responseTime: 85 },
    { service: 'milvus-service', status: 'UP', port: 8086, responseTime: 30 },
  ])

  // Simple chart component
  const SimpleLineChart = ({ data, color }: { data: number[]; color: string }) => {
    const max = Math.max(...data)
    return (
      <div className="flex items-end gap-1 h-48">
        {data.map((value, i) => (
          <div
            key={i}
            className="flex-1 rounded-t transition-all hover:opacity-80"
            style={{ height: `${(value / max) * 100}%`, backgroundColor: color }}
            title={value.toString()}
          />
        ))}
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800 dark:text-white">监控面板</h1>

      {/* Stats cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
              <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
              </svg>
            </div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">文档总数</p>
              <p className="text-xl font-bold text-gray-800 dark:text-white">1234</p>
            </div>
          </div>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-green-100 dark:bg-green-900/30 rounded-lg">
              <svg className="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
            </div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">对话次数</p>
              <p className="text-xl font-bold text-gray-800 dark:text-white">5678</p>
            </div>
          </div>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-yellow-100 dark:bg-yellow-900/30 rounded-lg">
              <svg className="w-5 h-5 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">平均延迟</p>
              <p className="text-xl font-bold text-gray-800 dark:text-white">156ms</p>
            </div>
          </div>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-purple-100 dark:bg-purple-900/30 rounded-lg">
              <svg className="w-5 h-5 text-purple-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">运行时间</p>
              <p className="text-xl font-bold text-gray-800 dark:text-white">72小时</p>
            </div>
          </div>
        </div>
      </div>

      {/* Service status */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
        <h2 className="text-lg font-medium text-gray-800 dark:text-white mb-4">服务状态</h2>
        <table className="w-full">
          <thead>
            <tr className="text-left text-sm text-gray-500 dark:text-gray-400 border-b border-gray-200 dark:border-gray-700">
              <th className="pb-2">服务名称</th>
              <th className="pb-2">状态</th>
              <th className="pb-2">端口</th>
              <th className="pb-2">响应时间(ms)</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
            {services.map((service) => (
              <tr key={service.service}>
                <td className="py-2 text-gray-800 dark:text-gray-200">{service.service}</td>
                <td className="py-2">
                  <span className={`flex items-center gap-2 ${service.status === 'UP' ? 'text-green-500' : 'text-red-500'}`}>
                    <span className={`w-2 h-2 rounded-full ${service.status === 'UP' ? 'bg-green-500' : 'bg-red-500'}`} />
                    {service.status === 'UP' ? '运行中' : '已停止'}
                  </span>
                </td>
                <td className="py-2 text-gray-600 dark:text-gray-400">{service.port}</td>
                <td className="py-2 text-gray-600 dark:text-gray-400">{service.responseTime}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <h2 className="text-lg font-medium text-gray-800 dark:text-white mb-4">QPS 趋势</h2>
          <SimpleLineChart data={[120, 80, 150, 280, 350, 220, 180]} color="#3b82f6" />
          <div className="flex justify-between mt-2 text-xs text-gray-500 dark:text-gray-400">
            <span>00:00</span><span>04:00</span><span>08:00</span><span>12:00</span><span>16:00</span><span>20:00</span><span>24:00</span>
          </div>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <h2 className="text-lg font-medium text-gray-800 dark:text-white mb-4">Token 使用趋势</h2>
          <SimpleLineChart data={[12000, 15000, 18000, 22000, 19000, 8000, 5000]} color="#10b981" />
          <div className="flex justify-between mt-2 text-xs text-gray-500 dark:text-gray-400">
            <span>周一</span><span>周二</span><span>周三</span><span>周四</span><span>周五</span><span>周六</span><span>周日</span>
          </div>
        </div>
      </div>

      {/* Cache hit rate */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
        <h2 className="text-lg font-medium text-gray-800 dark:text-white mb-4">缓存命中率</h2>
        <div className="flex items-center justify-center">
          <div className="relative w-48 h-48">
            <svg className="w-full h-full transform -rotate-90">
              <circle cx="96" cy="96" r="80" strokeWidth="16" stroke="currentColor" fill="none" className="text-red-200 dark:text-red-800" />
              <circle cx="96" cy="96" r="80" strokeWidth="16" stroke="currentColor" fill="none" className="text-green-500" strokeDasharray={`${75 * 5.02} 502`} />
            </svg>
            <div className="absolute inset-0 flex flex-col items-center justify-center">
              <span className="text-3xl font-bold text-green-500">75%</span>
              <span className="text-sm text-gray-500">命中率</span>
            </div>
          </div>
          <div className="ml-8 space-y-2">
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-green-500"></span>
              <span className="text-gray-600 dark:text-gray-400">命中 (75%)</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-red-400"></span>
              <span className="text-gray-600 dark:text-gray-400">未命中 (25%)</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}

export default Monitoring
