// 向量化任务类型定义

// 重新导出 embedding.ts 中的类型
export type { EmbeddingTask, EmbeddingTaskStatus } from './embedding'
export { EMBEDDING_TASK_STATUS_MAP } from './embedding'

// 向量化任务查询参数
export interface EmbeddingTaskQueryParams {
  pageNum: number
  pageSize: number
  status?: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'
  documentName?: string
  startDate?: string
  endDate?: string
}

// 创建向量化任务请求
export interface CreateEmbeddingTaskRequest {
  documentIds: number[]
  model?: string
  priority?: number
}

// 批量向量化响应
export interface BatchEmbeddingResponse {
  batchId: string
  total: number
  successCount: number
  failedCount: number
  taskIds: (string | number)[]
}
