// 向量化任务 API 层
import apiClient from './client'
import type { Result, PageResponse } from '@/types/api'
import type { EmbeddingTask, EmbeddingTaskQueryParams, CreateEmbeddingTaskRequest, BatchEmbeddingResponse } from '@/types/embedding-task'

export const embeddingTasksApi = {
  // 获取任务列表
  getTasks: (params: EmbeddingTaskQueryParams): Promise<Result<PageResponse<EmbeddingTask>>> =>
    apiClient.get('/api/v1/embeddings/tasks', { params }),

  // 获取任务详情
  getTaskDetail: (id: string): Promise<Result<EmbeddingTask>> =>
    apiClient.get(`/api/v1/embeddings/tasks/${id}`),

  // 创建向量化任务
  createTask: (request: CreateEmbeddingTaskRequest): Promise<Result<BatchEmbeddingResponse>> =>
    apiClient.post('/api/v1/embeddings/tasks', request),

  // 重试失败任务
  retryTask: (id: number): Promise<Result<EmbeddingTask>> =>
    apiClient.post(`/api/v1/embeddings/tasks/${id}/retry`),

  // 重试失败任务 (string id)
  retryTaskById: (id: string): Promise<Result<EmbeddingTask>> =>
    apiClient.post(`/api/v1/embeddings/tasks/${id}/retry`),

  // 批量重试失败任务
  batchRetryTasks: (ids: number[]): Promise<Result<{ successCount: number; failedCount: number }>> =>
    apiClient.post('/api/v1/embeddings/tasks/batch/retry', { taskIds: ids }),

  // 批量重试失败任务 (string ids)
  batchRetryTasksByIds: (ids: string[]): Promise<Result<{ successCount: number; failedCount: number }>> =>
    apiClient.post('/api/v1/embeddings/tasks/batch/retry', { taskIds: ids }),

  // 取消任务
  cancelTask: (id: number): Promise<Result<void>> =>
    apiClient.post(`/api/v1/embeddings/tasks/${id}/cancel`),

  // 取消任务 (string id)
  cancelTaskById: (id: string): Promise<Result<void>> =>
    apiClient.post(`/api/v1/embeddings/tasks/${id}/cancel`),

  // 删除任务
  deleteTask: (id: number): Promise<Result<void>> =>
    apiClient.delete(`/api/v1/embeddings/tasks/${id}`),

  // 删除任务 (string id)
  deleteTaskById: (id: string): Promise<Result<void>> =>
    apiClient.delete(`/api/v1/embeddings/tasks/${id}`)
}
