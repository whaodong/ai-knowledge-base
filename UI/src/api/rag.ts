import apiClient from './client'
import type { Result } from '@/types/api'
import type { ChatRequest, RagRequest, RagResponse, ChatHistory } from '@/types/chat'

export const ragApi = {
  // RAG查询
  query: (request: RagRequest): Promise<Result<RagResponse>> =>
    apiClient.post('/api/v1/rag/query', request),

  // 获取流式对话URL
  getStreamingUrl: (): string => {
    const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
    return `${baseUrl}/api/v1/rag/chat`
  },

  // 获取会话历史
  getHistory: (sessionId: string): Promise<Result<ChatHistory>> =>
    apiClient.get(`/api/v1/rag/history/${sessionId}`),

  // 健康检查
  health: (): Promise<Record<string, unknown>> =>
    apiClient.get('/api/v1/rag/health')
}
