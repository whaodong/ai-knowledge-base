import { useState } from 'react'
import type { CacheEntry, CacheStats, CacheType } from '@/types/cache'

// 模拟数据
const mockStats: CacheStats = {
  hitCount: 85678,
  missCount: 12456,
  hitRate: 87.3,
  totalKeys: 4523,
  totalSize: 128 * 1024 * 1024,
  avgTtl: 3600,
  memoryUsed: 256 * 1024 * 1024,
  memoryLimit: 512 * 1024 * 1024
}

const mockCacheEntries: CacheEntry[] = [
  { key: 'embedding:doc_101_chunk_0', value: '{"vector":[...]}', size: 4096, hitCount: 45, lastAccessTime: '2024-01-07 14:30:00', createTime: '2024-01-07 10:00:00', ttl: 7200, expired: false },
  { key: 'query:user001_session123', value: '{"messages":[...]}', size: 15360, hitCount: 78, lastAccessTime: '2024-01-07 14:28:00', createTime: '2024-01-07 09:00:00', ttl: 1800, expired: false },
  { key: 'vector:search_result_abc123', value: '{"results":[...]}', size: 6144, hitCount: 12, lastAccessTime: '2024-01-07 13:00:00', createTime: '2024-01-07 12:00:00', ttl: 600, expired: false },
  { key: 'embedding:doc_105_chunk_2', value: '{"vector":[...]}', size: 3072, hitCount: 0, lastAccessTime: '2024-01-07 08:00:00', createTime: '2024-01-07 08:00:00', ttl: 7200, expired: true }
]

