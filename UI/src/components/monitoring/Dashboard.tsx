import { useMemo } from 'react'
import { Row, Col, Card, Statistic } from 'antd'
import { CheckCircleOutlined, ThunderboltOutlined } from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import dayjs from 'dayjs'
import { useQuery } from '@tanstack/react-query'
import { monitoringApi } from '@/api/monitoring'

interface MetricPoint {
  timestamp: string
  value: number
}

const toSeries = (data: unknown): MetricPoint[] => {
  if (!Array.isArray(data)) return []
  return data
    .map((item) => {
      if (Array.isArray(item) && item.length >= 2) {
        return { timestamp: String(item[0]), value: Number(item[1]) || 0 }
      }
      if (item && typeof item === 'object') {
        const raw = item as Record<string, unknown>
        return {
          timestamp: String(raw.timestamp ?? raw.time ?? ''),
          value: Number(raw.value ?? raw.qps ?? raw.latency ?? 0) || 0
        }
      }
      return null
    })
    .filter((item): item is MetricPoint => Boolean(item && item.timestamp))
}

const Dashboard = () => {
  const now = dayjs()
  const oneHourAgo = now.subtract(1, 'hour')

  const start = oneHourAgo.toISOString()
  const end = now.toISOString()

  const { data: healthData } = useQuery({
    queryKey: ['monitoring', 'health'],
    queryFn: () => monitoringApi.getServiceHealth(),
    refetchInterval: 30000
  })
  const { data: qpsData } = useQuery({
    queryKey: ['monitoring', 'qps', start, end],
    queryFn: () => monitoringApi.getQpsMetrics(start, end),
    refetchInterval: 15000
  })
  const { data: latencyData } = useQuery({
    queryKey: ['monitoring', 'latency', start, end],
    queryFn: () => monitoringApi.getLatencyMetrics(start, end),
    refetchInterval: 15000
  })
  const { data: tokenData } = useQuery({
    queryKey: ['monitoring', 'tokens', start, end],
    queryFn: () => monitoringApi.getTokenMetrics(start, end),
    refetchInterval: 30000
  })

  const qpsSeries = useMemo(
    () => toSeries((qpsData as Record<string, unknown> | undefined)?.points ?? qpsData),
    [qpsData]
  )
  const latencySeries = useMemo(
    () => toSeries((latencyData as Record<string, unknown> | undefined)?.points ?? latencyData),
    [latencyData]
  )
  const latestQps = qpsSeries[qpsSeries.length - 1]?.value ?? 0
  const avgLatency = latencySeries.length
    ? Math.round(latencySeries.reduce((acc, cur) => acc + cur.value, 0) / latencySeries.length)
    : 0
  const cacheHitRate = Number((tokenData as Record<string, unknown> | undefined)?.cache_hit_rate ?? 0)
  const healthStatus = String((healthData as Record<string, unknown> | undefined)?.status ?? '未知')

  const qpsOption = {
    xAxis: { type: 'category', data: qpsSeries.map((p) => dayjs(p.timestamp).format('HH:mm:ss')) },
    yAxis: { type: 'value', name: 'QPS' },
    series: [{ type: 'line', smooth: true, data: qpsSeries.map((p) => p.value), areaStyle: {} }]
  }

  const latencyOption = {
    xAxis: { type: 'category', data: latencySeries.map((p) => dayjs(p.timestamp).format('HH:mm:ss')) },
    yAxis: { type: 'value', name: 'ms' },
    series: [{ type: 'bar', data: latencySeries.map((p) => p.value) }]
  }

  return (
    <div className="space-y-6">
      <Row gutter={16}>
        <Col span={6}>
          <Card><Statistic title="服务状态" value={healthStatus} prefix={<CheckCircleOutlined style={{ color: healthStatus === 'UP' || healthStatus === '正常' ? '#52c41a' : '#faad14' }} />} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="QPS" value={latestQps} prefix={<ThunderboltOutlined />} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="平均延迟" value={avgLatency} suffix="ms" /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="缓存命中率" value={cacheHitRate} suffix="%" /></Card>
        </Col>
      </Row>
      <Row gutter={16}>
        <Col span={12}>
          <Card title="QPS 实时监控">
            <ReactECharts option={qpsOption} style={{ height: '300px' }} />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="延迟分布">
            <ReactECharts option={latencyOption} style={{ height: '300px' }} />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Dashboard
