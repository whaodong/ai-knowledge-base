// 增强版聊天界面组件 - 支持打字机效果、引用来源、停止生成
import { useEffect, useMemo, useState, useRef } from 'react'
import ReactMarkdown from 'react-markdown'
import { v4 as uuidv4 } from 'uuid'
import { useChatStore } from '@/stores/chatStore'
import { useStreaming } from '@/hooks/useStreaming'
import AsyncState from '@/components/common/AsyncState'
import type { Reference } from '@/types/chat'

// 打字机效果组件
const TypewriterText = ({ content, speed = 20 }: { content: string; speed?: number }) => {
  const [displayedContent, setDisplayedContent] = useState('')
  const [isTyping, setIsTyping] = useState(true)
  const containerRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    setDisplayedContent('')
    setIsTyping(true)
    
    let index = 0
    const interval = setInterval(() => {
      if (index < content.length) {
        setDisplayedContent(content.slice(0, index + 1))
        index++
        
        // 自动滚动到底部
        if (containerRef.current) {
          containerRef.current.scrollTop = containerRef.current.scrollHeight
        }
      } else {
        setIsTyping(false)
        clearInterval(interval)
      }
    }, speed)

    return () => clearInterval(interval)
  }, [content, speed])

  return (
    <div ref={containerRef} className="relative">
      <div className="prose prose-sm dark:prose-invert max-w-none">
        <ReactMarkdown>
          {displayedContent || '思考中...'}
        </ReactMarkdown>
      </div>
      {isTyping && (
        <span className="inline-block w-2 h-4 ml-1 bg-blue-500 animate-pulse" />
      )}
    </div>
  )
}

// 引用来源卡片组件
const ReferenceCard = ({ 
  reference, 
  onClick 
}: { 
  reference: Reference
  onClick?: () => void 
}) => {
  const scoreColor = reference.score >= 0.8 
    ? 'text-green-500' 
    : reference.score >= 0.5 
    ? 'text-yellow-500' 
    : 'text-gray-500'

  return (
    <div 
      onClick={onClick}
      className={`p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg border border-gray-200 dark:border-gray-600 
                  ${onClick ? 'cursor-pointer hover:bg-gray-100 dark:hover:bg-gray-700' : ''}`}
    >
      <div className="flex items-center justify-between mb-2">
        <span className="text-xs font-medium text-gray-500 dark:text-gray-400">
          文档 {reference.documentId}
        </span>
        <span className={`text-xs font-medium ${scoreColor}`}>
          相似度: {(reference.score * 100).toFixed(1)}%
        </span>
      </div>
      <p className="text-sm text-gray-600 dark:text-gray-400 line-clamp-3">
        {reference.content}
      </p>
    </div>
  )
}

