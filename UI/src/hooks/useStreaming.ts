// 流式响应 Hook - 使用 fetch + ReadableStream 实现 POST 请求的 SSE
import { useState, useCallback, useRef } from 'react'
import { ragApi } from '@/api/rag'
import { useChatStore } from '@/stores/chatStore'
import type { ChatRequest, StreamingChunk, RetrievalResult } from '@/types/rag'
import type { Reference } from '@/types/chat'

export const useStreaming = (sessionId: string) => {
  const [isStreaming, setIsStreaming] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const abortControllerRef = useRef<AbortController | null>(null)
  const { addMessage, upsertMessage } = useChatStore()

  const sendMessage = useCallback((content: string, options?: Partial<ChatRequest>) => {
    if (isStreaming) return

    // 创建用户消息
    const userMessage = {
      id: `msg-${Date.now()}-user`,
      role: 'user' as const,
      content,
      timestamp: new Date().toISOString()
    }
    addMessage(sessionId, userMessage)

    // 创建助手消息占位
    const assistantMessageId = `msg-${Date.now()}-assistant`
    addMessage(sessionId, {
      id: assistantMessageId,
      role: 'assistant',
      content: '',
      timestamp: new Date().toISOString()
    })

    setIsStreaming(true)
    setError(null)

    // 创建 AbortController
    abortControllerRef.current = new AbortController()

    let fullContent = ''
    let retrievedRefs: RetrievalResult[] = []

    // 执行流式请求
    const request: ChatRequest = {
      message: content,
      sessionId,
      stream: true,
      enableRag: options?.enableRag ?? true,
      topK: options?.topK ?? 5,
      temperature: options?.temperature ?? 0.7,
      ...options
    }

    ragApi.streamChat(
      request,
      // onChunk
      (chunk: StreamingChunk) => {
        if (chunk.type === 'content' || chunk.content) {
          fullContent += chunk.content
          upsertMessage(sessionId, {
            id: assistantMessageId,
            role: 'assistant',
            content: fullContent,
            timestamp: new Date().toISOString()
          })
        }
        
        if (chunk.references) {
          retrievedRefs = chunk.references
        }
      },
      // onDone
      () => {
        // 将 RetrievalResult 转换为 Reference
        const references: Reference[] = retrievedRefs.map(ref => ({
          documentId: ref.documentId,
          content: ref.content,
          score: ref.rerankScore || ref.rawScore || 0,
          metadata: ref.metadata
        }))
        
        upsertMessage(sessionId, {
          id: assistantMessageId,
          role: 'assistant',
          content: fullContent,
          timestamp: new Date().toISOString(),
          finished: true,
          references
        })
        setIsStreaming(false)
        abortControllerRef.current = null
      },
      // onError
      (err: Error) => {
        setError(err.message)
        upsertMessage(sessionId, {
          id: assistantMessageId,
          role: 'assistant',
          content: `错误: ${err.message}`,
          timestamp: new Date().toISOString(),
          finished: true
        })
        setIsStreaming(false)
        abortControllerRef.current = null
      },
      abortControllerRef.current.signal
    )
  }, [sessionId, isStreaming, addMessage, upsertMessage])

  const stopStreaming = useCallback(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort()
      setIsStreaming(false)
    }
  }, [])

  return { 
    isStreaming, 
    error,
    sendMessage, 
    stopStreaming 
  }
}
