import { useState, useEffect } from 'react'
import {
  Table,
  Card,
  Button,
  Space,
  Tag,
  Modal,
  message,
  Popconfirm,
  Typography,
  Row,
  Col,
  Statistic,
  Descriptions,
  Divider,
  Input
} from 'antd'
import {
  DatabaseOutlined,
  DeleteOutlined,
  ReloadOutlined,
  BuildOutlined,
  EyeOutlined,
  SearchOutlined,
  AppstoreOutlined,
  TableOutlined
} from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { vectorApi } from '@/api/vector'
import type { Collection, CollectionDetail, CollectionStats, IndexType } from '@/types/vector'
import dayjs from 'dayjs'

const { Title, Text } = Typography

// 模拟数据
const mockCollections: Collection[] = [
  {
    name: 'documents',
    dimension: 1536,
    indexType: 'HNSW',
    metricType: 'COSINE',
    vectorCount: 125680,
    totalChunks: 125680,
    usingField: 'embedding',
    status: 'READY',
    createTime: '2024-01-01 10:00:00',
    description: '文档向量集合'
  },
  {
    name: 'knowledge_base',
    dimension: 1536,
    indexType: 'IVF_FLAT',
    metricType: 'L2',
    vectorCount: 45678,
    totalChunks: 45678,
    usingField: 'text_embedding',
    status: 'READY',
    createTime: '2024-01-05 14:30:00',
    description: '知识库向量集合'
  },
  {
    name: 'faq_vectors',
    dimension: 768,
    indexType: 'FLAT',
    metricType: 'IP',
    vectorCount: 2345,
    totalChunks: 2345,
    usingField: 'faq_embedding',
    status: 'BUILDING',
    createTime: '2024-01-07 09:00:00',
    description: 'FAQ问答向量集合'
  }
]

const mockStats: CollectionStats = {
  totalCollections: 3,
  totalVectors: 173703,
  totalDimension: 3840
}

