// 向量化任务相关类型
export type EmbeddingTaskStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

export interface EmbeddingTask {
  id: string
  documentId: number
  documentName: string
  status: EmbeddingTaskStatus
  totalChunks: number
  completedChunks: number
  failedChunks: number
  progress: number
  inputTokens: number
  outputTokens: number
  startTime?: string
  endTime?: string
  createTime: string
  errorMessage?: string
  retryCount: number
}

export interface EmbeddingRequest {
  text: string
  model?: string
  documentId?: number
  async?: boolean
}

export interface EmbeddingResponse {
  taskId: string
  text: string
  embedding?: number[]
  dimension?: number
  model: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  errorMessage?: string
  retryCount: number
}

export interface EmbeddingBatchRequest {
  texts: EmbeddingRequest[]
  model?: string
  async?: boolean
}

export interface EmbeddingBatchResponse {
  batchTaskId: string
  total: number
  successCount: number
  failedCount: number
  results: EmbeddingResponse[]
}

export const EMBEDDING_MODELS = [
  { value: 'text-embedding-3-small', label: 'text-embedding-3-small' },
  { value: 'text-embedding-v3', label: 'text-embedding-v3' }
]

export const EMBEDDING_STATUS_MAP: Record<string, { text: string; color: string }> = {
  PENDING: { text: '待处理', color: 'default' },
  PROCESSING: { text: '进行中', color: 'processing' },
  COMPLETED: { text: '已完成', color: 'success' },
  FAILED: { text: '失败', color: 'error' },
  CANCELLED: { text: '已取消', color: 'warning' }
}

// 兼容旧页面命名
export const EMBEDDING_TASK_STATUS_MAP = EMBEDDING_STATUS_MAP

// 批量提交请求
export interface BatchEmbeddingRequest {
  documentIds: number[]
  priority?: number
}

// 任务查询参数
export interface EmbeddingTaskQueryParams {
  pageNum: number
  pageSize: number
  status?: EmbeddingTaskStatus
  documentName?: string
  startDate?: string
  endDate?: string
}

// 任务统计
export interface EmbeddingTaskStats {
  totalTasks: number
  pendingTasks: number
  processingTasks: number
  completedTasks: number
  failedTasks: number
  avgProcessingTime: number
}
