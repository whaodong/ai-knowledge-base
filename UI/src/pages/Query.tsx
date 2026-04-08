import { useState, useRef, useEffect } from 'react'
import { Card, Input, Button, Space, Spin, Empty, Typography, message, Drawer, List } from 'antd'
import { SendOutlined, HistoryOutlined, ClearOutlined } from '@ant-design/icons'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import rehypeHighlight from 'rehype-highlight'
import 'highlight.js/styles/github-dark.css'
import type { Message, Reference } from '@/types/chat'
import { useChatStore } from '@/stores/chatStore'
import dayjs from 'dayjs'
import { v4 as uuidv4 } from 'uuid'

const { TextArea } = Input
const { Text, Paragraph } = Typography

const Query = () => {
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [historyVisible, setHistoryVisible] = useState(false)
  const messagesEndRef = useRef<HTMLDivElement>(null)
  const eventSourceRef = useRef<EventSource | null>(null)
  
  const { 
    messages, 
    addMessage, 
    updateLastMessage, 
    clearMessages,
    sessionId,
    setSessionId 
  } = useChatStore()

  // 滚动到底部
  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    scrollToBottom()
  }, [messages])

  // 发送消息
  const sendMessage = async () => {
    if (!input.trim() || loading) return

    const userMessage: Message = {
      id: uuidv4(),
      role: 'user',
      content: input.trim(),
      timestamp: dayjs().toISOString()
    }
    addMessage(userMessage)
    setInput('')
    setLoading(true)

    // 创建AI消息占位
    const aiMessage: Message = {
      id: uuidv4(),
      role: 'assistant',
      content: '',
      timestamp: dayjs().toISOString(),
      finished: false
    }
    addMessage(aiMessage)

    try {
      const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
      const url = `${baseUrl}/api/v1/rag/chat`
      
      // 使用 fetch 发送 POST 请求，接收 SSE 响应
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('accessToken')}`
        },
        body: JSON.stringify({
          message: userMessage.content,
          sessionId: sessionId || undefined
        })
      })

      if (!response.ok) {
        throw new Error('请求失败')
      }

      const reader = response.body?.getReader()
      const decoder = new TextDecoder()

      if (!reader) {
        throw new Error('无法读取响应')
      }

      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            try {
              const data = JSON.parse(line.slice(5))
              if (data.reply) {
                updateLastMessage(data.reply, data.finished, data.references)
                if (data.sessionId) {
                  setSessionId(data.sessionId)
                }
              }
            } catch {
              // 忽略解析错误
            }
          }
        }
      }
    } catch (error) {
      message.error('发送消息失败')
      updateLastMessage('抱歉，发生了错误，请稍后重试。', true)
    } finally {
      setLoading(false)
    }
  }

  // 清空对话
  const handleClear = () => {
    clearMessages()
    setSessionId(null)
  }

  // 渲染引用来源
  const renderReferences = (references?: Reference[]) => {
    if (!references || references.length === 0) return null
    
    return (
      <div className="mt-4 p-3 bg-gray-50 rounded-lg">
        <Text type="secondary" className="text-xs">引用来源：</Text>
        <List
          size="small"
          dataSource={references}
          renderItem={(ref, index) => (
            <List.Item className="text-sm">
              <Text ellipsis={{ rows: 2 }} style={{ maxWidth: '100%' }}>
                [{index + 1}] {ref.content} (相关度: {(ref.score * 100).toFixed(1)}%)
              </Text>
            </List.Item>
          )}
        />
      </div>
    )
  }

  return (
    <div className="h-full flex">
      {/* 对话区域 */}
      <Card className="flex-1 flex flex-col" bodyStyle={{ flex: 1, display: 'flex', flexDirection: 'column', padding: 0 }}>
        {/* 消息列表 */}
        <div className="flex-1 overflow-auto p-4 space-y-4">
          {messages.length === 0 ? (
            <Empty description="开始对话吧" className="mt-20" />
          ) : (
            messages.map((msg) => (
              <div key={msg.id} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
                <div className={`max-w-[80%] ${msg.role === 'user' ? 'bg-blue-500 text-white' : 'bg-gray-100'} rounded-lg p-3`}>
                  {msg.role === 'user' ? (
                    <Text className="text-white">{msg.content}</Text>
                  ) : (
                    <div className="prose prose-sm max-w-none">
                      <ReactMarkdown
                        remarkPlugins={[remarkGfm]}
                        rehypePlugins={[rehypeHighlight]}
                      >
                        {msg.content || <Spin size="small" />}
                      </ReactMarkdown>
                      {msg.finished && renderReferences(msg.references)}
                    </div>
                  )}
                  <Text type="secondary" className="text-xs mt-1 block">
                    {dayjs(msg.timestamp).format('HH:mm')}
                  </Text>
                </div>
              </div>
            ))
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* 输入区域 */}
        <div className="border-t p-4">
          <Space.Compact style={{ width: '100%' }}>
            <TextArea
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="输入您的问题..."
              autoSize={{ minRows: 1, maxRows: 4 }}
              onPressEnter={(e) => {
                if (!e.shiftKey) {
                  e.preventDefault()
                  sendMessage()
                }
              }}
              style={{ borderRadius: '8px 0 0 8px' }}
            />
            <Button type="primary" icon={<SendOutlined />} onClick={sendMessage} loading={loading}>
              发送
            </Button>
          </Space.Compact>
        </div>
      </Card>

      {/* 右侧工具栏 */}
      <div className="w-12 flex flex-col items-center py-4 space-y-4 border-l bg-white">
        <Button type="text" icon={<HistoryOutlined />} onClick={() => setHistoryVisible(true)} title="历史记录" />
        <Button type="text" icon={<ClearOutlined />} onClick={handleClear} title="清空对话" />
      </div>

      {/* 历史记录抽屉 */}
      <Drawer
        title="对话历史"
        placement="right"
        open={historyVisible}
        onClose={() => setHistoryVisible(false)}
      >
        <Empty description="暂无历史记录" />
      </Drawer>
    </div>
  )
}

export default Query
