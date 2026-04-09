import { useState } from 'react'
import type { EmbeddingTask, EmbeddingTaskStatus, EmbeddingTaskStats, EmbeddingTaskQueryParams } from '@/types/embedding'

// 模拟数据
const mockTasks: EmbeddingTask[] = [
  {
    id: '1',
    documentId: 101,
    documentName: '产品需求文档.pdf',
    status: 'COMPLETED',
    totalChunks: 50,
    completedChunks: 50,
    failedChunks: 0,
    progress: 100,
    inputTokens: 12500,
    outputTokens: 8000,
    startTime: '2024-01-07 10:00:00',
    endTime: '2024-01-07 10:02:35',
    createTime: '2024-01-07 09:58:00',
    retryCount: 0
  },
  {
    id: '2',
    documentId: 102,
    documentName: '技术架构设计.docx',
    status: 'PROCESSING',
    totalChunks: 120,
    completedChunks: 78,
    failedChunks: 2,
    progress: 65,
    inputTokens: 28000,
    outputTokens: 0,
    startTime: '2024-01-07 11:30:00',
    createTime: '2024-01-07 11:25:00',
    retryCount: 1
  },
  {
    id: '3',
    documentId: 103,
    documentName: '用户手册.md',
    status: 'PENDING',
    totalChunks: 35,
    completedChunks: 0,
    failedChunks: 0,
    progress: 0,
    inputTokens: 0,
    outputTokens: 0,
    createTime: '2024-01-07 12:00:00',
    retryCount: 0
  },
  {
    id: '4',
    documentId: 104,
    documentName: 'API接口文档.pdf',
    status: 'FAILED',
    totalChunks: 80,
    completedChunks: 45,
    failedChunks: 35,
    progress: 56,
    inputTokens: 15000,
    outputTokens: 12000,
    startTime: '2024-01-07 09:00:00',
    createTime: '2024-01-07 08:55:00',
    errorMessage: '向量维度不匹配',
    retryCount: 3
  }
]

const mockStats: EmbeddingTaskStats = {
  totalTasks: 156,
  pendingTasks: 12,
  processingTasks: 5,
  completedTasks: 128,
  failedTasks: 11,
  avgProcessingTime: 45
}

const statusConfig: Record<string, { color: string; text: string }> = {
  PENDING: { color: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400', text: '待处理' },
  PROCESSING: { color: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400', text: '处理中' },
  COMPLETED: { color: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400', text: '已完成' },
  FAILED: { color: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400', text: '失败' }
}

const EmbeddingTasks = () => {
  const [tasks] = useState<EmbeddingTask[]>(mockTasks)
  const [stats] = useState<EmbeddingTaskStats>(mockStats)
  const [selectedRowKeys] = useState<string[]>([])
  const [params, setParams] = useState<EmbeddingTaskQueryParams>({
    pageNum: 1,
    pageSize: 10
  })

  const handleRetry = (id: string) => {
    alert(`重试任务 ${id}`)
  }

  const handleCancel = (id: string) => {
    alert(`取消任务 ${id}`)
  }

  const handleDelete = (id: string) => {
    alert(`删除任务 ${id}`)
  }

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-800 dark:text-white">向量化任务</h1>
        <div className="flex gap-2">
          <button className="px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 flex items-center gap-2">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            重试失败任务
          </button>
          <button className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 flex items-center gap-2">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            批量提交 ({selectedRowKeys.length})
          </button>
          <button className="px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600">
            刷新
          </button>
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
              <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">总任务数</p>
              <p className="text-xl font-bold text-gray-800 dark:text-white">{stats.totalTasks}</p>
            </div>
          </div>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-yellow-100 dark:bg-yellow-900/30 rounded-lg">
              <svg className="w-5 h-5 text-yellow-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">待处理</p>
              <p className="text-xl font-bold text-gray-800 dark:text-white">{stats.pendingTasks}</p>
            </div>
          </div>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
              <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">进行中</p>
              <p className="text-xl font-bold text-gray-800 dark:text-white">{stats.processingTasks}</p>
            </div>
          </div>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-red-100 dark:bg-red-900/30 rounded-lg">
              <svg className="w-5 h-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
              </svg>
            </div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">失败任务</p>
              <p className="text-xl font-bold text-red-500">{stats.failedTasks}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Filter */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
        <div className="flex flex-wrap gap-3">
          <select
            className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
            onChange={(e) => setParams({ ...params, status: e.target.value as EmbeddingTaskStatus })}
          >
            <option value="">全部状态</option>
            {Object.entries(statusConfig).map(([value, config]) => (
              <option key={value} value={value}>{config.text}</option>
            ))}
          </select>
          <input
            type="text"
            placeholder="搜索文档名称"
            className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
          />
        </div>
      </div>

      {/* Table */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-gray-50 dark:bg-gray-700/50">
              <tr>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">ID</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">文档名称</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">状态</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-48">进度</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">重试</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-36">创建时间</th>
                <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-36">操作</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
              {tasks.map((task) => (
                <tr key={task.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                  <td className="px-4 py-3 text-sm text-gray-900 dark:text-gray-100">{task.id}</td>
                  <td className="px-4 py-3 text-sm text-gray-900 dark:text-gray-100 truncate max-w-xs">{task.documentName}</td>
                  <td className="px-4 py-3 text-sm">
                    <span className={`px-2 py-1 rounded text-xs ${statusConfig[task.status]?.color}`}>
                      {statusConfig[task.status]?.text || task.status}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <div className="flex items-center gap-2">
                      <div className="flex-1 h-2 bg-gray-200 dark:bg-gray-600 rounded-full overflow-hidden">
                        <div
                          className={`h-full rounded-full ${task.status === 'FAILED' ? 'bg-red-500' : 'bg-blue-500'}`}
                          style={{ width: `${task.progress}%` }}
                        />
                      </div>
                      <span className="text-xs text-gray-500">{task.completedChunks}/{task.totalChunks}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">
                    {task.retryCount > 0 ? (
                      <span className="text-yellow-500">{task.retryCount}</span>
                    ) : '-'}
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">
                    {task.createTime?.split(' ')[0]}
                  </td>
                  <td className="px-4 py-3 text-sm">
                    <div className="flex items-center gap-2">
                      {task.status === 'FAILED' && (
                        <button
                          onClick={() => handleRetry(task.id)}
                          className="text-blue-500 hover:text-blue-600"
                        >
                          重试
                        </button>
                      )}
                      {task.status === 'PENDING' && (
                        <button
                          onClick={() => handleCancel(task.id)}
                          className="text-red-500 hover:text-red-600"
                        >
                          取消
                        </button>
                      )}
                      {task.status !== 'PROCESSING' && (
                        <button
                          onClick={() => handleDelete(task.id)}
                          className="text-red-500 hover:text-red-600"
                        >
                          删除
                        </button>
                      )}
                    </div>
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

export default EmbeddingTasks