// 消息气泡组件
const MessageBubble = ({ 
  message, 
  onViewReference 
}: { 
  message: { role: string; content: string; references?: Reference[]; finished?: boolean }
  onViewReference?: (ref: Reference) => void
}) => {
  const isUser = message.role === 'user'
  
  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`
          max-w-[85%] p-4 rounded-2xl
          ${isUser
            ? 'bg-blue-500 text-white rounded-br-sm'
            : 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200 rounded-bl-sm'
          }
        `}
      >
        {isUser ? (
          <p className="whitespace-pre-wrap">{message.content}</p>
        ) : (
          <>
            {/* 打字机效果 */}
            {!message.finished ? (
              <TypewriterText content={message.content} speed={15} />
            ) : (
              <div className="prose prose-sm dark:prose-invert max-w-none">
                <ReactMarkdown>
                  {message.content || '回答已生成'}
                </ReactMarkdown>
              </div>
            )}
            
            {/* 引用来源 */}
            {message.references && message.references.length > 0 && message.finished && (
              <div className="mt-4 pt-3 border-t border-gray-200 dark:border-gray-600">
                <p className="text-xs font-medium text-gray-500 dark:text-gray-400 mb-2">
                  引用来源 ({message.references.length})
                </p>
                <div className="space-y-2">
                  {message.references.slice(0, 3).map((ref, idx) => (
                    <ReferenceCard 
                      key={idx} 
                      reference={ref}
                      onClick={() => onViewReference?.(ref)}
                    />
                  ))}
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  )
}

const ChatInterface = () => {
  const [sessionId, setSessionId] = useState(uuidv4())
  const [input, setInput] = useState('')
  const [enableRag, setEnableRag] = useState(true)
  const [showSettings, setShowSettings] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  
  const { sessions, currentSession, addSession, setCurrentSession } = useChatStore()
  const { isStreaming, sendMessage, stopStreaming, error } = useStreaming(sessionId)

  // 初始化会话
  useEffect(() => {
    const existing = sessions.find((s) => s.id === sessionId)
    if (existing) {
      setCurrentSession(existing)
      return
    }
    const now = new Date().toISOString()
    const session = { id: sessionId, title: '新对话', messages: [], created_at: now, updated_at: now }
    addSession(session)
    setCurrentSession(session)
  }, [addSession, sessionId, sessions, setCurrentSession])

  // 滚动到底部
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [currentSession?.messages])

  // 按时间排序会话
  const sortedSessions = useMemo(
    () => [...sessions].sort((a, b) => new Date(b.updated_at).getTime() - new Date(a.updated_at).getTime()),
    [sessions]
  )

  const handleSend = () => {
    if (input.trim() && !isStreaming) {
      sendMessage(input.trim(), { enableRag })
      setInput('')
    }
  }

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  const handleCreateSession = () => {
    if (isStreaming) return
    setSessionId(uuidv4())
    setInput('')
  }

  const handleSelectSession = (id: string) => {
    if (isStreaming) return
    const next = sessions.find((s) => s.id === id)
    if (!next) return
    setSessionId(next.id)
    setCurrentSession(next)
  }

  const handleViewReference = (ref: Reference) => {
    // 可以在此处打开文档详情弹窗
    console.log('View reference:', ref)
  }

  return (
    <div className="flex h-[calc(100vh-200px)] gap-4">
      {/* Session list */}
      <div className="w-64 bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700 overflow-hidden flex flex-col">
        <div className="p-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
          <h3 className="font-medium text-gray-800 dark:text-white">会话</h3>
          <button
            onClick={handleCreateSession}
            disabled={isStreaming}
            className="p-1.5 rounded-md text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed"
            title="新建会话"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
          </button>
        </div>
        <div className="flex-1 overflow-auto">
          {sortedSessions.length === 0 ? (
            <div className="p-4 text-center text-sm text-gray-500 dark:text-gray-400">
              暂无会话
            </div>
          ) : (
            <div className="p-2 space-y-1">
              {sortedSessions.map((item) => (
                <button
                  key={item.id}
                  onClick={() => handleSelectSession(item.id)}
                  className={`
                    w-full text-left px-3 py-2 rounded-md text-sm transition-colors truncate
                    ${item.id === sessionId
                      ? 'bg-blue-50 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400'
                      : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-700'
                    }
                  `}
                >
                  {item.title}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Chat area */}
      <div className="flex-1 bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700 overflow-hidden flex flex-col">
        {/* Header */}
        <div className="px-4 py-3 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <svg className="w-5 h-5 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
            </svg>
            <span className="font-medium text-gray-800 dark:text-white">RAG 对话</span>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowSettings(!showSettings)}
              className="p-2 text-gray-500 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-lg"
              title="设置"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
            </button>
          </div>
        </div>

        {/* Settings panel */}
        {showSettings && (
          <div className="px-4 py-3 bg-gray-50 dark:bg-gray-700/50 border-b border-gray-200 dark:border-gray-700">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={enableRag}
                onChange={(e) => setEnableRag(e.target.checked)}
                className="w-4 h-4 text-blue-500 border-gray-300 rounded focus:ring-blue-500"
              />
              <span className="text-sm text-gray-700 dark:text-gray-300">启用 RAG 检索</span>
            </label>
          </div>
        )}

        {/* Error message */}
        {error && (
          <div className="px-4 py-2 bg-red-50 dark:bg-red-900/20 border-b border-red-200 dark:border-red-800">
            <p className="text-sm text-red-600 dark:text-red-400">{error}</p>
          </div>
        )}

        {/* Messages */}
        <div className="flex-1 overflow-auto p-4 space-y-4">
          {currentSession?.messages.length === 0 ? (
            <AsyncState empty emptyDescription="开始提问吧" />
          ) : (
            <>
              {currentSession?.messages.map((msg) => (
                <MessageBubble 
                  key={msg.id} 
                  message={msg}
                  onViewReference={handleViewReference}
                />
              ))}
              <div ref={messagesEndRef} />
            </>
          )}
        </div>

        {/* Input area */}
        <div className="p-4 border-t border-gray-200 dark:border-gray-700">
          <div className="flex gap-2">
            <textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="输入问题，按 Enter 发送"
              rows={2}
              className="flex-1 px-4 py-3 border border-gray-300 dark:border-gray-600 rounded-xl 
                         bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100
                         focus:ring-2 focus:ring-blue-500 focus:border-transparent
                         resize-none placeholder-gray-400"
              disabled={isStreaming}
            />
            <div className="flex flex-col gap-2">
              {isStreaming ? (
                <button
                  onClick={stopStreaming}
                  className="px-4 py-2 bg-red-500 text-white rounded-xl hover:bg-red-600 transition-colors flex items-center justify-center gap-2"
                  title="停止生成"
                >
                  <svg className="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                    <rect x="6" y="6" width="12" height="12" rx="2" />
                  </svg>
                </button>
              ) : (
                <button
                  onClick={handleSend}
                  disabled={!input.trim()}
                  className="px-4 py-2 bg-blue-500 text-white rounded-xl hover:bg-blue-600 
                             disabled:opacity-50 disabled:cursor-not-allowed transition-colors flex items-center justify-center gap-2"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                  </svg>
                </button>
              )}
            </div>
          </div>
          <p className="mt-2 text-xs text-gray-400 dark:text-gray-500">
            按 Shift + Enter 换行
          </p>
        </div>
      </div>
    </div>
  )
}

export default ChatInterface
