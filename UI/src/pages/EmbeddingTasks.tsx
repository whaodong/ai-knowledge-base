import { useState, useEffect } from 'react'
import {
  Table,
  Card,
  Button,
  Space,
  Tag,
  Progress,
  Modal,
  Select,
  Input,
  message,
  Popconfirm,
  Typography,
  Row,
  Col,
  Statistic,
  DatePicker
} from 'antd'
import {
  ReloadOutlined,
  PlayCircleOutlined,
  StopOutlined,
  DeleteOutlined,
  RetryOutlined,
  PlusOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { embeddingApi } from '@/api/embedding'
import type { EmbeddingTask, EmbeddingTaskStatus, EmbeddingTaskStats, EmbeddingTaskQueryParams } from '@/types/embedding'
import { EMBEDDING_TASK_STATUS_MAP } from '@/types/embedding'
import dayjs from 'dayjs'

const { Title, Text } = Typography
const { RangePicker } = DatePicker
const { Search } = Input

// 模拟数据
const mockTasks: EmbeddingTask[] = [
  {
    id: '1',
    documentId: 101,
    documentName: '产品需求文档.pdf',
    status: 'COMPLETED',
    totalChunks: 50,
    completedChunks: 50,
    failedChunks: 0,
    progress: 100,
    inputTokens: 12500,
    outputTokens: 8000,
    startTime: '2024-01-07 10:00:00',
    endTime: '2024-01-07 10:02:35',
    createTime: '2024-01-07 09:58:00',
    retryCount: 0
  },
  {
    id: '2',
    documentId: 102,
    documentName: '技术架构设计.docx',
    status: 'PROCESSING',
    totalChunks: 120,
    completedChunks: 78,
    failedChunks: 2,
    progress: 65,
    inputTokens: 28000,
    outputTokens: 0,
    startTime: '2024-01-07 11:30:00',
    createTime: '2024-01-07 11:25:00',
    retryCount: 1
  },
  {
    id: '3',
    documentId: 103,
    documentName: '用户手册.md',
    status: 'PENDING',
    totalChunks: 35,
    completedChunks: 0,
    failedChunks: 0,
    progress: 0,
    inputTokens: 0,
    outputTokens: 0,
    createTime: '2024-01-07 12:00:00',
    retryCount: 0
  },
  {
    id: '4',
    documentId: 104,
    documentName: 'API接口文档.pdf',
    status: 'FAILED',
    totalChunks: 80,
    completedChunks: 45,
    failedChunks: 35,
    progress: 56,
    inputTokens: 15000,
    outputTokens: 12000,
    startTime: '2024-01-07 09:00:00',
    createTime: '2024-01-07 08:55:00',
    errorMessage: '向量维度不匹配，请检查嵌入模型配置',
    retryCount: 3
  },
  {
    id: '5',
    documentId: 105,
    documentName: '数据库设计.sql',
    status: 'COMPLETED',
    totalChunks: 25,
    completedChunks: 25,
    failedChunks: 0,
    progress: 100,
    inputTokens: 5800,
    outputTokens: 4200,
    startTime: '2024-01-07 08:00:00',
    endTime: '2024-01-07 08:01:15',
    createTime: '2024-01-07 07:58:00',
    retryCount: 0
  },
  {
    id: '6',
    documentId: 106,
    documentName: '测试报告.docx',
    status: 'PENDING',
    totalChunks: 45,
    completedChunks: 0,
    failedChunks: 0,
    progress: 0,
    inputTokens: 0,
    outputTokens: 0,
    createTime: '2024-01-07 12:15:00',
    retryCount: 0
  },
  {
    id: '7',
    documentId: 107,
    documentName: '会议纪要.txt',
    status: 'PROCESSING',
    totalChunks: 15,
    completedChunks: 10,
    failedChunks: 0,
    progress: 67,
    inputTokens: 3200,
    outputTokens: 0,
    startTime: '2024-01-07 12:10:00',
    createTime: '2024-01-07 12:08:00',
    retryCount: 0
  }
]

const mockStats: EmbeddingTaskStats = {
  totalTasks: 156,
  pendingTasks: 12,
  processingTasks: 5,
  completedTasks: 128,
  failedTasks: 11,
  avgProcessingTime: 45
}

const EmbeddingTasks = () => {
  const [loading, setLoading] = useState(false)
  const [tasks, setTasks] = useState<EmbeddingTask[]>(mockTasks)
  const [stats, setStats] = useState<EmbeddingTaskStats>(mockStats)
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([])
  const [params, setParams] = useState<EmbeddingTaskQueryParams>({
    pageNum: 1,
    pageSize: 10
  })
  const [total, setTotal] = useState(0)
  const [batchModalVisible, setBatchModalVisible] = useState(false)
  const [retryModalVisible, setRetryModalVisible] = useState(false)

  useEffect(() => {
    loadTasks()
  }, [params])

  // 加载任务列表
  const loadTasks = async () => {
    setLoading(true)
    try {
      // 实际项目中调用API
      // const res = await embeddingApi.getEmbeddingTasks(params)
      // if (res.code === 200 && res.data) {
      //   setTasks(res.data.records)
      //   setTotal(res.data.total)
      // }
      setLoading(false)
    } catch (error) {
      message.error('加载任务列表失败')
      setLoading(false)
    }
  }

  // 重试失败任务
  const handleRetry = async (id: string) => {
    try {
      // await embeddingApi.retryTask(id)
      message.success('任务已重新提交')
      loadTasks()
    } catch (error) {
      message.error('重试失败')
    }
  }

  // 批量重试失败任务
  const handleBatchRetryFailed = async () => {
    try {
      // await embeddingApi.batchRetryFailedTasks()
      message.success('已重新提交所有失败任务')
      setRetryModalVisible(false)
      loadTasks()
    } catch (error) {
      message.error('批量重试失败')
    }
  }

  // 批量提交
  const handleBatchSubmit = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要提交的任务')
      return
    }
    try {
      // await embeddingApi.batchSubmitTasks({ documentIds: selectedRowKeys.map(Number) })
      message.success(`已提交 ${selectedRowKeys.length} 个任务`)
      setBatchModalVisible(false)
      setSelectedRowKeys([])
      loadTasks()
    } catch (error) {
      message.error('批量提交失败')
    }
  }

  // 取消任务
  const handleCancel = async (id: string) => {
    try {
      // await embeddingApi.cancelTask(id)
      message.success('任务已取消')
      loadTasks()
    } catch (error) {
      message.error('取消失败')
    }
  }

  // 删除任务
  const handleDelete = async (id: string) => {
    try {
      // await embeddingApi.deleteTask(id)
      message.success('删除成功')
      loadTasks()
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 表格列定义
  const columns: ColumnsType<EmbeddingTask> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 80
    },
    {
      title: '文档名称',
      dataIndex: 'documentName',
      ellipsis: true,
      render: (text: string) => <Text ellipsis={{ tooltip: text }}>{text}</Text>
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: EmbeddingTaskStatus) => {
        const config = EMBEDDING_TASK_STATUS_MAP[status]
        return <Tag color={config?.color}>{config?.text || status}</Tag>
      }
    },
    {
      title: '进度',
      dataIndex: 'progress',
      width: 180,
      render: (progress: number, record: EmbeddingTask) => (
        <div className="w-full pr-4">
          <Progress
            percent={progress}
            size="small"
            status={record.status === 'FAILED' ? 'exception' : undefined}
            format={(p) => `${record.completedChunks}/${record.totalChunks}`}
          />
        </div>
      )
    },
    {
      title: 'Token',
      width: 120,
      render: (_: unknown, record: EmbeddingTask) => (
        <span>
          {record.inputTokens > 0 ? (
            <>
              <Text type="secondary">入:</Text> {(record.inputTokens / 1000).toFixed(1)}K
              {record.outputTokens > 0 && (
                <> | <Text type="secondary">出:</Text> {(record.outputTokens / 1000).toFixed(1)}K</>
              )}
            </>
          ) : (
            <Text type="secondary">-</Text>
          )}
        </span>
      )
    },
    {
      title: '重试次数',
      dataIndex: 'retryCount',
      width: 80,
      render: (count: number) => (
        <span style={{ color: count > 0 ? '#faad14' : 'inherit' }}>{count}</span>
      )
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 160,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm')
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      fixed: 'right',
      render: (_: unknown, record: EmbeddingTask) => (
        <Space size="small">
          {record.status === 'FAILED' && (
            <Button
              type="link"
              size="small"
              icon={<RetryOutlined />}
              onClick={() => handleRetry(record.id)}
            >
              重试
            </Button>
          )}
          {record.status === 'PENDING' && (
            <Button
              type="link"
              size="small"
              danger
              icon={<StopOutlined />}
              onClick={() => handleCancel(record.id)}
            >
              取消
            </Button>
          )}
          {record.status !== 'PROCESSING' && (
            <Popconfirm
              title="确定删除该任务？"
              onConfirm={() => handleDelete(record.id)}
            >
              <Button type="link" size="small" danger icon={<DeleteOutlined />}>
                删除
              </Button>
            </Popconfirm>
          )}
        </Space>
      )
    }
  ]

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <Title level={4}>向量化任务</Title>
        <Space>
          <Button
            icon={<RetryOutlined />}
            onClick={() => setRetryModalVisible(true)}
          >
            重试失败任务
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setBatchModalVisible(true)}
            disabled={selectedRowKeys.length === 0}
          >
            批量提交 ({selectedRowKeys.length})
          </Button>
          <Button icon={<ReloadOutlined />} onClick={loadTasks}>
            刷新
          </Button>
        </Space>
      </div>

      {/* 统计卡片 */}
      <Row gutter={16}>
        <Col span={6}>
          <Card>
            <Statistic
              title="总任务数"
              value={stats.totalTasks}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="待处理"
              value={stats.pendingTasks}
              prefix={<ClockCircleOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="进行中"
              value={stats.processingTasks}
              prefix={<PlayCircleOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="失败任务"
              value={stats.failedTasks}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 筛选条件 */}
      <Card>
        <Space wrap>
          <Select
            placeholder="状态筛选"
            allowClear
            style={{ width: 120 }}
            onChange={(value) => setParams({ ...params, status: value })}
            options={Object.entries(EMBEDDING_TASK_STATUS_MAP).map(([value, config]) => ({
              label: config.text,
              value
            }))}
          />
          <Search
            placeholder="搜索文档名称"
            style={{ width: 200 }}
            onSearch={(value) => setParams({ ...params, documentName: value })}
          />
          <RangePicker
            onChange={(dates) => {
              if (dates) {
                setParams({
                  ...params,
                  startDate: dates[0]?.format('YYYY-MM-DD'),
                  endDate: dates[1]?.format('YYYY-MM-DD')
                })
              }
            }}
          />
        </Space>
      </Card>

      {/* 任务列表 */}
      <Card>
        <Table
          columns={columns}
          dataSource={tasks}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1200 }}
          rowSelection={{
            selectedRowKeys,
            onChange: (keys) => setSelectedRowKeys(keys as string[])
          }}
          pagination={{
            current: params.pageNum,
            pageSize: params.pageSize,
            total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => setParams({ ...params, pageNum: page, pageSize })
          }}
        />
      </Card>

      {/* 批量提交弹窗 */}
      <Modal
        title="批量提交任务"
        open={batchModalVisible}
        onOk={handleBatchSubmit}
        onCancel={() => {
          setBatchModalVisible(false)
          setSelectedRowKeys([])
        }}
      >
        <p>确定要提交选中的 {selectedRowKeys.length} 个任务吗？</p>
        <p className="text-gray-500">提示：已在队列中的任务将重新排队</p>
      </Modal>

      {/* 重试失败任务弹窗 */}
      <Modal
        title="重试失败任务"
        open={retryModalVisible}
        onOk={handleBatchRetryFailed}
        onCancel={() => setRetryModalVisible(false)}
      >
        <p>确定要重新提交所有失败的任务吗？</p>
        <p className="text-gray-500">当前有 {stats.failedTasks} 个失败任务</p>
      </Modal>
    </div>
  )
}

export default EmbeddingTasks
