import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import AsyncState from '@/components/common/AsyncState'
import { embeddingsApi } from '@/api/embeddings'

const modelOptions = [
  { value: 'text-embedding-3-small', label: 'text-embedding-3-small' },
  { value: 'text-embedding-v3', label: 'text-embedding-v3' }
]

export default function EmbeddingsPage() {
  const [text, setText] = useState('')
  const [model, setModel] = useState('text-embedding-3-small')
  const [taskId, setTaskId] = useState('')

  const createMutation = useMutation({
    mutationFn: (payload: { text: string; model: string; async: boolean }) => embeddingsApi.embedText(payload),
    onSuccess: (res) => {
      if (res.data?.taskId) {
        setTaskId(res.data.taskId)
      }
    }
  })

  const statusMutation = useMutation({
    mutationFn: (id: string) => embeddingsApi.getTaskStatus(id)
  })

  const handleCreateTask = async () => {
    if (!text.trim()) {
      alert('请输入文本')
      return
    }
    createMutation.mutate({ text, model, async: false })
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800 dark:text-white">向量任务</h1>

      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-6 border border-gray-200 dark:border-gray-700">
        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              文本
            </label>
            <textarea
              rows={5}
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="请输入要向量化的文本"
              className="w-full px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-lg
                         bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                         focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
              模型
            </label>
            <select
              value={model}
              onChange={(e) => setModel(e.target.value)}
              className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg
                         bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                         focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              {modelOptions.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          <div className="flex gap-3">
            <button
              onClick={handleCreateTask}
              disabled={createMutation.isPending}
              className="px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600
                         disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {createMutation.isPending ? '创建中...' : '创建向量任务'}
            </button>

            <button
              onClick={() => taskId && statusMutation.mutate(taskId)}
              disabled={!taskId || statusMutation.isPending}
              className="px-6 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300
                         rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600
                         disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {statusMutation.isPending ? '查询中...' : '刷新任务状态'}
            </button>
          </div>
        </div>

        {/* Task ID */}
        <div className="mt-4 text-sm text-gray-600 dark:text-gray-400">
          当前任务ID：{taskId || '-'}
        </div>

        {/* Errors */}
        {createMutation.isError || statusMutation.isError ? (
          <div className="mt-4">
            <AsyncState
              error={(createMutation.error as Error)?.message || (statusMutation.error as Error)?.message || '请求失败'}
            />
          </div>
        ) : null}

        {/* Result */}
        {(createMutation.data || statusMutation.data) && (
          <div className="mt-4 p-4 bg-gray-50 dark:bg-gray-700 rounded-lg border border-gray-200 dark:border-gray-600">
            <h4 className="font-medium text-gray-800 dark:text-white mb-2">任务结果</h4>
            <div className="space-y-2 text-sm">
              <p className="text-gray-600 dark:text-gray-400">
                状态：{(statusMutation.data ?? createMutation.data)?.data.status}
              </p>
              <p className="text-gray-600 dark:text-gray-400">
                模型：{(statusMutation.data ?? createMutation.data)?.data.model}
              </p>
              <p className="text-gray-600 dark:text-gray-400">
                维度：{(statusMutation.data ?? createMutation.data)?.data.dimension ?? '-'}
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
