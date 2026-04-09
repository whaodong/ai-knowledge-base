import { useEffect, useMemo, useState } from 'react'
import ReactMarkdown from 'react-markdown'
import { v4 as uuidv4 } from 'uuid'
import { useChatStore } from '@/stores/chatStore'
import { useStreaming } from '@/hooks/useStreaming'
import AsyncState from '@/components/common/AsyncState'

const ChatInterface = () => {
  const [sessionId, setSessionId] = useState(uuidv4())
  const [input, setInput] = useState('')
  const { sessions, currentSession, addSession, setCurrentSession } = useChatStore()
  const { isStreaming, sendMessage } = useStreaming(sessionId)

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

  const sortedSessions = useMemo(
    () => [...sessions].sort((a, b) => new Date(b.updated_at).getTime() - new Date(a.updated_at).getTime()),
    [sessions]
  )

  const handleSend = () => {
    if (input.trim() && !isStreaming) {
      sendMessage(input.trim())
      setInput('')
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
                    w-full text-left px-3 py-2 rounded-md text-sm transition-colors
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
        {/* Messages */}
        <div className="flex-1 overflow-auto p-4 space-y-4">
          {currentSession?.messages.length === 0 ? (
            <AsyncState empty emptyDescription="开始提问吧" />
          ) : (
            currentSession?.messages.map((msg) => (
              <div key={msg.id} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div
                  className={`
                    max-w-[80%] p-3 rounded-lg
                    ${msg.role === 'user'
                      ? 'bg-blue-500 text-white'
                      : 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-200'
                    }
                  `}
                >
                  {msg.role === 'assistant' ? (
                    <div className="prose prose-sm dark:prose-invert max-w-none">
                      <ReactMarkdown>
                        {msg.content || '思考中...'}
                      </ReactMarkdown>
                    </div>
                  ) : (
                    <p className="whitespace-pre-wrap">{msg.content}</p>
                  )}
                </div>
              </div>
            ))
          )}
        </div>

        {/* Input area */}
        <div className="p-4 border-t border-gray-200 dark:border-gray-700">
          <div className="flex gap-2">
            <textarea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="输入问题"
              rows={2}
              className="flex-1 px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg 
                         bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-100
                         focus:ring-2 focus:ring-blue-500 focus:border-transparent
                         resize-none"
              onKeyDown={(e) => {
                if (e.key === 'Enter' && !e.shiftKey) {
                  e.preventDefault()
                  handleSend()
                }
              }}
            />
            <button
              onClick={handleSend}
              disabled={isStreaming || !input.trim()}
              className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 
                         disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              {isStreaming ? (
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
              ) : (
                '发送'
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

export default ChatInterface
