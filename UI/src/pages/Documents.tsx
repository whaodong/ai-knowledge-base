import { useState, useEffect } from 'react'
import { Table, Card, Button, Upload, Space, Tag, Modal, message, Popconfirm, Input, Select } from 'antd'
import { UploadOutlined, DeleteOutlined, EyeOutlined, ReloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { UploadFile } from 'antd/es/upload/interface'
import { documentsApi } from '@/api/documents'
import type { Document, DocumentQueryParams } from '@/types/document'
import { DOCUMENT_STATUS_MAP, FILE_TYPE_MAP } from '@/types/document'
import dayjs from 'dayjs'

const Documents = () => {
  const [loading, setLoading] = useState(false)
  const [documents, setDocuments] = useState<Document[]>([])
  const [total, setTotal] = useState(0)
  const [params, setParams] = useState<DocumentQueryParams>({
    pageNum: 1,
    pageSize: 10,
    sortOrder: 'DESC'
  })
  const [selectedRowKeys, setSelectedRowKeys] = useState<number[]>([])
  const [uploadModalVisible, setUploadModalVisible] = useState(false)
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [uploading, setUploading] = useState(false)

  // 加载文档列表
  const loadDocuments = async () => {
    setLoading(true)
    try {
      const res = await documentsApi.getDocuments(params)
      if (res.code === 200 && res.data) {
        setDocuments(res.data.records)
        setTotal(res.data.total)
      }
    } catch (error) {
      message.error('加载文档列表失败')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadDocuments()
  }, [params])

  // 上传文档
  const handleUpload = async () => {
    if (fileList.length === 0) {
      message.warning('请选择要上传的文件')
      return
    }
    setUploading(true)
    try {
      const files = fileList.map(f => f.originFileObj as File)
      if (files.length === 1) {
        await documentsApi.uploadDocument(files[0])
      } else {
        await documentsApi.batchUploadDocuments(files)
      }
      message.success('上传成功')
      setUploadModalVisible(false)
      setFileList([])
      loadDocuments()
    } catch (error) {
      message.error('上传失败')
    } finally {
      setUploading(false)
    }
  }

  // 删除文档
  const handleDelete = async (id: number) => {
    try {
      await documentsApi.deleteDocument(id)
      message.success('删除成功')
      loadDocuments()
    } catch (error) {
      message.error('删除失败')
    }
  }

  // 批量删除
  const handleBatchDelete = async () => {
    if (selectedRowKeys.length === 0) {
      message.warning('请选择要删除的文档')
      return
    }
    try {
      await documentsApi.batchDeleteDocuments(selectedRowKeys)
      message.success('批量删除成功')
      setSelectedRowKeys([])
      loadDocuments()
    } catch (error) {
      message.error('批量删除失败')
    }
  }

  // 表格列定义
  const columns: ColumnsType<Document> = [
    {
      title: 'ID',
      dataIndex: 'id',
      width: 80
    },
    {
      title: '文件名',
      dataIndex: 'originalFileName',
      ellipsis: true,
      render: (text: string) => <a>{text}</a>
    },
    {
      title: '类型',
      dataIndex: 'fileType',
      width: 100,
      render: (type: string) => FILE_TYPE_MAP[type] || type
    },
    {
      title: '大小',
      dataIndex: 'fileSize',
      width: 100,
      render: (size: number) => {
        if (size < 1024) return `${size} B`
        if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
        return `${(size / 1024 / 1024).toFixed(1)} MB`
      }
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status: string) => {
        const config = DOCUMENT_STATUS_MAP[status] || { text: status, color: 'default' }
        return <Tag color={config.color}>{config.text}</Tag>
      }
    },
    {
      title: '创建时间',
      dataIndex: 'createTime',
      width: 180,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm:ss')
    },
    {
      title: '操作',
      key: 'action',
      width: 150,
      render: (_, record) => (
        <Space size="small">
          <Button type="link" size="small" icon={<EyeOutlined />}>查看</Button>
          <Popconfirm title="确定删除该文档吗？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      )
    }
  ]

  return (
    <div className="space-y-4">
      {/* 搜索栏 */}
      <Card>
        <Space wrap>
          <Input.Search
            placeholder="搜索文件名"
            allowClear
            style={{ width: 250 }}
            onSearch={(value) => setParams({ ...params, fileName: value, pageNum: 1 })}
          />
          <Select
            placeholder="文件类型"
            allowClear
            style={{ width: 120 }}
            onChange={(value) => setParams({ ...params, fileType: value, pageNum: 1 })}
            options={Object.entries(FILE_TYPE_MAP).map(([key, label]) => ({ value: key, label }))}
          />
          <Select
            placeholder="状态"
            allowClear
            style={{ width: 120 }}
            onChange={(value) => setParams({ ...params, status: value, pageNum: 1 })}
            options={Object.entries(DOCUMENT_STATUS_MAP).map(([key, { text }]) => ({ value: key, label: text }))}
          />
          <Button icon={<ReloadOutlined />} onClick={() => loadDocuments()}>刷新</Button>
        </Space>
      </Card>

      {/* 文档列表 */}
      <Card
        title="文档列表"
        extra={
          <Space>
            {selectedRowKeys.length > 0 && (
              <Popconfirm title="确定删除选中的文档吗？" onConfirm={handleBatchDelete}>
                <Button danger>批量删除 ({selectedRowKeys.length})</Button>
              </Popconfirm>
            )}
            <Button type="primary" icon={<UploadOutlined />} onClick={() => setUploadModalVisible(true)}>
              上传文档
            </Button>
          </Space>
        }
      >
        <Table
          columns={columns}
          dataSource={documents}
          rowKey="id"
          loading={loading}
          rowSelection={{
            selectedRowKeys,
            onChange: (keys) => setSelectedRowKeys(keys as number[])
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

      {/* 上传弹窗 */}
      <Modal
        title="上传文档"
        open={uploadModalVisible}
        onCancel={() => {
          setUploadModalVisible(false)
          setFileList([])
        }}
        onOk={handleUpload}
        confirmLoading={uploading}
      >
        <Upload.Dragger
          multiple
          fileList={fileList}
          onChange={({ fileList }) => setFileList(fileList)}
          beforeUpload={() => false}
          accept=".pdf,.doc,.docx,.txt,.md,.json"
        >
          <p className="ant-upload-drag-icon">
            <UploadOutlined />
          </p>
          <p className="ant-upload-text">点击或拖拽文件到此区域上传</p>
          <p className="ant-upload-hint">支持 PDF、Word、TXT、Markdown、JSON 格式</p>
        </Upload.Dragger>
      </Modal>
    </div>
  )
}

export default Documents
