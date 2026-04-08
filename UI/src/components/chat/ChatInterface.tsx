import { useEffect, useMemo, useState } from 'react'
import { Card, Button, Input, Empty, List } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import ReactMarkdown from 'react-markdown'
import { v4 as uuidv4 } from 'uuid'
import { useChatStore } from '@/stores/chatStore'
import { useStreaming } from '@/hooks/useStreaming'

const { TextArea } = Input

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
      <Card className="w-64" title="会话" extra={<Button type="text" icon={<PlusOutlined />} onClick={handleCreateSession} />}>
        {sortedSessions.length === 0 ? (
          <Empty description="暂无会话" />
        ) : (
          <List
            size="small"
            dataSource={sortedSessions}
            renderItem={(item) => (
              <List.Item className={item.id === sessionId ? 'bg-gray-100 rounded px-2' : 'px-2'}>
                <button type="button" className="w-full text-left" onClick={() => handleSelectSession(item.id)}>
                  {item.title}
                </button>
              </List.Item>
            )}
          />
        )}
      </Card>
      <Card className="flex-1 flex flex-col">
        <div className="flex-1 overflow-auto mb-4">
          {currentSession?.messages.length === 0 ? (
            <Empty description="开始提问吧" className="mt-20" />
          ) : (
            currentSession?.messages.map((msg) => (
              <div key={msg.id} className={`mb-4 ${msg.role === 'user' ? 'text-right' : 'text-left'}`}>
                <div className={`inline-block max-w-[80%] p-3 rounded-lg ${msg.role === 'user' ? 'bg-blue-500 text-white' : 'bg-gray-100'}`}>
                  <ReactMarkdown>{msg.content || '思考中...'}</ReactMarkdown>
                </div>
              </div>
            ))
          )}
        </div>
        <div className="flex gap-2">
          <TextArea value={input} onChange={(e) => setInput(e.target.value)} placeholder="输入问题" autoSize={{ minRows: 2, maxRows: 4 }} />
          <Button type="primary" onClick={handleSend} loading={isStreaming}>发送</Button>
        </div>
      </Card>
    </div>
  )
}

export default ChatInterface
