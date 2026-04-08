import { useState, useEffect } from 'react'
import {
  Table,
  Card,
  Button,
  Space,
  Tag,
  Modal,
  Select,
  Input,
  message,
  Popconfirm,
  Typography,
  Row,
  Col,
  Statistic,
  Progress,
  Divider,
  Alert,
  Form
} from 'antd'
import {
  ThunderboltOutlined,
  DeleteOutlined,
  ReloadOutlined,
  FireOutlined,
  ClockCircleOutlined,
  DatabaseOutlined,
  SearchOutlined,
  ClearOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import type { ColumnsType } from 'antd/es/table'
import { cacheApi } from '@/api/cache'
import type { CacheEntry, CacheStats, CacheType, CacheQueryParams } from '@/types/cache'
import dayjs from 'dayjs'

const { Title, Text, Paragraph } = Typography

// 模拟数据
const mockStats: CacheStats = {
  hitCount: 85678,
  missCount: 12456,
  hitRate: 87.3,
  totalKeys: 4523,
  totalSize: 128 * 1024 * 1024, // 128MB
  avgTtl: 3600,
  memoryUsed: 256 * 1024 * 1024, // 256MB
  memoryLimit: 512 * 1024 * 1024 // 512MB
}

const mockCacheEntries: CacheEntry[] = [
  {
    key: 'embedding:doc_101_chunk_0',
    value: '{"vector":[...],"metadata":{...}}',
    size: 4096,
    hitCount: 45,
    lastAccessTime: '2024-01-07 14:30:00',
    createTime: '2024-01-07 10:00:00',
    ttl: 7200,
    expired: false
  },
  {
    key: 'embedding:doc_102_chunk_5',
    value: '{"vector":[...],"metadata":{...}}',
    size: 8192,
    hitCount: 32,
    lastAccessTime: '2024-01-07 14:25:00',
    createTime: '2024-01-07 11:30:00',
    ttl: 7200,
    expired: false
  },
  {
    key: 'query:user001_session123',
    value: '{"messages":[...],"context":{...}}',
    size: 15360,
    hitCount: 78,
    lastAccessTime: '2024-01-07 14:28:00',
    createTime: '2024-01-07 09:00:00',
    ttl: 1800,
    expired: false
  },
  {
    key: 'vector:search_result_abc123',
    value: '{"results":[...],"scores":[...]}',
    size: 6144,
    hitCount: 12,
    lastAccessTime: '2024-01-07 13:00:00',
    createTime: '2024-01-07 12:00:00',
    ttl: 600,
    expired: false
  },
  {
    key: 'embedding:doc_105_chunk_2',
    value: '{"vector":[...],"metadata":{...}}',
    size: 3072,
    hitCount: 0,
    lastAccessTime: '2024-01-07 08:00:00',
    createTime: '2024-01-07 08:00:00',
    ttl: 7200,
    expired: true
  },
  {
    key: 'document:meta_201',
    value: '{"title":"...","content":"..."}',
    size: 2048,
    hitCount: 156,
    lastAccessTime: '2024-01-07 14:29:00',
    createTime: '2024-01-07 07:00:00',
    ttl: 14400,
    expired: false
  }
]

// 格式化文件大小
const formatSize = (bytes: number): string => {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

const Cache = () => {
  const [loading, setLoading] = useState(false)
  const [stats, setStats] = useState<CacheStats>(mockStats)
  const [cacheEntries, setCacheEntries] = useState<CacheEntry[]>(mockCacheEntries)
  const [params, setParams] = useState<CacheQueryParams>({
    pageNum: 1,
    pageSize: 10
  })
  const [total, setTotal] = useState(0)
  const [clearModalVisible, setClearModalVisible] = useState(false)
  const [warmupModalVisible, setWarmupModalVisible] = useState(false)
  const [selectedCacheType, setSelectedCacheType] = useState<CacheType>('ALL')
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([])
  const [form] = Form.useForm()

  useEffect(() => {
    loadStats()
    loadCacheEntries()
  }, [params])

  // 加载缓存统计
  const loadStats = async () => {
    try {
      // const res = await cacheApi.getCacheStats()
      // if (res.code === 200 && res.data) {
      //   setStats(res.data)
      // }
    } catch (error) {
      message.error('加载缓存统计失败')
    }
  }

  // 加载缓存键列表
  const loadCacheEntries = async () => {
    setLoading(true)
    try {
      // const res = await cacheApi.getCacheKeys(params)
      // if (res.code === 200 && res.data) {
      //   setCacheEntries(res.data.records)
      //   setTotal(res.data.total)
      // }
      setLoading(false)
    } catch (error) {
      message.error('加载缓存列表失败')
      setLoading(false)
    }
  }

  // 清除缓存
  const handleClearCache = async () => {
    try {
      // await cacheApi.clearCache({ cacheType: selectedCacheType, confirmClear: true })
      message.success('缓存清除成功')
      setClearModalVisible(false)
      loadStats()
      loadCacheEntries()
    } catch (error) {
      message.error('清除失败')
    }
  }

  // 批量删除选中的缓存
  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要删除的缓存')
      return
    }
    try {
      // await cacheApi.batchDeleteCache(selectedRowKeys)
      message.success(`已删除 ${selectedRowKeys.length} 个缓存`)
      setSelectedRowKeys([])
      loadStats()
      loadCacheEntries()
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 删除单个缓存
  const handleDeleteCache = async (key: string) => {
    try {
      // await cacheApi.deleteCache(key)
      message.success('删除成功')
      loadStats()
      loadCacheEntries()
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 缓存预热
  const handleWarmup = async (values: { cacheType: CacheType; priority: string }) => {
    try {
      // await cacheApi.warmupCache({ cacheType: values.cacheType, priority: values.priority })
      message.success('缓存预热任务已提交')
      setWarmupModalVisible(false)
      form.resetFields()
    } catch (error) {
      message.error('预热失败')
    }
  }

  // 命中率图表配置
  const hitRateOption = {
    title: { text: '缓存命中率', left: 'center', top: 10 },
    series: [
      {
        type: 'gauge',
        startAngle: 180,
        endAngle: 0,
        center: ['50%', '70%'],
        radius: '90%',
        min: 0,
        max: 100,
        splitNumber: 5,
        axisLine: {
          lineStyle: {
            width: 20,
            color: [
              [0.3, '#ff4d4f'],
              [0.7, '#faad14'],
              [1, '#52c41a']
            ]
          }
        },
        pointer: {
          itemStyle: { color: '#1890ff' }
        },
        axisTick: { show: false },
        splitLine: {
          length: 15,
          lineStyle: { width: 2, color: '#999' }
        },
        axisLabel: {
          distance: 30,
          color: '#999',
          fontSize: 12,
          formatter: '{value}%'
        },
        detail: {
          valueAnimation: true,
          formatter: '{value}%',
          color: '#1890ff',
          fontSize: 28,
          offsetCenter: [0, '0%']
        },
        data: [{ value: stats.hitRate }]
      }
    ]
  }

  // 内存使用图表配置
  const memoryOption = {
    title: { text: '内存使用情况', left: 'center', top: 10 },
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '55%'],
        avoidLabelOverlap: false,
        label: {
          show: true,
          position: 'outside',
          formatter: '{b}: {d}%'
        },
        data: [
          {
            name: '已使用',
            value: stats.memoryUsed,
            itemStyle: { color: '#1890ff' }
          },
          {
            name: '未使用',
            value: stats.memoryLimit - stats.memoryUsed,
            itemStyle: { color: '#f0f0f0' }
          }
        ]
      }
    ]
  }

  // 表格列定义
  const columns: ColumnsType<CacheEntry> = [
    {
      title: '缓存键',
      dataIndex: 'key',
      ellipsis: true,
      render: (text: string) => (
        <Text code ellipsis={{ tooltip: text }}>
          {text}
        </Text>
      )
    },
    {
      title: '大小',
      dataIndex: 'size',
      width: 100,
      render: (size: number) => formatSize(size)
    },
    {
      title: '命中次数',
      dataIndex: 'hitCount',
      width: 100,
      sorter: (a, b) => a.hitCount - b.hitCount,
      render: (count: number) => (
        <Tag color={count > 50 ? 'green' : count > 10 ? 'blue' : 'default'}>
          {count}
        </Tag>
      )
    },
    {
      title: 'TTL',
      dataIndex: 'ttl',
      width: 100,
      render: (ttl: number) => {
        const minutes = Math.floor(ttl / 60)
        const seconds = ttl % 60
        return `${minutes > 0 ? `${minutes}分` : ''}${seconds}秒`
      }
    },
    {
      title: '状态',
      dataIndex: 'expired',
      width: 80,
      render: (expired: boolean) => (
        <Tag color={expired ? 'error' : 'success'}>
          {expired ? '已过期' : '有效'}
        </Tag>
      )
    },
    {
      title: '最后访问',
      dataIndex: 'lastAccessTime',
      width: 160,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm')
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: unknown, record: CacheEntry) => (
        <Popconfirm
          title="确定删除该缓存？"
          onConfirm={() => handleDeleteCache(record.key)}
        >
          <Button type="link" size="small" danger icon={<DeleteOutlined />}>
            删除
          </Button>
        </Popconfirm>
      )
    }
  ]

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <Title level={4}>缓存管理</Title>
        <Space>
          <Button
            icon={<FireOutlined />}
            onClick={() => setWarmupModalVisible(true)}
          >
            缓存预热
          </Button>
          <Button
            danger
            icon={<ClearOutlined />}
            onClick={() => setClearModalVisible(true)}
          >
            清除缓存
          </Button>
          <Button icon={<ReloadOutlined />} onClick={loadStats}>
            刷新
          </Button>
        </Space>
      </div>

      {/* 统计卡片 */}
      <Row gutter={16}>
        <Col span={6}>
          <Card>
            <Statistic
              title="缓存命中率"
              value={stats.hitRate}
              suffix="%"
              prefix={<ThunderboltOutlined />}
              valueStyle={{ color: stats.hitRate > 80 ? '#52c41a' : '#faad14' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="总缓存键数"
              value={stats.totalKeys}
              prefix={<DatabaseOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="缓存大小"
              value={formatSize(stats.totalSize)}
              prefix={<DatabaseOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="平均TTL"
              value={Math.floor(stats.avgTtl / 60)}
              suffix="分钟"
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 图表 */}
      <Row gutter={16}>
        <Col span={12}>
          <Card>
            <ReactECharts option={hitRateOption} style={{ height: 250 }} />
          </Card>
        </Col>
        <Col span={12}>
          <Card>
            <Row gutter={16} align="middle">
              <Col span={12}>
                <ReactECharts option={memoryOption} style={{ height: 250 }} />
              </Col>
              <Col span={12}>
                <div className="space-y-4">
                  <div>
                    <Text type="secondary">内存使用</Text>
                    <Progress
                      percent={Math.round((stats.memoryUsed / stats.memoryLimit) * 100)}
                      format={(p) => `${formatSize(stats.memoryUsed)} / ${formatSize(stats.memoryLimit)}`}
                      status="active"
                    />
                  </div>
                  <div>
                    <Text type="secondary">命中次数</Text>
                    <div className="text-2xl font-bold text-green-500">{stats.hitCount.toLocaleString()}</div>
                  </div>
                  <div>
                    <Text type="secondary">未命中次数</Text>
                    <div className="text-2xl font-bold text-red-500">{stats.missCount.toLocaleString()}</div>
                  </div>
                </div>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      {/* 缓存键列表 */}
      <Card>
        <div className="flex justify-between items-center mb-4">
          <Space>
            <Select
              value={selectedCacheType}
              onChange={(value) => {
                setSelectedCacheType(value)
                setParams({ ...params, cacheType: value })
              }}
              style={{ width: 140 }}
              options={[
                { label: '全部', value: 'ALL' },
                { label: 'Embedding', value: 'EMBEDDING' },
                { label: 'Query', value: 'QUERY' },
                { label: 'Document', value: 'DOCUMENT' },
                { label: 'Vector', value: 'VECTOR' }
              ]}
            />
            <Input
              placeholder="搜索缓存键"
              prefix={<SearchOutlined />}
              style={{ width: 200 }}
              onSearch={(value) => setParams({ ...params, searchKey: value })}
              allowClear
            />
          </Space>
          <Space>
            <Text type="secondary">已选择 {selectedRowKeys.length} 项</Text>
            <Button
              danger
              disabled={selectedRowKeys.length === 0}
              onClick={handleBatchDelete}
            >
              批量删除
            </Button>
          </Space>
        </div>
        <Table
          columns={columns}
          dataSource={cacheEntries}
          rowKey="key"
          loading={loading}
          rowSelection={{
            selectedRowKeys,
            onChange: (keys) => setSelectedRowKeys(keys as string[])
          }}
          pagination={{
            current: params.pageNum,
            pageSize: params.pageSize,
            total,
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => setParams({ ...params, pageNum: page, pageSize })
          }}
        />
      </Card>

      {/* 清除缓存弹窗 */}
      <Modal
        title="清除缓存"
        open={clearModalVisible}
        onOk={handleClearCache}
        onCancel={() => setClearModalVisible(false)}
        okText="确认清除"
        okButtonProps={{ danger: true }}
      >
        <Alert
          message="警告"
          description="清除缓存将导致系统性能下降，所有缓存数据将被永久删除。"
          type="warning"
          showIcon
          className="mb-4"
        />
        <Form layout="vertical">
          <Form.Item label="选择要清除的缓存类型">
            <Select
              value={selectedCacheType}
              onChange={setSelectedCacheType}
              options={[
                { label: '全部缓存', value: 'ALL' },
                { label: 'Embedding缓存', value: 'EMBEDDING' },
                { label: 'Query缓存', value: 'QUERY' },
                { label: 'Document缓存', value: 'DOCUMENT' },
                { label: 'Vector缓存', value: 'VECTOR' }
              ]}
            />
          </Form.Item>
        </Form>
      </Modal>

      {/* 缓存预热弹窗 */}
      <Modal
        title="缓存预热"
        open={warmupModalVisible}
        onCancel={() => {
          setWarmupModalVisible(false)
          form.resetFields()
        }}
        footer={null}
      >
        <Form form={form} layout="vertical" onFinish={handleWarmup}>
          <Paragraph type="secondary">
            缓存预热会将热点数据提前加载到缓存中，提高系统响应速度。
          </Paragraph>
          <Form.Item
            name="cacheType"
            label="缓存类型"
            rules={[{ required: true, message: '请选择缓存类型' }]}
          >
            <Select
              placeholder="选择缓存类型"
              options={[
                { label: 'Embedding缓存', value: 'EMBEDDING' },
                { label: 'Query缓存', value: 'QUERY' },
                { label: 'Document缓存', value: 'DOCUMENT' }
              ]}
            />
          </Form.Item>
          <Form.Item
            name="priority"
            label="优先级"
            initialValue="NORMAL"
          >
            <Select
              options={[
                { label: '高优先级', value: 'HIGH' },
                { label: '普通优先级', value: 'NORMAL' },
                { label: '低优先级', value: 'LOW' }
              ]}
            />
          </Form.Item>
          <Form.Item className="mb-0">
            <Space>
              <Button type="primary" htmlType="submit">
                开始预热
              </Button>
              <Button onClick={() => setWarmupModalVisible(false)}>
                取消
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default Cache
