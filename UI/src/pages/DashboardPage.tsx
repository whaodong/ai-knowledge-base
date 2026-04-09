// 知识库统计仪表盘
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import StatsCard from '@/components/dashboard/StatsCard'
import { LineChart, ProgressRing, DonutChart } from '@/components/dashboard/Charts'
import { ragApi } from '@/api/rag'
import { documentsApi } from '@/api/documents'

// 格式化大数字
const formatNumber = (num: number): string => {
  if (num >= 1000000) {
    return (num / 1000000).toFixed(1) + 'M'
  }
  if (num >= 1000) {
    return (num / 1000).toFixed(1) + 'K'
  }
  return num.toString()
}

// 格式化存储大小
const formatStorage = (bytes: number): string => {
  if (bytes >= 1073741824) {
    return (bytes / 1073741824).toFixed(2) + ' GB'
  }
  if (bytes >= 1048576) {
    return (bytes / 1048576).toFixed(2) + ' MB'
  }
  if (bytes >= 1024) {
    return (bytes / 1024).toFixed(2) + ' KB'
  }
  return bytes + ' B'
}

// 获取服务状态
async function fetchServiceStatus() {
  const [rag, docs] = await Promise.allSettled([
    ragApi.health(),
    documentsApi.health()
  ])
  
  return {
    ragService: rag.status === 'fulfilled' ? 'UP' : 'DOWN',
    documentService: docs.status === 'fulfilled' ? 'UP' : 'DOWN'
  }
}

// 获取统计数据
async function fetchStats() {
  try {
    const res = await ragApi.getStats()
    return res.data
  } catch {
    return {
      totalDocuments: 0,
      totalVectors: 0,
      totalTokens: 0,
      monthlyTokens: 0,
      storageUsed: 0,
      storageTotal: 1073741824 // 默认 1GB
    }
  }
}

