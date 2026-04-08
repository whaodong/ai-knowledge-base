import apiClient from './client'
import type { Result, PageResponse } from '@/types/api'
import type {
  EmbeddingTask,
  EmbeddingTaskQueryParams,
  EmbeddingTaskStats,
  BatchEmbeddingRequest
} from '@/types/embedding'

export const embeddingApi = {
  // 获取任务列表（分页）
  getEmbeddingTasks: (params: EmbeddingTaskQueryParams): Promise<Result<PageResponse<EmbeddingTask>>> =>
    apiClient.get('/api/v1/embedding/tasks', { params }),

  // 获取任务详情
  getEmbeddingTask: (id: string): Promise<Result<EmbeddingTask>> =>
    apiClient.get(`/api/v1/embedding/tasks/${id}`),

  // 获取任务统计
  getEmbeddingTaskStats: (): Promise<Result<EmbeddingTaskStats>> =>
    apiClient.get('/api/v1/embedding/stats'),

  // 批量提交任务
  batchSubmitTasks: (request: BatchEmbeddingRequest): Promise<Result<{ taskIds: string[] }>> =>
    apiClient.post('/api/v1/embedding/tasks/batch', request),

  // 重试失败任务
  retryTask: (id: string): Promise<Result<void>> =>
    apiClient.post(`/api/v1/embedding/tasks/${id}/retry`),

  // 批量重试失败任务
  batchRetryFailedTasks: (ids?: string[]): Promise<Result<{ retriedCount: number }>> =>
    apiClient.post('/api/v1/embedding/tasks/retry-failed', { taskIds: ids }),

  // 取消任务
  cancelTask: (id: string): Promise<Result<void>> =>
    apiClient.post(`/api/v1/embedding/tasks/${id}/cancel`),

  // 删除任务
  deleteTask: (id: string): Promise<Result<void>> =>
    apiClient.delete(`/api/v1/embedding/tasks/${id}`),

  // 批量删除任务
  batchDeleteTasks: (ids: string[]): Promise<Result<void>> =>
    apiClient.delete('/api/v1/embedding/tasks/batch', { data: ids })
}
