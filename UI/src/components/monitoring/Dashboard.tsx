import { Row, Col, Card, Statistic } from 'antd'
import { CheckCircleOutlined, ThunderboltOutlined } from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import dayjs from 'dayjs'

const Dashboard = () => {
  const now = dayjs()
  const oneHourAgo = now.subtract(1, 'hour')

  const qpsOption = {
    xAxis: { type: 'category', data: ['10:00', '10:10', '10:20', '10:30', '10:40', '10:50'] },
    yAxis: { type: 'value', name: 'QPS' },
    series: [{ type: 'line', smooth: true, data: [120, 132, 101, 134, 90, 230], areaStyle: {} }]
  }

  const latencyOption = {
    xAxis: { type: 'category', data: ['P50', 'P90', 'P99'] },
    yAxis: { type: 'value', name: 'ms' },
    series: [{ type: 'bar', data: [120, 350, 890] }]
  }

  return (
    <div className="space-y-6">
      <Row gutter={16}>
        <Col span={6}>
          <Card><Statistic title="服务状态" value="正常" prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="QPS" value={1234} prefix={<ThunderboltOutlined />} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="平均延迟" value={156} suffix="ms" /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="缓存命中率" value={85.5} suffix="%" /></Card>
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
