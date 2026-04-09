import { useState } from 'react'
import { Button, Card, Input, List, Space, Typography, message } from 'antd'
import { useMutation } from '@tanstack/react-query'
import AsyncState from '@/components/common/AsyncState'
import { ragApi } from '@/api/rag'
import type { RagRequest } from '@/types/rag'

const { TextArea } = Input

export default function RagWorkbenchPage() {
  const [query, setQuery] = useState('')

  const queryMutation = useMutation({
    mutationFn: (payload: RagRequest) => ragApi.query(payload),
    onError: (err) => {
      message.error((err as Error).message)
    }
  })

  const docs = queryMutation.data?.data.retrievedDocuments ?? []

  return (
    <Card>
      <Typography.Title level={3}>RAG 工作台</Typography.Title>
      <Space direction="vertical" style={{ width: '100%' }} size={16}>
        <TextArea rows={4} placeholder="请输入问题" value={query} onChange={(e) => setQuery(e.target.value)} />
        <Button
          type="primary"
          loading={queryMutation.isPending}
          onClick={() => queryMutation.mutate({ query, topK: 5, hybridSearch: true, rerankEnabled: true })}
        >
          执行检索
        </Button>

        {queryMutation.isPending || queryMutation.isError || (queryMutation.isSuccess && docs.length === 0) ? (
          <AsyncState
            loading={queryMutation.isPending}
            error={queryMutation.isError ? (queryMutation.error as Error).message : null}
            empty={queryMutation.isSuccess && docs.length === 0}
            emptyDescription="未检索到相关文档"
          />
        ) : null}

        {docs.length > 0 ? (
          <List
            dataSource={docs}
            renderItem={(item) => (
              <List.Item>
                <List.Item.Meta
                  title={`文档 ${item.documentId} | 评分 ${item.rerankScore.toFixed(4)}`}
                  description={item.content}
                />
              </List.Item>
            )}
          />
        ) : null}
      </Space>
    </Card>
  )
}
