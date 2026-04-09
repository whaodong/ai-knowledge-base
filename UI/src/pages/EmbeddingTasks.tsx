// 向量化任务管理页面
import { useState, useMemo } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { embeddingTasksApi } from '@/api/embedding-tasks'
import type { EmbeddingTask, EmbeddingTaskStatus, EmbeddingTaskQueryParams } from '@/types/embedding'
import AsyncState from '@/components/common/AsyncState'

const statusConfig: Record<string, { color: string; text: string; bgColor: string }> = {
  PENDING: { color: 'text-yellow-600 dark:text-yellow-400', bgColor: 'bg-yellow-100 dark:bg-yellow-900/30', text: '待处理' },
  PROCESSING: { color: 'text-blue-600 dark:text-blue-400', bgColor: 'bg-blue-100 dark:bg-blue-900/30', text: '处理中' },
  COMPLETED: { color: 'text-green-600 dark:text-green-400', bgColor: 'bg-green-100 dark:bg-green-900/30', text: '已完成' },
  FAILED: { color: 'text-red-600 dark:text-red-400', bgColor: 'bg-red-100 dark:bg-red-900/30', text: '失败' },
  CANCELLED: { color: 'text-gray-600 dark:text-gray-400', bgColor: 'bg-gray-100 dark:bg-gray-700', text: '已取消' }
}

const formatDate = (dateStr: string | undefined): string => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const formatDuration = (start: string | undefined, end: string | undefined): string => {
  if (!start) return '-'
  const startTime = new Date(start).getTime()
  const endTime = end ? new Date(end).getTime() : Date.now()
  const duration = Math.floor((endTime - startTime) / 1000)
  
  if (duration < 60) return `${duration}s`
  if (duration < 3600) return `${Math.floor(duration / 60)}m ${duration % 60}s`
  return `${Math.floor(duration / 3600)}h ${Math.floor((duration % 3600) / 60)}m`
}

