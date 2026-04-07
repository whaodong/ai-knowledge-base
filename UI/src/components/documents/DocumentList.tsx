import { useState } from 'react'
import { Table, Tag, Button, Space, Modal, Popconfirm } from 'antd'
import { DeleteOutlined, EyeOutlined, UploadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import { useDocuments, useDocumentMutations } from '@/hooks/useDocuments'
import DocumentUpload from './DocumentUpload'
import type { Document } from '@/types/document'

const DocumentList = () => {
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [uploadOpen, setUploadOpen] = useState(false)
  
  const { data, isLoading } = useDocuments({ page, page_size: pageSize })
  const { delete: deleteDoc } = useDocumentMutations()

  const columns: ColumnsType<Document> = [
    { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
    { title: '类型', dataIndex: 'file_type', key: 'file_type', width: 80, render: (t: string) => <Tag>{t.toUpperCase()}</Tag> },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100, render: (s: string) => {
      const colors: Record<string, string> = { pending: 'default', processing: 'processing', completed: 'success', failed: 'error' }
      const texts: Record<string, string> = { pending: '待处理', processing: '处理中', completed: '已完成', failed: '失败' }
      return <Tag color={colors[s]}>{texts[s]}</Tag>
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
        dataSource={data?.items}
        rowKey="id"
        loading={isLoading}
        pagination={{
          current: page,
          pageSize,
          total: data?.total,
          onChange: (p, ps) => { setPage(p); setPageSize(ps) }
        }}
      />
      <DocumentUpload open={uploadOpen} onClose={() => setUploadOpen(false)} />
    </>
  )
}

export default DocumentList