export default function DashboardPage() {
  const { data: serviceStatus } = useQuery({
    queryKey: ['service-status'],
    queryFn: fetchServiceStatus,
    refetchInterval: 30000
  })

  const { data: stats } = useQuery({
    queryKey: ['dashboard-stats'],
    queryFn: fetchStats,
    refetchInterval: 60000
  })

  // 生成模拟的Token趋势数据
  const tokenTrendData = [1200, 1450, 1320, 1680, 1890, 2100, 1950, 2300, 2560, 2780, 2450, 2890]
  const weekDays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

  // 生成文档分布数据
  const documentDistribution = [
    { label: 'PDF', value: 45, color: '#EF4444' },
    { label: 'Word', value: 28, color: '#3B82F6' },
    { label: 'TXT', value: 15, color: '#10B981' },
    { label: 'Markdown', value: 12, color: '#F59E0B' }
  ]

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-gray-800 dark:text-white">知识库仪表盘</h1>
        <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
          概览知识库运行状态和统计数据
        </p>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatsCard
          title="文档总数"
          value={formatNumber(stats?.totalDocuments || 0)}
          subtitle="已上传文档"
          color="blue"
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
          }
        />
        <StatsCard
          title="向量总数"
          value={formatNumber(stats?.totalVectors || 0)}
          subtitle="已向量化分块"
          color="green"
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19.428 15.428a2 2 0 00-1.022-.547l-2.387-.477a6 6 0 00-3.86.517l-.318.158a6 6 0 01-3.86.517L6.05 15.21a2 2 0 00-1.806.547M8 4h8l-1 1v5.172a2 2 0 00.586 1.414l5 5c1.26 1.26.367 3.414-1.415 3.414H4.828c-1.782 0-2.674-2.154-1.414-3.414l5-5A2 2 0 009 10.172V5L8 4z" />
            </svg>
          }
        />
        <StatsCard
          title="本月 Token"
          value={formatNumber(stats?.monthlyTokens || 0)}
          subtitle="本月使用量"
          color="purple"
          trend={{ value: 12.5, direction: 'up' }}
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
            </svg>
          }
        />
        <StatsCard
          title="存储空间"
          value={formatStorage(stats?.storageUsed || 0)}
          subtitle={`共 ${formatStorage(stats?.storageTotal || 1073741824)}`}
          color="yellow"
          icon={
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4m0 5c0 2.21-3.582 4-8 4s-8-1.79-8-4" />
            </svg>
          }
        />
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Token trend */}
        <div className="lg:col-span-2 bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-medium text-gray-800 dark:text-white">Token 使用趋势</h3>
            <span className="text-sm text-gray-500 dark:text-gray-400">近7天</span>
          </div>
          <LineChart 
            data={tokenTrendData.slice(-7)} 
            labels={weekDays}
            color="#8B5CF6"
          />
        </div>

        {/* Storage usage */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-medium text-gray-800 dark:text-white mb-4">存储使用</h3>
          <div className="flex justify-center">
            <ProgressRing
              value={stats?.storageUsed || 0}
              max={stats?.storageTotal || 1073741824}
              size={140}
              color="#F59E0B"
              label="已使用"
            />
          </div>
          <div className="mt-4 space-y-2 text-sm">
            <div className="flex justify-between">
              <span className="text-gray-500 dark:text-gray-400">已使用</span>
              <span className="text-gray-800 dark:text-white font-medium">
                {formatStorage(stats?.storageUsed || 0)}
              </span>
            </div>
            <div className="flex justify-between">
              <span className="text-gray-500 dark:text-gray-400">可用</span>
              <span className="text-gray-800 dark:text-white font-medium">
                {formatStorage((stats?.storageTotal || 1073741824) - (stats?.storageUsed || 0))}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Second row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4">
        {/* Document distribution */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-medium text-gray-800 dark:text-white mb-4">文档类型分布</h3>
          <DonutChart data={documentDistribution} size={140} />
        </div>

        {/* Service status */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-medium text-gray-800 dark:text-white mb-4">服务状态</h3>
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <span className="text-gray-600 dark:text-gray-400">RAG 服务</span>
              <div className="flex items-center gap-2">
                <span className={`w-2.5 h-2.5 rounded-full ${serviceStatus?.ragService === 'UP' ? 'bg-green-500' : 'bg-red-500'}`} />
                <span className={`text-sm font-medium ${serviceStatus?.ragService === 'UP' ? 'text-green-500' : 'text-red-500'}`}>
                  {serviceStatus?.ragService === 'UP' ? '运行中' : '已停止'}
                </span>
              </div>
            </div>
            <div className="flex items-center justify-between">
              <span className="text-gray-600 dark:text-gray-400">文档服务</span>
              <div className="flex items-center gap-2">
                <span className={`w-2.5 h-2.5 rounded-full ${serviceStatus?.documentService === 'UP' ? 'bg-green-500' : 'bg-red-500'}`} />
                <span className={`text-sm font-medium ${serviceStatus?.documentService === 'UP' ? 'text-green-500' : 'text-red-500'}`}>
                  {serviceStatus?.documentService === 'UP' ? '运行中' : '已停止'}
                </span>
              </div>
            </div>
          </div>
        </div>

        {/* Quick actions */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <h3 className="text-lg font-medium text-gray-800 dark:text-white mb-4">快捷操作</h3>
          <div className="grid grid-cols-2 gap-3">
            <Link
              to="/documents"
              className="flex flex-col items-center p-3 rounded-lg bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors"
            >
              <svg className="w-6 h-6 text-blue-500 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
              </svg>
              <span className="text-sm text-gray-700 dark:text-gray-300">上传文档</span>
            </Link>
            <Link
              to="/query"
              className="flex flex-col items-center p-3 rounded-lg bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors"
            >
              <svg className="w-6 h-6 text-green-500 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
              </svg>
              <span className="text-sm text-gray-700 dark:text-gray-300">RAG对话</span>
            </Link>
            <Link
              to="/embeddings"
              className="flex flex-col items-center p-3 rounded-lg bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors"
            >
              <svg className="w-6 h-6 text-purple-500 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4" />
              </svg>
              <span className="text-sm text-gray-700 dark:text-gray-300">向量管理</span>
            </Link>
            <Link
              to="/monitoring"
              className="flex flex-col items-center p-3 rounded-lg bg-gray-50 dark:bg-gray-700 hover:bg-gray-100 dark:hover:bg-gray-600 transition-colors"
            >
              <svg className="w-6 h-6 text-yellow-500 mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
              </svg>
              <span className="text-sm text-gray-700 dark:text-gray-300">监控面板</span>
            </Link>
          </div>
        </div>
      </div>

      {/* Activity list */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Recent documents */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-medium text-gray-800 dark:text-white">最近上传</h3>
            <Link to="/documents" className="text-sm text-blue-500 hover:text-blue-600">
              查看全部
            </Link>
          </div>
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700">
                <div className="p-2 bg-gray-100 dark:bg-gray-700 rounded">
                  <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                  </svg>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-gray-800 dark:text-gray-200 truncate">文档示例_{i}.pdf</p>
                  <p className="text-xs text-gray-400">刚刚</p>
                </div>
                <span className="px-2 py-0.5 bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400 text-xs rounded">
                  已完成
                </span>
              </div>
            ))}
          </div>
        </div>

        {/* Recent conversations */}
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center justify-between mb-4">
            <h3 className="text-lg font-medium text-gray-800 dark:text-white">最近对话</h3>
            <Link to="/query" className="text-sm text-blue-500 hover:text-blue-600">
              查看全部
            </Link>
          </div>
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="flex items-center gap-3 p-2 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700 cursor-pointer">
                <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded">
                  <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                  </svg>
                </div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm text-gray-800 dark:text-gray-200 truncate">
                    {i === 1 ? '关于知识库的使用问题' : i === 2 ? '向量检索的原理是什么' : '如何优化检索效果'}
                  </p>
                  <p className="text-xs text-gray-400">{i} 分钟前</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
