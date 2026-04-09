export interface RagRequest {
  query: string
  topK: number
  similarityThreshold?: number
  hybridSearch?: boolean
  rerankEnabled?: boolean
}

export interface RetrievalResult {
  documentId: string
  content: string
  metadata?: Record<string, unknown>
  rawScore: number
  rerankScore: number
  passedThreshold: boolean
  retrieverType: string
  chunkIndex: number
  totalChunks: number
}

export interface RagResponse {
  success: boolean
  errorMessage?: string
  retrievedDocuments: RetrievalResult[]
  fusedContext?: string
  retrievalTimeMs: number
  vectorRetrievalTimeMs: number
  keywordRetrievalTimeMs: number
  rerankTimeMs: number
}

export interface ChatRequest {
  message: string
  sessionId?: string
  stream?: boolean
  enableRag?: boolean
  topK?: number
  temperature?: number
}

export interface ChatHistoryResponse {
  sessionId: string
  messages: Array<{
    role: string
    content: string
    timestamp: string
  }>
  createTime: string
  updateTime: string
}

// 流式响应数据类型
export interface StreamingChunk {
  type?: 'content' | 'reference' | 'done' | 'error'
  content?: string
  finished?: boolean
  references?: RetrievalResult[]
  error?: string
}