const EmbeddingTasks = () => {
  const queryClient = useQueryClient()
  const [selectedRowKeys, setSelectedRowKeys] = useState<Set<string>>(new Set())
  const [params, setParams] = useState<EmbeddingTaskQueryParams>({
    pageNum: 1,
    pageSize: 10
  })
  const [statusFilter, setStatusFilter] = useState<EmbeddingTaskStatus | ''>('')
  const [showDetail, setShowDetail] = useState<EmbeddingTask | null>(null)

  // 获取任务列表
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['embedding-tasks', params, statusFilter],
    queryFn: () => embeddingTasksApi.getTasks({ ...params, status: statusFilter || undefined }),
    refetchInterval: 5000
  })

  const records = data?.data?.records ?? []
  const total = data?.data?.total ?? 0

  // 统计数据
  const stats = useMemo(() => {
    return {
      total: records.length,
      pending: records.filter((t: EmbeddingTask) => t.status === 'PENDING').length,
      processing: records.filter((t: EmbeddingTask) => t.status === 'PROCESSING').length,
      completed: records.filter((t: EmbeddingTask) => t.status === 'COMPLETED').length,
      failed: records.filter((t: EmbeddingTask) => t.status === 'FAILED').length
    }
  }, [records])

  // 重试任务
  const retryMutation = useMutation({
    mutationFn: (id: string) => embeddingTasksApi.retryTaskById(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['embedding-tasks'] })
    }
  })

  // 批量重试失败任务
  const batchRetryMutation = useMutation({
    mutationFn: (ids: string[]) => embeddingTasksApi.batchRetryTasksByIds(ids),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['embedding-tasks'] })
      setSelectedRowKeys(new Set())
    }
  })

  // 取消任务
  const cancelMutation = useMutation({
    mutationFn: (id: string) => embeddingTasksApi.cancelTaskById(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['embedding-tasks'] })
    }
  })

  // 删除任务
  const deleteMutation = useMutation({
    mutationFn: (id: string) => embeddingTasksApi.deleteTaskById(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['embedding-tasks'] })
    }
  })

  const handleRetry = (task: EmbeddingTask) => {
    if (confirm(`确定重试任务 "${task.documentName}"？`)) {
      retryMutation.mutate(task.id)
    }
  }

  const handleCancel = (task: EmbeddingTask) => {
    if (confirm(`确定取消任务 "${task.documentName}"？`)) {
      cancelMutation.mutate(task.id)
    }
  }

  const handleDelete = (task: EmbeddingTask) => {
    if (confirm(`确定删除任务 "${task.documentName}"？`)) {
      deleteMutation.mutate(task.id)
    }
  }

  const handleBatchRetry = () => {
    const failedIds = records
      .filter((t: EmbeddingTask) => t.status === 'FAILED' && selectedRowKeys.has(t.id))
      .map((t: EmbeddingTask) => t.id)
    
    if (failedIds.length === 0) {
      alert('请选择失败的任务')
      return
    }
    
    if (confirm(`确定重试选中的 ${failedIds.length} 个失败任务？`)) {
      batchRetryMutation.mutate(failedIds)
    }
  }

  const handleRetryAllFailed = () => {
    const failedIds = records
      .filter((t: EmbeddingTask) => t.status === 'FAILED')
      .map((t: EmbeddingTask) => t.id)
    
    if (failedIds.length === 0) {
      alert('没有失败的任务')
      return
    }
    
    if (confirm(`确定重试所有 ${failedIds.length} 个失败任务？`)) {
      batchRetryMutation.mutate(failedIds)
    }
  }

  const handleSelectAll = () => {
    if (selectedRowKeys.size === records.length) {
      setSelectedRowKeys(new Set())
    } else {
      setSelectedRowKeys(new Set(records.map((t: EmbeddingTask) => t.id)))
    }
  }

  const handleSelectOne = (id: string) => {
    const newSet = new Set(selectedRowKeys)
    if (newSet.has(id)) {
      newSet.delete(id)
    } else {
      newSet.add(id)
    }
    setSelectedRowKeys(newSet)
  }

  return (
    <div className="space-y-4">
      {/* Header */}
      <div className="flex justify-between items-center">
        <div>
          <h1 className="text-2xl font-bold text-gray-800 dark:text-white">向量化任务</h1>
          <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
            管理文档向量化处理任务
          </p>
        </div>
        <div className="flex gap-2">
          <button
            onClick={handleRetryAllFailed}
            disabled={stats.failed === 0 || batchRetryMutation.isPending}
            className="px-4 py-2 bg-yellow-500 text-white rounded-lg hover:bg-yellow-600 disabled:opacity-50 flex items-center gap-2"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            重试失败 ({stats.failed})
          </button>
          {selectedRowKeys.size > 0 && (
            <button
              onClick={handleBatchRetry}
              disabled={batchRetryMutation.isPending}
              className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:opacity-50 flex items-center gap-2"
            >
              <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              批量重试 ({selectedRowKeys.size})
            </button>
          )}
          <button
            onClick={() => refetch()}
            className="px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600 flex items-center gap-2"
          >
            <svg className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
            刷新
          </button>
        </div>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-gray-100 dark:bg-gray-700 rounded-lg">
              <svg className="w-5 h-5 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
              </svg>
            </div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">总任务</p>
              <p className="text-xl font-bold text-gray-800 dark:text-white">{stats.total}</p>
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
              <p className="text-xl font-bold text-yellow-600 dark:text-yellow-400">{stats.pending}</p>
            </div>
          </div>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-100 dark:bg-blue-900/30 rounded-lg">
              <svg className="w-5 h-5 text-blue-500 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
            </div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">处理中</p>
              <p className="text-xl font-bold text-blue-600 dark:text-blue-400">{stats.processing}</p>
            </div>
          </div>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-green-100 dark:bg-green-900/30 rounded-lg">
              <svg className="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">已完成</p>
              <p className="text-xl font-bold text-green-600 dark:text-green-400">{stats.completed}</p>
            </div>
          </div>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-red-100 dark:bg-red-900/30 rounded-lg">
              <svg className="w-5 h-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <div>
              <p className="text-sm text-gray-500 dark:text-gray-400">失败</p>
              <p className="text-xl font-bold text-red-600 dark:text-red-400">{stats.failed}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
        <div className="flex flex-wrap items-center gap-4">
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as EmbeddingTaskStatus | '')}
            className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg 
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white"
          >
            <option value="">全部状态</option>
            <option value="PENDING">待处理</option>
            <option value="PROCESSING">处理中</option>
            <option value="COMPLETED">已完成</option>
            <option value="FAILED">失败</option>
            <option value="CANCELLED">已取消</option>
          </select>
        </div>
      </div>

      {/* Tasks table */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
        {isLoading || isError ? (
          <div className="p-8">
            <AsyncState
              loading={isLoading}
              error={isError ? (error as Error).message : null}
            />
          </div>
        ) : records.length === 0 ? (
          <div className="p-8 text-center">
            <svg className="w-12 h-12 mx-auto text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
            </svg>
            <p className="text-gray-500 dark:text-gray-400">暂无任务数据</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700/50">
                <tr>
                  <th className="px-4 py-3 text-left w-10">
                    <input
                      type="checkbox"
                      checked={selectedRowKeys.size === records.length}
                      onChange={handleSelectAll}
                      className="w-4 h-4 text-blue-500 border-gray-300 rounded"
                    />
                  </th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">文档</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">状态</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-40">进度</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-28">耗时</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-32">创建时间</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-28">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {records.map((task: EmbeddingTask) => (
                  <tr 
                    key={task.id}
                    className={`hover:bg-gray-50 dark:hover:bg-gray-700/50 ${selectedRowKeys.has(task.id) ? 'bg-blue-50 dark:bg-blue-900/10' : ''}`}
                  >
                    <td className="px-4 py-3">
                      <input
                        type="checkbox"
                        checked={selectedRowKeys.has(task.id)}
                        onChange={() => handleSelectOne(task.id)}
                        className="w-4 h-4 text-blue-500 border-gray-300 rounded"
                      />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <svg className="w-5 h-5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                        <div className="min-w-0">
                          <p className="text-sm text-gray-900 dark:text-gray-100 truncate max-w-xs">
                            {task.documentName || `文档 ${task.documentId}`}
                          </p>
                          {task.errorMessage && (
                            <p className="text-xs text-red-500 truncate max-w-xs" title={task.errorMessage}>
                              {task.errorMessage}
                            </p>
                          )}
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-1 rounded text-xs ${statusConfig[task.status]?.bgColor} ${statusConfig[task.status]?.color}`}>
                        {statusConfig[task.status]?.text || task.status}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <div className="flex-1 h-2 bg-gray-200 dark:bg-gray-600 rounded-full overflow-hidden max-w-24">
                          <div
                            className={`h-full transition-all ${
                              task.status === 'COMPLETED' ? 'bg-green-500' :
                              task.status === 'FAILED' ? 'bg-red-500' :
                              task.status === 'PROCESSING' ? 'bg-blue-500' : 'bg-gray-400'
                            }`}
                            style={{ width: `${task.progress}%` }}
                          />
                        </div>
                        <span className="text-xs text-gray-500 dark:text-gray-400">
                          {task.progress}%
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">
                      {formatDuration(task.createTime, task.endTime)}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">
                      {formatDate(task.createTime)}
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-1">
                        {task.status === 'FAILED' && (
                          <button
                            onClick={() => handleRetry(task)}
                            className="p-1.5 text-yellow-500 hover:text-yellow-600 hover:bg-yellow-50 dark:hover:bg-yellow-900/20 rounded"
                            title="重试"
                          >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                            </svg>
                          </button>
                        )}
                        {task.status === 'PENDING' && (
                          <button
                            onClick={() => handleCancel(task)}
                            className="p-1.5 text-gray-500 hover:text-gray-600 hover:bg-gray-100 dark:hover:bg-gray-700 rounded"
                            title="取消"
                          >
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                            </svg>
                          </button>
                        )}
                        <button
                          onClick={() => setShowDetail(task)}
                          className="p-1.5 text-blue-500 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-blue-900/20 rounded"
                          title="详情"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                          </svg>
                        </button>
                        <button
                          onClick={() => handleDelete(task)}
                          className="p-1.5 text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded"
                          title="删除"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {total > params.pageSize && (
          <div className="px-4 py-3 border-t border-gray-200 dark:border-gray-700 flex items-center justify-between">
            <div className="text-sm text-gray-500 dark:text-gray-400">
              共 {total} 条记录，已选择 {selectedRowKeys.size} 项
            </div>
            <div className="flex items-center gap-2">
              <button
                onClick={() => setParams(p => ({ ...p, pageNum: Math.max(1, p.pageNum - 1) }))}
                disabled={params.pageNum === 1}
                className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded-md text-sm
                           disabled:opacity-50 disabled:cursor-not-allowed
                           hover:bg-gray-50 dark:hover:bg-gray-700"
              >
                上一页
              </button>
              <span className="text-sm text-gray-600 dark:text-gray-400">
                第 {params.pageNum} / {Math.ceil(total / params.pageSize)} 页
              </span>
              <button
                onClick={() => setParams(p => ({ ...p, pageNum: p.pageNum + 1 }))}
                disabled={params.pageNum >= Math.ceil(total / params.pageSize)}
                className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded-md text-sm
                           disabled:opacity-50 disabled:cursor-not-allowed
                           hover:bg-gray-50 dark:hover:bg-gray-700"
              >
                下一页
              </button>
            </div>
          </div>
        )}
      </div>

      {/* Task Detail Modal */}
      {showDetail && (
        <div className="fixed inset-0 z-50 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/50" onClick={() => setShowDetail(null)} />
          <div className="relative bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-lg mx-4">
            <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
              <h3 className="text-lg font-medium text-gray-900 dark:text-white">任务详情</h3>
              <button onClick={() => setShowDetail(null)} className="text-gray-400 hover:text-gray-600">
                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm text-gray-500 dark:text-gray-400">文档名称</label>
                  <p className="mt-1 text-gray-900 dark:text-white">{showDetail.documentName || '-'}</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500 dark:text-gray-400">状态</label>
                  <span className={`inline-block mt-1 px-2 py-1 rounded text-xs ${statusConfig[showDetail.status]?.bgColor} ${statusConfig[showDetail.status]?.color}`}>
                    {statusConfig[showDetail.status]?.text}
                  </span>
                </div>
                <div>
                  <label className="block text-sm text-gray-500 dark:text-gray-400">文档ID</label>
                  <p className="mt-1 text-gray-900 dark:text-white">{showDetail.documentId}</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500 dark:text-gray-400">任务ID</label>
                  <p className="mt-1 text-gray-900 dark:text-white">{showDetail.id}</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500 dark:text-gray-400">重试次数</label>
                  <p className="mt-1 text-gray-900 dark:text-white">{showDetail.retryCount}</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500 dark:text-gray-400">进度</label>
                  <p className="mt-1 text-gray-900 dark:text-white">{showDetail.progress}%</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500 dark:text-gray-400">完成分块</label>
                  <p className="mt-1 text-gray-900 dark:text-white">{showDetail.completedChunks} / {showDetail.totalChunks}</p>
                </div>
                <div>
                  <label className="block text-sm text-gray-500 dark:text-gray-400">失败分块</label>
                  <p className="mt-1 text-red-500">{showDetail.failedChunks}</p>
                </div>
                {showDetail.errorMessage && (
                  <div className="col-span-2">
                    <label className="block text-sm text-gray-500 dark:text-gray-400">错误信息</label>
                    <p className="mt-1 text-red-500">{showDetail.errorMessage}</p>
                  </div>
                )}
              </div>
            </div>
            <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-end">
              <button
                onClick={() => setShowDetail(null)}
                className="px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600"
              >
                关闭
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default EmbeddingTasks
