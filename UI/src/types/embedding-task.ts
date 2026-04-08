// 向量化任务类型定义

// 向量化任务状态
export type EmbeddingTaskStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'

// 向量化任务
export interface EmbeddingTask {
  id: number
  documentId?: number
  documentName?: string
  textLength: number
  status: EmbeddingTaskStatus
  progress: number
  dimension?: number
  model?: string
  errorMessage?: string
  retryCount: number
  createTime: string
  updateTime?: string
  completeTime?: string
}

// 向量化任务查询参数
export interface EmbeddingTaskQueryParams {
  pageNum: number
  pageSize: number
  status?: EmbeddingTaskStatus
  documentName?: string
  startDate?: string
  endDate?: string
  sortBy?: string
  sortOrder?: 'ASC' | 'DESC'
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
  taskIds: number[]
}

// 向量化任务状态映射
export const EMBEDDING_TASK_STATUS_MAP: Record<EmbeddingTaskStatus, { text: string; color: string }> = {
  'PENDING': { text: '待处理', color: 'default' },
  'PROCESSING': { text: '处理中', color: 'processing' },
  'COMPLETED': { text: '已完成', color: 'success' },
  'FAILED': { text: '失败', color: 'error' }
}
