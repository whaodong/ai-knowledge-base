import { useState } from 'react'
import { Button, Card, Input, Space, Table, Tag, Typography, Upload, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { UploadFile } from 'antd/es/upload/interface'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AsyncState from '@/components/common/AsyncState'
import { documentsApi } from '@/api/documents'
import type { Document, DocumentQueryParams } from '@/types/document'

const statusColorMap: Record<string, string> = {
  UPLOADED: 'default',
  PROCESSING: 'processing',
  PARSED: 'cyan',
  EMBEDDED: 'success',
  FAILED: 'error',
  DELETED: 'warning'
}

export default function DocumentsPage() {
  const queryClient = useQueryClient()
  const [fileName, setFileName] = useState('')
  const [uploadList, setUploadList] = useState<UploadFile[]>([])

  const params: DocumentQueryParams = {
    pageNum: 1,
    pageSize: 10,
    fileName: fileName || undefined
  }

  const { data, isLoading, isError, error } = useQuery({
    queryKey: ['documents', params],
    queryFn: () => documentsApi.getDocuments(params)
  })

  const uploadMutation = useMutation({
    mutationFn: async () => {
      if (!uploadList.length || !uploadList[0].originFileObj) {
        throw new Error('请先选择文件')
      }
      return documentsApi.uploadDocument(uploadList[0].originFileObj)
    },
    onSuccess: () => {
      message.success('上传成功')
      setUploadList([])
      queryClient.invalidateQueries({ queryKey: ['documents'] })
    },
    onError: (err) => {
      message.error((err as Error).message)
    }
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => documentsApi.deleteDocument(id),
    onSuccess: () => {
      message.success('删除成功')
      queryClient.invalidateQueries({ queryKey: ['documents'] })
    },
    onError: (err) => {
      message.error((err as Error).message)
    }
  })

  const records = data?.data.records ?? []

  const columns: ColumnsType<Document> = [
    { title: 'ID', dataIndex: 'id', width: 90 },
    { title: '文件名', dataIndex: 'fileName' },
    { title: '类型', dataIndex: 'fileType', width: 120 },
    { title: '大小', dataIndex: 'fileSize', width: 140, render: (val: number) => `${val} B` },
    {
      title: '状态',
      dataIndex: 'status',
      width: 120,
      render: (val: string) => <Tag color={statusColorMap[val] ?? 'default'}>{val}</Tag>
    },
    {
      title: '操作',
      width: 140,
      render: (_, row) => (
        <Button danger loading={deleteMutation.isPending} onClick={() => deleteMutation.mutate(row.id)}>
          删除
        </Button>
      )
    }
  ]

  return (
    <Card>
      <Typography.Title level={3}>文档管理</Typography.Title>
      <Space style={{ marginBottom: 16 }}>
        <Input
          placeholder="按文件名过滤"
          value={fileName}
          onChange={(e) => setFileName(e.target.value)}
          style={{ width: 240 }}
        />
        <Upload
          fileList={uploadList}
          beforeUpload={() => false}
          onChange={({ fileList: fl }) => setUploadList(fl.slice(-1))}
          maxCount={1}
        >
          <Button>选择文件</Button>
        </Upload>
        <Button type="primary" loading={uploadMutation.isPending} onClick={() => uploadMutation.mutate()}>
          上传
        </Button>
      </Space>

      {isLoading || isError || records.length === 0 ? (
        <AsyncState
          loading={isLoading}
          error={isError ? (error as Error).message : null}
          empty={!isLoading && !isError}
          emptyDescription="暂无文档数据"
        />
      ) : (
        <Table rowKey="id" columns={columns} dataSource={records} pagination={false} />
      )}
    </Card>
  )
}
