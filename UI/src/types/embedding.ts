// 向量化任务相关类型

// 任务状态
export type EmbeddingTaskStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'CANCELLED'

// 向量化任务
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

// 任务状态映射
export const EMBEDDING_TASK_STATUS_MAP: Record<string, { text: string; color: string }> = {
  'PENDING': { text: '待处理', color: 'default' },
  'PROCESSING': { text: '进行中', color: 'processing' },
  'COMPLETED': { text: '已完成', color: 'success' },
  'FAILED': { text: '失败', color: 'error' },
  'CANCELLED': { text: '已取消', color: 'warning' }
}

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
