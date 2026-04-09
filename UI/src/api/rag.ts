import apiClient from './client'
import type { Result } from '@/types/api'
import type { RagRequest, RagResponse, ChatHistoryResponse } from '@/types/rag'

export const ragApi = {
  // RAG查询
  query: (request: RagRequest): Promise<Result<RagResponse>> =>
    apiClient.post('/api/v1/rag/query', request),

  // 获取流式对话URL
  getStreamingUrl: (_content?: string): string => {
    const rawApiBaseURL = import.meta.env.VITE_API_URL
    const isLocalGateway =
      rawApiBaseURL === 'http://localhost:8080' ||
      rawApiBaseURL === 'http://127.0.0.1:8080'
    const baseUrl = import.meta.env.DEV && (!rawApiBaseURL || isLocalGateway)
      ? ''
      : (rawApiBaseURL || '')
    return `${baseUrl}/api/v1/rag/chat`
  },

  // 获取会话历史
  getHistory: (sessionId: string): Promise<Result<ChatHistoryResponse>> =>
    apiClient.get(`/api/v1/rag/history/${sessionId}`),

  // 健康检查
  health: (): Promise<Record<string, unknown>> =>
    apiClient.get('/api/v1/rag/health')
}
