// RAG API 层
import apiClient from './client'
import type { Result } from '@/types/api'
import type { 
  RagRequest, 
  RagResponse, 
  ChatHistoryResponse,
  ChatRequest,
  StreamingChunk
} from '@/types/rag'

// 获取 API 基础 URL
const getApiBaseUrl = (): string => {
  const rawApiBaseURL = import.meta.env.VITE_API_URL
  const isLocalGateway =
    rawApiBaseURL === 'http://localhost:8080' ||
    rawApiBaseURL === 'http://127.0.0.1:8080'
  const baseUrl = import.meta.env.DEV && (!rawApiBaseURL || isLocalGateway)
    ? ''
    : (rawApiBaseURL || '')
  return baseUrl
}

export const ragApi = {
  // RAG查询（非流式）
  query: (request: RagRequest): Promise<Result<RagResponse>> =>
    apiClient.post('/api/v1/rag/query', request),

  // 获取流式对话 URL
  getStreamingUrl: (): string => {
    return `${getApiBaseUrl()}/api/v1/rag/chat`
  },

  // 流式对话 - 使用 fetch + ReadableStream
  streamChat: async (
    request: ChatRequest,
    onChunk: (chunk: StreamingChunk) => void,
    onDone: () => void,
    onError: (error: Error) => void,
    signal?: AbortSignal
  ): Promise<void> => {
    try {
      const response = await fetch(`${getApiBaseUrl()}/api/v1/rag/chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
        signal,
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const reader = response.body?.getReader()
      if (!reader) {
        throw new Error('Failed to get reader')
      }

      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        
        if (done) {
          onDone()
          break
        }

        buffer += decoder.decode(value, { stream: true })
        
        // 处理 SSE 事件
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6).trim()
            
            if (data === '[DONE]') {
              onDone()
              return
            }

            try {
              const chunk = JSON.parse(data) as StreamingChunk
              onChunk(chunk)
            } catch {
              // 如果不是 JSON，可能是纯文本
              if (data) {
                onChunk({
                  type: 'content',
                  content: data,
                  finished: false
                })
              }
            }
          }
        }
      }
    } catch (error) {
      if ((error as Error).name === 'AbortError') {
        onDone()
      } else {
        onError(error as Error)
      }
    }
  },

  // 获取会话历史
  getHistory: (sessionId: string): Promise<Result<ChatHistoryResponse>> =>
    apiClient.get(`/api/v1/rag/history/${sessionId}`),

  // 获取知识库统计
  getStats: (): Promise<Result<{
    totalDocuments: number
    totalVectors: number
    totalTokens: number
    monthlyTokens: number
    storageUsed: number
    storageTotal: number
  }>> =>
    apiClient.get('/api/v1/rag/stats'),

  // 健康检查
  health: (): Promise<Record<string, unknown>> =>
    apiClient.get('/api/v1/rag/health')
}