const formatSize = (bytes: number): string => {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

const Cache = () => {
  const [stats] = useState<CacheStats>(mockStats)
  const [cacheEntries] = useState<CacheEntry[]>(mockCacheEntries)
  const [selectedCacheType, setSelectedCacheType] = useState<CacheType>('ALL')
  const [searchKey, setSearchKey] = useState('')

  const filteredEntries = searchKey
    ? cacheEntries.filter(e => e.key.toLowerCase().includes(searchKey.toLowerCase()))
    : cacheEntries

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-800 dark:text-white">缓存管理</h1>
        <div className="flex gap-2">
          <button className="px-4 py-2 bg-orange-100 dark:bg-orange-900/30 text-orange-600 dark:text-orange-400 rounded-lg hover:bg-orange-200 dark:hover:bg-orange-900/50 flex items-center gap-2">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17.657 18.657A8 8 0 016.343 7.343S7 9 9 10c0-2 .5-5 2.986-7C14 5 16.09 5.777 17.656 7.343A7.975 7.975 0 0120 13a7.975 7.975 0 01-2.343 5.657z" />
            </svg>
            缓存预热
          </button>
          <button className="px-4 py-2 bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400 rounded-lg hover:bg-red-200 dark:hover:bg-red-900/50 flex items-center gap-2">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
            </svg>
            清除缓存
          </button>
          <button className="px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600">
            刷新
          </button>
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">缓存命中率</p>
          <p className={`text-2xl font-bold mt-1 ${stats.hitRate > 80 ? 'text-green-500' : 'text-yellow-500'}`}>
            {stats.hitRate.toFixed(1)}%
          </p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">总缓存键数</p>
          <p className="text-2xl font-bold text-blue-500 mt-1">{stats.totalKeys.toLocaleString()}</p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">缓存大小</p>
          <p className="text-2xl font-bold text-purple-500 mt-1">{formatSize(stats.totalSize)}</p>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <p className="text-sm text-gray-500 dark:text-gray-400">平均TTL</p>
          <p className="text-2xl font-bold text-yellow-500 mt-1">{Math.floor(stats.avgTtl / 60)}分钟</p>
        </div>
      </div>

      {/* Charts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <h2 className="text-lg font-medium text-gray-800 dark:text-white mb-4">缓存命中率</h2>
          <div className="flex items-center justify-center">
            <div className="relative w-48 h-48">
              <svg className="w-full h-full transform -rotate-90">
                <circle
                  cx="96"
                  cy="96"
                  r="80"
                  strokeWidth="16"
                  stroke="currentColor"
                  fill="none"
                  className="text-gray-200 dark:text-gray-700"
                />
                <circle
                  cx="96"
                  cy="96"
                  r="80"
                  strokeWidth="16"
                  stroke="currentColor"
                  fill="none"
                  className={stats.hitRate > 80 ? 'text-green-500' : 'text-yellow-500'}
                  strokeDasharray={`${stats.hitRate * 5.02} 502`}
                />
              </svg>
              <div className="absolute inset-0 flex items-center justify-center">
                <span className={`text-3xl font-bold ${stats.hitRate > 80 ? 'text-green-500' : 'text-yellow-500'}`}>
                  {stats.hitRate.toFixed(1)}%
                </span>
              </div>
            </div>
          </div>
        </div>

        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
          <h2 className="text-lg font-medium text-gray-800 dark:text-white mb-4">内存使用情况</h2>
          <div className="space-y-4">
            <div>
              <div className="flex justify-between text-sm mb-1">
                <span className="text-gray-500 dark:text-gray-400">内存使用</span>
                <span className="text-gray-800 dark:text-gray-200">
                  {formatSize(stats.memoryUsed)} / {formatSize(stats.memoryLimit)}
                </span>
              </div>
              <div className="h-3 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
                <div
                  className={`h-full rounded-full ${(stats.memoryUsed / stats.memoryLimit) > 0.8 ? 'bg-red-500' : 'bg-blue-500'}`}
                  style={{ width: `${(stats.memoryUsed / stats.memoryLimit) * 100}%` }}
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-4 pt-4">
              <div className="text-center">
                <p className="text-2xl font-bold text-green-500">{stats.hitCount.toLocaleString()}</p>
                <p className="text-sm text-gray-500 dark:text-gray-400">命中次数</p>
              </div>
              <div className="text-center">
                <p className="text-2xl font-bold text-red-500">{stats.missCount.toLocaleString()}</p>
                <p className="text-sm text-gray-500 dark:text-gray-400">未命中次数</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Cache list */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div className="p-4 border-b border-gray-200 dark:border-gray-700 flex flex-wrap gap-3">
          <select
            value={selectedCacheType}
            onChange={(e) => setSelectedCacheType(e.target.value as CacheType)}
            className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
          >
            <option value="ALL">全部</option>
            <option value="EMBEDDING">Embedding</option>
            <option value="QUERY">Query</option>
            <option value="DOCUMENT">Document</option>
            <option value="VECTOR">Vector</option>
          </select>
          <input
            type="text"
            placeholder="搜索缓存键"
            value={searchKey}
            onChange={(e) => setSearchKey(e.target.value)}
            className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white w-60"
          />
        </div>

        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 dark:bg-gray-700/50">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">缓存键</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">大小</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">命中</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">TTL</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-20">状态</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-36">最后访问</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-20">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {filteredEntries.map((entry) => (
                <tr key={entry.key} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                  <td className="px-4 py-3 text-sm">
                    <code className="text-xs bg-gray-100 dark:bg-gray-700 px-2 py-1 rounded text-gray-600 dark:text-gray-400 truncate max-w-xs block">
                      {entry.key}
                    </code>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{formatSize(entry.size)}</td>
                  <td className="px-4 py-3 text-sm">
                    <span className={`px-2 py-1 rounded text-xs ${entry.hitCount > 50 ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' : 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-400'}`}>
                      {entry.hitCount}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{Math.floor(entry.ttl / 60)}分</td>
                  <td className="px-4 py-3 text-sm">
                    <span className={`px-2 py-1 rounded text-xs ${entry.expired ? 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400' : 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400'}`}>
                      {entry.expired ? '已过期' : '有效'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">{entry.lastAccessTime}</td>
                  <td className="px-4 py-3 text-sm">
                    <button className="text-red-500 hover:text-red-600">删除</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  )
}

export default Cache
