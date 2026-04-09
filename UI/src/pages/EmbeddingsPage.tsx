import { useState } from 'react'
import { Button, Card, Form, Input, Select, Space, Typography } from 'antd'
import { useMutation } from '@tanstack/react-query'
import AsyncState from '@/components/common/AsyncState'
import { embeddingsApi } from '@/api/embeddings'
import type { EmbeddingRequest } from '@/types/embedding'

const modelOptions = [
  { value: 'text-embedding-3-small', label: 'text-embedding-3-small' },
  { value: 'text-embedding-v3', label: 'text-embedding-v3' }
]

export default function EmbeddingsPage() {
  const [form] = Form.useForm<EmbeddingRequest>()
  const [taskId, setTaskId] = useState('')

  const createMutation = useMutation({
    mutationFn: (payload: EmbeddingRequest) => embeddingsApi.embedText(payload),
    onSuccess: (res) => {
      setTaskId(res.data.taskId)
    }
  })

  const statusMutation = useMutation({
    mutationFn: (id: string) => embeddingsApi.getTaskStatus(id)
  })

  return (
    <Card>
      <Typography.Title level={3}>向量任务</Typography.Title>
      <Form form={form} layout="vertical" initialValues={{ model: 'text-embedding-3-small', async: false }}>
        <Form.Item name="text" label="文本" rules={[{ required: true, message: '请输入文本' }]}>
          <Input.TextArea rows={5} />
        </Form.Item>
        <Form.Item name="model" label="模型">
          <Select options={modelOptions} />
        </Form.Item>
        <Space>
          <Button
            type="primary"
            loading={createMutation.isPending}
            onClick={async () => {
              const values = await form.validateFields()
              createMutation.mutate(values)
            }}
          >
            创建向量任务
          </Button>
          <Button disabled={!taskId} onClick={() => statusMutation.mutate(taskId)} loading={statusMutation.isPending}>
            刷新任务状态
          </Button>
        </Space>
      </Form>

      <div style={{ marginTop: 24 }}>
        <Typography.Text>当前任务ID：{taskId || '-'}</Typography.Text>
      </div>

      {createMutation.isError || statusMutation.isError ? (
        <AsyncState
          error={(createMutation.error as Error)?.message || (statusMutation.error as Error)?.message || '请求失败'}
        />
      ) : null}

      {(createMutation.data || statusMutation.data) ? (
        <Card size="small" style={{ marginTop: 16 }}>
          <Typography.Paragraph>
            状态：{(statusMutation.data ?? createMutation.data)?.data.status}
          </Typography.Paragraph>
          <Typography.Paragraph>
            模型：{(statusMutation.data ?? createMutation.data)?.data.model}
          </Typography.Paragraph>
          <Typography.Paragraph>
            维度：{(statusMutation.data ?? createMutation.data)?.data.dimension ?? '-'}
          </Typography.Paragraph>
        </Card>
      ) : null}
    </Card>
  )
}
