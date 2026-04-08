// 向量化请求
export interface EmbeddingRequest {
  text: string
  model?: string
}

// 向量化响应
export interface EmbeddingResponse {
  taskId: string
  text: string
  embedding?: number[]
  dimension?: number
  model: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  errorMessage?: string
  duration?: number
}

// 批量向量化请求
export interface EmbeddingBatchRequest {
  texts: EmbeddingRequest[]
  model?: string
}

// 批量向量化响应
export interface EmbeddingBatchResponse {
  batchTaskId: string
  total: number
  successCount: number
  failedCount: number
  results: EmbeddingResponse[]
}

// 支持的模型
export const EMBEDDING_MODELS = [
  { value: 'text-embedding-v3', label: '通义千问 text-embedding-v3' },
  { value: 'text-embedding-v2', label: '通义千问 text-embedding-v2' }
]

// 状态映射
export const EMBEDDING_STATUS_MAP: Record<string, { text: string; color: string }> = {
  'PENDING': { text: '待处理', color: 'default' },
  'PROCESSING': { text: '处理中', color: 'processing' },
  'COMPLETED': { text: '已完成', color: 'success' },
  'FAILED': { text: '失败', color: 'error' }
}
