import apiClient from './client'
import type { Result } from '@/types/api'
import type { EmbeddingRequest, EmbeddingResponse, EmbeddingBatchRequest, EmbeddingBatchResponse } from '@/types/embedding'

export const embeddingsApi = {
  // 文本向量化
  embedText: (request: EmbeddingRequest): Promise<Result<EmbeddingResponse>> =>
    apiClient.post('/api/v1/embeddings', request),

  // 批量向量化
  batchEmbed: (request: EmbeddingBatchRequest): Promise<Result<EmbeddingBatchResponse>> =>
    apiClient.post('/api/v1/embeddings/batch', request),

  // 查询任务状态
  getTaskStatus: (taskId: string): Promise<Result<EmbeddingResponse>> =>
    apiClient.get(`/api/v1/embeddings/status/${taskId}`),

  // 健康检查
  health: (): Promise<Record<string, unknown>> =>
    apiClient.get('/api/v1/embeddings/health')
}
