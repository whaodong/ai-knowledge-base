import { Alert, Empty, Spin } from 'antd'

interface AsyncStateProps {
  loading?: boolean
  error?: string | null
  empty?: boolean
  emptyDescription?: string
}

export default function AsyncState({
  loading = false,
  error = null,
  empty = false,
  emptyDescription = '暂无数据'
}: AsyncStateProps) {
  if (loading) {
    return <Spin size="large" />
  }

  if (error) {
    return <Alert type="error" showIcon message="请求失败" description={error} />
  }

  if (empty) {
    return <Empty description={emptyDescription} />
  }

  return null
}
