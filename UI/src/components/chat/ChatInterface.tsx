import { useState } from 'react'
import { Card, Button, Input, Empty } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import ReactMarkdown from 'react-markdown'
import { v4 as uuidv4 } from 'uuid'
import { useChatStore } from '@/stores/chatStore'
import { useStreaming } from '@/hooks/useStreaming'

const { TextArea } = Input

const ChatInterface = () => {
  const [sessionId] = useState(uuidv4())
  const [input, setInput] = useState('')
  const { currentSession, addSession, setCurrentSession } = useChatStore()
  const { isStreaming, sendMessage } = useStreaming(sessionId)

  useState(() => {
    const session = { id: sessionId, title: '新对话', messages: [], created_at: new Date().toISOString(), updated_at: new Date().toISOString() }
    addSession(session)
    setCurrentSession(session)
  })

  const handleSend = () => {
    if (input.trim() && !isStreaming) {
      sendMessage(input.trim())
      setInput('')
    }
  }

  return (
    <div className="flex h-[calc(100vh-200px)] gap-4">
      <Card className="w-64" title="会话" extra={<Button type="text" icon={<PlusOutlined />} />}>
        <Empty description="暂无会话" />
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
