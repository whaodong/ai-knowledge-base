import { Row, Col, Card, Statistic, Table } from 'antd'
import { FileTextOutlined, MessageOutlined, ThunderboltOutlined, ClockCircleOutlined } from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import { useState } from 'react'

interface ServiceHealth {
  service: string
  status: 'UP' | 'DOWN'
  port: number
  responseTime: number
}

const Monitoring = () => {
  const [services] = useState<ServiceHealth[]>([
    { service: 'document-service', status: 'UP', port: 8081, responseTime: 45 },
    { service: 'embedding-service', status: 'UP', port: 8082, responseTime: 120 },
    { service: 'rag-service', status: 'UP', port: 8083, responseTime: 85 },
    { service: 'milvus-service', status: 'UP', port: 8086, responseTime: 30 },
  ])

  const columns = [
    { title: '服务名称', dataIndex: 'service', key: 'service' },
    { 
      title: '状态', 
      dataIndex: 'status', 
      key: 'status',
      render: (status: string) => (
        <span style={{ color: status === 'UP' ? '#52c41a' : '#ff4d4f' }}>
          {status === 'UP' ? '● 运行中' : '● 已停止'}
        </span>
      )
    },
    { title: '端口', dataIndex: 'port', key: 'port' },
    { title: '响应时间(ms)', dataIndex: 'responseTime', key: 'responseTime' },
  ]

  // QPS趋势图配置
  const qpsOption = {
    title: { text: 'QPS 趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00']
    },
    yAxis: { type: 'value', name: 'QPS' },
    series: [{
      name: 'QPS',
      type: 'line',
      smooth: true,
      data: [120, 80, 150, 280, 350, 220, 180],
      areaStyle: { opacity: 0.3 }
    }]
  }

  // 延迟分布图配置
  const latencyOption = {
    title: { text: '延迟分布 (P50/P90/P99)', left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['P50', 'P90', 'P99'], top: 30 },
    xAxis: {
      type: 'category',
      data: ['文档上传', '向量生成', 'RAG查询', '向量搜索']
    },
    yAxis: { type: 'value', name: '延迟(ms)' },
    series: [
      { name: 'P50', type: 'bar', data: [120, 85, 150, 45] },
      { name: 'P90', type: 'bar', data: [280, 150, 320, 80] },
      { name: 'P99', type: 'bar', data: [450, 250, 580, 120] }
    ]
  }

  // Token使用趋势
  const tokenOption = {
    title: { text: 'Token 使用趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: { type: 'value', name: 'Token数' },
    series: [{
      name: 'Token使用量',
      type: 'line',
      smooth: true,
      data: [12000, 15000, 18000, 22000, 19000, 8000, 5000],
      areaStyle: { opacity: 0.3 }
    }]
  }

  // 缓存命中率
  const cacheOption = {
    title: { text: '缓存命中率', left: 'center' },
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      label: { show: true, formatter: '{b}: {d}%' },
      data: [
        { value: 75, name: '命中', itemStyle: { color: '#52c41a' } },
        { value: 25, name: '未命中', itemStyle: { color: '#ff4d4f' } }
      ]
    }]
  }

  return (
    <div className="space-y-4">
      {/* 统计卡片 */}
      <Row gutter={16}>
        <Col span={6}>
          <Card>
            <Statistic
              title="文档总数"
              value={1234}
              prefix={<FileTextOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="对话次数"
              value={5678}
              prefix={<MessageOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="平均延迟"
              value={156}
              suffix="ms"
              prefix={<ThunderboltOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="运行时间"
              value={72}
              suffix="小时"
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 服务状态 */}
      <Card title="服务状态">
        <Table
          columns={columns}
          dataSource={services}
          rowKey="service"
          pagination={false}
          size="small"
        />
      </Card>

      {/* 图表区域 */}
      <Row gutter={16}>
        <Col span={12}>
          <Card>
            <ReactECharts option={qpsOption} style={{ height: 300 }} />
          </Card>
        </Col>
        <Col span={12}>
          <Card>
            <ReactECharts option={latencyOption} style={{ height: 300 }} />
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={12}>
          <Card>
            <ReactECharts option={tokenOption} style={{ height: 300 }} />
          </Card>
        </Col>
        <Col span={12}>
          <Card>
            <ReactECharts option={cacheOption} style={{ height: 300 }} />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Monitoring
