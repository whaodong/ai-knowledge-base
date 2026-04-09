import { Card, Col, Row, Typography } from 'antd'
import { useQuery } from '@tanstack/react-query'
import AsyncState from '@/components/common/AsyncState'
import { ragApi } from '@/api/rag'
import { embeddingsApi } from '@/api/embeddings'
import { documentsApi } from '@/api/documents'

type ServiceStatus = {
  name: string
  status: string
}

async function fetchStatuses(): Promise<ServiceStatus[]> {
  const [rag, embedding, docs] = await Promise.allSettled([
    ragApi.health(),
    embeddingsApi.health(),
    documentsApi.health()
  ])

  return [
    { name: 'rag-service', status: rag.status === 'fulfilled' ? 'UP' : 'DOWN' },
    { name: 'embedding-service', status: embedding.status === 'fulfilled' ? 'UP' : 'DOWN' },
    { name: 'document-service', status: docs.status === 'fulfilled' ? 'UP' : 'DOWN' }
  ]
}

export default function DashboardPage() {
  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['dashboard-status'],
    queryFn: fetchStatuses
  })

  if (isLoading || isError || !data?.length) {
    return (
      <AsyncState
        loading={isLoading}
        error={isError ? (error as Error).message : null}
        empty={!isLoading && !isError}
        emptyDescription="暂无服务状态"
      />
    )
  }

  return (
    <div>
      <Typography.Title level={3}>仪表盘</Typography.Title>
      <Row gutter={16}>
        {data.map((item) => (
          <Col key={item.name} xs={24} md={8}>
            <Card title={item.name}>
              <Typography.Text type={item.status === 'UP' ? 'success' : 'danger'}>
                {item.status}
              </Typography.Text>
            </Card>
          </Col>
        ))}
      </Row>
    </div>
  )
}
