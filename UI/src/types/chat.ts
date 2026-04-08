// 对话消息
export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: string
  references?: Reference[]
  finished?: boolean
}

// 引用来源（匹配后端 RetrievalResult）
export interface Reference {
  documentId: string
  content: string
  score: number
  metadata?: Record<string, unknown>
}

// 对话请求（匹配后端 ChatRequest）
export interface ChatRequest {
  message: string
  sessionId?: string
}

// 对话响应（匹配后端 ChatResponse）
export interface ChatResponse {
  sessionId: string
  reply: string
  finished: boolean
  timestamp: string
  references?: Reference[]
}

// 对话历史（匹配后端 ChatHistoryResponse）
export interface ChatHistory {
  sessionId: string
  messages: ChatHistoryMessage[]
  createdAt: string
  updatedAt: string
}

export interface ChatHistoryMessage {
  role: 'user' | 'assistant'
  content: string
  timestamp: string
}

// RAG查询请求（匹配后端 RagRequest）
export interface RagRequest {
  query: string
  topK?: number
  hybridSearch?: boolean
  rerankEnabled?: boolean
  similarityThreshold?: number
}

// RAG查询响应（匹配后端 RagResponse）
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

// 检索结果（匹配后端 RetrievalResult）
export interface RetrievalResult {
  documentId: string
  content: string
  score: number
  metadata?: Record<string, unknown>
}

// SSE事件类型
export enum SSEEventType {
  MESSAGE = 'message',
  DONE = 'done',
  ERROR = 'error'
}
