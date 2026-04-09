import { useState } from 'react'
import { Table, Tag, Button, Space, Popconfirm } from 'antd'
import { DeleteOutlined, EyeOutlined, UploadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useDocuments, useDocumentMutations } from '@/hooks/useDocuments'
import DocumentUpload from './DocumentUpload'
import type { Document } from '@/types/document'

const DocumentList = () => {
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [uploadOpen, setUploadOpen] = useState(false)
  
  const { data, isLoading } = useDocuments({ pageNum: page, pageSize, sortOrder: 'DESC' })
  const { delete: deleteDoc } = useDocumentMutations()

  const columns: ColumnsType<Document> = [
    { title: '标题', dataIndex: 'originalFileName', key: 'originalFileName', ellipsis: true },
    { title: '类型', dataIndex: 'fileType', key: 'fileType', width: 80, render: (t: string) => <Tag>{String(t).toUpperCase()}</Tag> },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100, render: (s: string) => {
      const colors: Record<string, string> = { UPLOADED: 'default', PROCESSING: 'processing', PARSED: 'cyan', EMBEDDED: 'success', FAILED: 'error', DELETED: 'warning' }
      const texts: Record<string, string> = { UPLOADED: '已上传', PROCESSING: '处理中', PARSED: '已解析', EMBEDDED: '已向量化', FAILED: '失败', DELETED: '已删除' }
      return <Tag color={colors[s] ?? 'default'}>{texts[s] ?? s}</Tag>
    }},
    { title: '操作', key: 'actions', width: 150, render: (_, record) => (
      <Space>
        <Button type="link" size="small" icon={<EyeOutlined />}>预览</Button>
        <Popconfirm title="确定删除?" onConfirm={() => deleteDoc(record.id)}>
          <Button type="link" size="small" danger icon={<DeleteOutlined />}>删除</Button>
        </Popconfirm>
      </Space>
    )}
  ]

  return (
    <>
      <div className="mb-4">
        <Button type="primary" icon={<UploadOutlined />} onClick={() => setUploadOpen(true)}>
          上传文档
        </Button>
      </div>
      <Table
        columns={columns}
        dataSource={data?.data.records}
        rowKey="id"
        loading={isLoading}
        pagination={{
          current: page,
          pageSize,
          total: data?.data.total,
          onChange: (p, ps) => { setPage(p); setPageSize(ps) }
        }}
      />
      <DocumentUpload open={uploadOpen} onClose={() => setUploadOpen(false)} />
    </>
  )
}

export default DocumentList