const Vectors = () => {
  const [loading, setLoading] = useState(false)
  const [collections, setCollections] = useState<Collection[]>(mockCollections)
  const [stats, setStats] = useState<CollectionStats>(mockStats)
  const [selectedCollection, setSelectedCollection] = useState<Collection | null>(null)
  const [detailModalVisible, setDetailModalVisible] = useState(false)
  const [deleteModalVisible, setDeleteModalVisible] = useState(false)
  const [confirmName, setConfirmName] = useState('')
  const [searchKeyword, setSearchKeyword] = useState('')

  useEffect(() => {
    loadCollections()
  }, [])

  // 加载Collection列表
  const loadCollections = async () => {
    setLoading(true)
    try {
      // 实际项目中调用API
      // const res = await vectorApi.getCollections()
      // if (res.code === 200 && res.data) {
      //   setCollections(res.data)
      // }
      setLoading(false)
    } catch (error) {
      message.error('加载Collection列表失败')
      setLoading(false)
    }
  }

  // 查看详情
  const handleViewDetail = async (collection: Collection) => {
    setSelectedCollection(collection)
    setDetailModalVisible(true)
    try {
      // const res = await vectorApi.getCollectionDetail(collection.name)
      // if (res.code === 200) {
      //   setSelectedCollection(res.data)
      // }
    } catch (error) {
      message.error('加载详情失败')
    }
  }

  // 删除Collection
  const handleDelete = async () => {
    if (!selectedCollection || confirmName !== selectedCollection.name) {
      message.warning('请输入正确的Collection名称')
      return
    }
    try {
      // await vectorApi.deleteCollection({ collectionName: selectedCollection.name, confirmName })
      message.success('删除成功')
      setDeleteModalVisible(false)
      setConfirmName('')
      loadCollections()
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 重建索引
  const handleRebuildIndex = async (collection: Collection) => {
    Modal.confirm({
      title: '确认重建索引',
      content: `确定要重建 "${collection.name}" 的索引吗？这可能需要一些时间。`,
      onOk: async () => {
        try {
          // await vectorApi.buildIndex({ collectionName: collection.name, fieldName: collection.usingField, indexType: 'HNSW' })
          message.success('索引重建任务已提交')
          loadCollections()
        } catch (error) {
          message.error('提交失败')
        }
      }
    })
  }

  // 获取向量数量
  const handleGetCount = async (collection: Collection) => {
    try {
      // const res = await vectorApi.getCollectionCount(collection.name)
      message.info(`${collection.name} 包含 ${collection.vectorCount.toLocaleString()} 个向量`)
    } catch (error) {
      message.error('获取数量失败')
    }
  }

  // 表格列定义
  const columns: ColumnsType<Collection> = [
    {
      title: 'Collection名称',
      dataIndex: 'name',
      render: (text: string) => (
        <Space>
          <DatabaseOutlined />
          <Text strong>{text}</Text>
        </Space>
      )
    },
    {
      title: '向量维度',
      dataIndex: 'dimension',
      width: 100,
      render: (dim: number) => (
        <Tag color="blue">{dim}</Tag>
      )
    },
    {
      title: '索引类型',
      dataIndex: 'indexType',
      width: 100,
      render: (type: IndexType) => (
        <Tag color="green">{type}</Tag>
      )
    },
    {
      title: '度量类型',
      dataIndex: 'metricType',
      width: 100,
      render: (type: string) => (
        <Tag>{type}</Tag>
      )
    },
    {
      title: '向量数量',
      dataIndex: 'vectorCount',
      width: 120,
      render: (count: number) => count.toLocaleString()
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={status === 'READY' ? 'success' : status === 'BUILDING' ? 'processing' : 'error'}>
          {status === 'READY' ? '就绪' : status === 'BUILDING' ? '构建中' : '失败'}
        </Tag>
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
      width: 200,
      render: (_: unknown, record: Collection) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetail(record)}
          >
            详情
          </Button>
          <Button
            type="link"
            size="small"
            icon={<BuildOutlined />}
            onClick={() => handleRebuildIndex(record)}
          >
            重建索引
          </Button>
          <Popconfirm
            title="确认删除"
            description={
              <div>
                <p>确定要删除 Collection "{record.name}" 吗？</p>
                <p className="text-red-500">此操作不可恢复！</p>
              </div>
            }
            onConfirm={() => {
              setSelectedCollection(record)
              setDeleteModalVisible(true)
            }}
          >
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  // 过滤后的集合
  const filteredCollections = searchKeyword
    ? collections.filter(c => c.name.toLowerCase().includes(searchKeyword.toLowerCase()))
    : collections

  return (
    <div className="space-y-4">
      <div className="flex justify-between items-center">
        <Title level={4}>向量管理</Title>
        <Space>
          <Input
            placeholder="搜索Collection"
            prefix={<SearchOutlined />}
            style={{ width: 200 }}
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            allowClear
          />
          <Button icon={<ReloadOutlined />} onClick={loadCollections}>
            刷新
          </Button>
        </Space>
      </div>

      {/* 统计卡片 */}
      <Row gutter={16}>
        <Col span={8}>
          <Card>
            <Statistic
              title="Collection数量"
              value={stats.totalCollections}
              prefix={<AppstoreOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="总向量数"
              value={stats.totalVectors}
              prefix={<DatabaseOutlined />}
              valueStyle={{ color: '#52c41a' }}
              formatter={(value) => Number(value).toLocaleString()}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="总维度"
              value={stats.totalDimension}
              prefix={<TableOutlined />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Collection列表 */}
      <Card>
        <Table
          columns={columns}
          dataSource={filteredCollections}
          rowKey="name"
          loading={loading}
          pagination={false}
        />
      </Card>

      {/* 详情弹窗 */}
      <Modal
        title="Collection 详情"
        open={detailModalVisible}
        onCancel={() => setDetailModalVisible(false)}
        footer={
          <Button onClick={() => setDetailModalVisible(false)}>
            关闭
          </Button>
        }
        width={700}
      >
        {selectedCollection && (
          <div>
            <Descriptions bordered column={2}>
              <Descriptions.Item label="Collection名称" span={2}>
                <Space>
                  <DatabaseOutlined />
                  <Text strong>{selectedCollection.name}</Text>
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="向量维度">
                <Tag color="blue">{selectedCollection.dimension}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="索引类型">
                <Tag color="green">{selectedCollection.indexType}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="度量类型">
                {selectedCollection.metricType}
              </Descriptions.Item>
              <Descriptions.Item label="向量字段">
                {selectedCollection.usingField}
              </Descriptions.Item>
              <Descriptions.Item label="向量数量" span={2}>
                {selectedCollection.vectorCount.toLocaleString()}
              </Descriptions.Item>
              <Descriptions.Item label="总Chunk数">
                {selectedCollection.totalChunks.toLocaleString()}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={selectedCollection.status === 'READY' ? 'success' : 'processing'}>
                  {selectedCollection.status === 'READY' ? '就绪' : '构建中'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="描述" span={2}>
                {selectedCollection.description || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="创建时间" span={2}>
                {dayjs(selectedCollection.createTime).format('YYYY-MM-DD HH:mm:ss')}
              </Descriptions.Item>
            </Descriptions>
            <Divider />
            <Space>
              <Button
                icon={<BuildOutlined />}
                onClick={() => {
                  setDetailModalVisible(false)
                  handleRebuildIndex(selectedCollection)
                }}
              >
                重建索引
              </Button>
              <Button
                icon={<DeleteOutlined />}
                danger
                onClick={() => {
                  setDetailModalVisible(false)
                  setSelectedCollection(selectedCollection)
                  setDeleteModalVisible(true)
                }}
              >
                删除Collection
              </Button>
            </Space>
          </div>
        )}
      </Modal>

      {/* 删除确认弹窗 */}
      <Modal
        title="确认删除 Collection"
        open={deleteModalVisible}
        onOk={handleDelete}
        onCancel={() => {
          setDeleteModalVisible(false)
          setConfirmName('')
        }}
        okText="确认删除"
        okButtonProps={{ danger: true, disabled: confirmName !== selectedCollection?.name }}
      >
        <div className="space-y-4">
          <Text type="danger">此操作不可恢复！删除后所有向量数据将被永久删除。</Text>
          <div>
            <Text>请输入 Collection 名称以确认：</Text>
            <Input
              placeholder={selectedCollection?.name}
              value={confirmName}
              onChange={(e) => setConfirmName(e.target.value)}
              style={{ marginTop: 8 }}
            />
          </div>
        </div>
      </Modal>
    </div>
  )
}

export default Vectors
