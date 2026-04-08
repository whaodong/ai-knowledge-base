import { useState, useEffect } from 'react'
import { Row, Col, Card, Statistic, Table, Select, DatePicker, Typography, Space } from 'antd'
import {
  ThunderboltOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  LineChartOutlined,
  PieChartOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import { tokenApi } from '@/api/token'
import type {
  TokenStatsSummary,
  TokenTrendData,
  ServiceTokenStats,
  TopTokenConsumer,
  TimeRange
} from '@/types/token'
import dayjs from 'dayjs'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

// 模拟数据
const mockSummary: TokenStatsSummary = {
  totalTokens: 1256890,
  inputTokens: 856234,
  outputTokens: 400656,
  totalCost: 23.45,
  avgDailyTokens: 179555,
  peakDailyTokens: 256780,
  totalRequests: 3456
}

const mockTrendData: TokenTrendData[] = [
  { date: '2024-01-01', totalTokens: 120000, inputTokens: 82000, outputTokens: 38000, requestCount: 456 },
  { date: '2024-01-02', totalTokens: 135000, inputTokens: 92000, outputTokens: 43000, requestCount: 512 },
  { date: '2024-01-03', totalTokens: 156000, inputTokens: 105000, outputTokens: 51000, requestCount: 589 },
  { date: '2024-01-04', totalTokens: 178000, inputTokens: 120000, outputTokens: 58000, requestCount: 634 },
  { date: '2024-01-05', totalTokens: 145000, inputTokens: 98000, outputTokens: 47000, requestCount: 523 },
  { date: '2024-01-06', totalTokens: 98000, inputTokens: 67000, outputTokens: 31000, requestCount: 378 },
  { date: '2024-01-07', totalTokens: 85000, inputTokens: 58000, outputTokens: 27000, requestCount: 298 }
]

const mockServiceStats: ServiceTokenStats[] = [
  { serviceType: 'RAG', totalTokens: 567890, requestCount: 1234, avgTokensPerRequest: 460, cost: 11.35 },
  { serviceType: 'EMBEDDING', totalTokens: 456789, requestCount: 1567, avgTokensPerRequest: 291, cost: 6.85 },
  { serviceType: 'SEARCH', totalTokens: 189012, requestCount: 678, avgTokensPerRequest: 279, cost: 4.15 },
  { serviceType: 'DOCUMENT', totalTokens: 43199, requestCount: 977, avgTokensPerRequest: 44, cost: 1.10 }
]

const mockTopConsumers: TopTokenConsumer[] = [
  { username: 'admin', totalTokens: 256780, requestCount: 890, avgTokensPerRequest: 288 },
  { username: 'user001', totalTokens: 198234, requestCount: 654, avgTokensPerRequest: 303 },
  { username: 'user002', totalTokens: 167890, requestCount: 523, avgTokensPerRequest: 321 },
  { username: 'user003', totalTokens: 145678, requestCount: 456, avgTokensPerRequest: 319 },
  { username: 'user004', totalTokens: 123456, requestCount: 389, avgTokensPerRequest: 317 },
  { username: 'user005', totalTokens: 98765, requestCount: 312, avgTokensPerRequest: 316 },
  { username: 'user006', totalTokens: 87654, requestCount: 287, avgTokensPerRequest: 305 },
  { username: 'user007', totalTokens: 76543, requestCount: 234, avgTokensPerRequest: 327 },
  { username: 'user008', totalTokens: 65432, requestCount: 198, avgTokensPerRequest: 330 },
  { username: 'user009', totalTokens: 54321, requestCount: 167, avgTokensPerRequest: 325 }
]

const TokenStats = () => {
  const [loading, setLoading] = useState(false)
  const [timeRange, setTimeRange] = useState<TimeRange>('week')
  const [summary, setSummary] = useState<TokenStatsSummary>(mockSummary)
  const [trendData, setTrendData] = useState<TokenTrendData[]>(mockTrendData)
  const [serviceStats, setServiceStats] = useState<ServiceTokenStats[]>(mockServiceStats)
  const [topConsumers, setTopConsumers] = useState<TopTokenConsumer[]>(mockTopConsumers)

  useEffect(() => {
    // 实际项目中从API获取数据
    // loadData()
  }, [timeRange])

  // Token使用趋势图
  const trendOption = {
    title: { text: 'Token 使用量趋势', left: 'center' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['总Token', '输入Token', '输出Token'], top: 30 },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '20%', containLabel: true },
    xAxis: {
      type: 'category',
      data: trendData.map(d => d.date),
      boundaryGap: false
    },
    yAxis: { type: 'value', name: 'Token数' },
    series: [
      {
        name: '总Token',
        type: 'line',
        smooth: true,
        data: trendData.map(d => d.totalTokens),
        areaStyle: { opacity: 0.3 },
        itemStyle: { color: '#1890ff' }
      },
      {
        name: '输入Token',
        type: 'line',
        smooth: true,
        data: trendData.map(d => d.inputTokens),
        itemStyle: { color: '#52c41a' }
      },
      {
        name: '输出Token',
        type: 'line',
        smooth: true,
        data: trendData.map(d => d.outputTokens),
        itemStyle: { color: '#faad14' }
      }
    ]
  }

  // 服务类型饼图
  const servicePieOption = {
    title: { text: '服务类型占比', left: 'center' },
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', top: 'center' },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        label: { show: true, formatter: '{b}\n{d}%' },
        data: serviceStats.map(s => ({
          name: s.serviceType,
          value: s.totalTokens
        })),
        itemStyle: {
          color: (params: { dataIndex: number }) => {
            const colors = ['#1890ff', '#52c41a', '#faad14', '#722ed1']
            return colors[params.dataIndex % colors.length]
          }
        }
      }
    ]
  }

  // 服务类型柱状图
  const serviceBarOption = {
    title: { text: '各服务Token消耗', left: 'center' },
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: serviceStats.map(s => s.serviceType)
    },
    yAxis: [
      { type: 'value', name: 'Token数' },
      { type: 'value', name: '费用($)' }
    ],
    series: [
      {
        name: 'Token数',
        type: 'bar',
        data: serviceStats.map(s => s.totalTokens),
        itemStyle: { color: '#1890ff' }
      },
      {
        name: '费用',
        type: 'bar',
        yAxisIndex: 1,
        data: serviceStats.map(s => s.cost),
        itemStyle: { color: '#52c41a' }
      }
    ]
  }

  // Top10消费者表格列
  const topConsumerColumns = [
    { title: '排名', dataIndex: 'rank', key: 'rank', width: 60, render: (_: unknown, __: unknown, index: number) => index + 1 },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '总Token', dataIndex: 'totalTokens', key: 'totalTokens', render: (val: number) => val.toLocaleString() },
    { title: '请求次数', dataIndex: 'requestCount', key: 'requestCount', render: (val: number) => val.toLocaleString() },
    { title: '平均Token/请求', dataIndex: 'avgTokensPerRequest', key: 'avgTokensPerRequest', render: (val: number) => val.toFixed(0) }
  ]

  const serviceTableColumns = [
    { title: '服务类型', dataIndex: 'serviceType', key: 'serviceType' },
    { title: '总Token', dataIndex: 'totalTokens', key: 'totalTokens', render: (val: number) => val.toLocaleString() },
    { title: '请求次数', dataIndex: 'requestCount', key: 'requestCount', render: (val: number) => val.toLocaleString() },
    { title: '平均Token/请求', dataIndex: 'avgTokensPerRequest', key: 'avgTokensPerRequest', render: (val: number) => val.toFixed(0) },
    { title: '费用($)', dataIndex: 'cost', key: 'cost', render: (val: number) => `$${val.toFixed(2)}` }
  ]

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <Title level={4}>Token 统计</Title>
        <Space>
          <Select
            value={timeRange}
            onChange={setTimeRange}
            style={{ width: 120 }}
            options={[
              { label: '今日', value: 'today' },
              { label: '本周', value: 'week' },
              { label: '本月', value: 'month' },
              { label: '自定义', value: 'custom' }
            ]}
          />
          {timeRange === 'custom' && (
            <RangePicker onChange={(dates) => console.log(dates)} />
          )}
        </Space>
      </div>

      {/* 关键指标卡片 */}
      <Row gutter={16}>
        <Col span={6}>
          <Card>
            <Statistic
              title="总Token数"
              value={summary.totalTokens}
              prefix={<ThunderboltOutlined />}
              suffix="Tokens"
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="日均消耗"
              value={summary.avgDailyTokens}
              prefix={<LineChartOutlined />}
              suffix="Tokens"
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="峰值消耗"
              value={summary.peakDailyTokens}
              prefix={<ArrowUpOutlined />}
              suffix="Tokens"
              valueStyle={{ color: '#faad14' }}
              valueRender={(value) => (
                <span style={{ color: '#faad14' }}>{Number(value).toLocaleString()}</span>
              )}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="总费用"
              value={summary.totalCost}
              prefix={<PieChartOutlined />}
              suffix="$"
              precision={2}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Token使用趋势图 */}
      <Card>
        <ReactECharts option={trendOption} style={{ height: 350 }} />
      </Card>

      {/* 服务类型分布 */}
      <Row gutter={16}>
        <Col span={12}>
          <Card>
            <ReactECharts option={servicePieOption} style={{ height: 300 }} />
          </Card>
        </Col>
        <Col span={12}>
          <Card title="服务类型统计">
            <Table
              columns={serviceTableColumns}
              dataSource={serviceStats}
              rowKey="serviceType"
              pagination={false}
              size="small"
            />
          </Card>
        </Col>
      </Row>

      {/* Token消耗Top10 */}
      <Card title="Token 消耗 Top 10">
        <Table
          columns={topConsumerColumns}
          dataSource={topConsumers.map((c, i) => ({ ...c, key: i }))}
          pagination={false}
          size="small"
        />
      </Card>
    </div>
  )
}

export default TokenStats
