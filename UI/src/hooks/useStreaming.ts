import { useState, useCallback, useRef } from 'react'
import { ragApi } from '@/api/rag'
import { useChatStore } from '@/stores/chatStore'

export const useStreaming = (sessionId: string) => {
  const [isStreaming, setIsStreaming] = useState(false)
  const eventSourceRef = useRef<EventSource | null>(null)
  const { addMessage, upsertMessage } = useChatStore()

  const sendMessage = useCallback((content: string) => {
    if (isStreaming) return

    const userMessage = {
      id: `msg-${Date.now()}-user`,
      role: 'user' as const,
      content,
      timestamp: new Date().toISOString()
    }
    addMessage(sessionId, userMessage)

    const assistantMessageId = `msg-${Date.now()}-assistant`
    addMessage(sessionId, {
      id: assistantMessageId,
      role: 'assistant',
      content: '',
      timestamp: new Date().toISOString()
    })

    setIsStreaming(true)
    const url = ragApi.getStreamingUrl(content)
    const eventSource = new EventSource(url)
    eventSourceRef.current = eventSource

    let fullContent = ''

    eventSource.onmessage = (event) => {
      if (event.data === '[DONE]') {
        eventSource.close()
        setIsStreaming(false)
        return
      }
      fullContent += event.data
      upsertMessage(sessionId, {
        id: assistantMessageId,
        role: 'assistant',
        content: fullContent,
        timestamp: new Date().toISOString()
      })
    }

    eventSource.onerror = () => {
      eventSource.close()
      setIsStreaming(false)
    }
  }, [sessionId, isStreaming, addMessage, upsertMessage])

  const stopStreaming = useCallback(() => {
    eventSourceRef.current?.close()
    setIsStreaming(false)
  }, [])

  return { isStreaming, sendMessage, stopStreaming }
}
