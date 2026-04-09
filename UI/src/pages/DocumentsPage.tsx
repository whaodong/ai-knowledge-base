import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import AsyncState from '@/components/common/AsyncState'
import { documentsApi } from '@/api/documents'
import type { Document, DocumentQueryParams } from '@/types/document'

const statusColorMap: Record<string, string> = {
  UPLOADED: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300',
  PROCESSING: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400',
  PARSED: 'bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-400',
  EMBEDDED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
  FAILED: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400',
  DELETED: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400'
}

export default function DocumentsPage() {
  const queryClient = useQueryClient()
  const [fileName, setFileName] = useState('')
  const [uploadList, setUploadList] = useState<File[]>([])

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
      if (!uploadList.length) {
        throw new Error('请先选择文件')
      }
      return documentsApi.uploadDocument(uploadList[0])
    },
    onSuccess: () => {
      alert('上传成功')
      setUploadList([])
      queryClient.invalidateQueries({ queryKey: ['documents'] })
    },
    onError: (err: Error) => {
      alert(err.message)
    }
  })

  const deleteMutation = useMutation({
    mutationFn: (id: number) => documentsApi.deleteDocument(id),
    onSuccess: () => {
      alert('删除成功')
      queryClient.invalidateQueries({ queryKey: ['documents'] })
    },
    onError: (err: Error) => {
      alert(err.message)
    }
  })

  const records = data?.data.records ?? []

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files
    if (files) {
      setUploadList(Array.from(files).slice(0, 1))
    }
  }

  return (
    <div className="space-y-4">
      <h1 className="text-2xl font-bold text-gray-800 dark:text-white">文档管理</h1>

      {/* Toolbar */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow p-4 border border-gray-200 dark:border-gray-700">
        <div className="flex flex-wrap items-center gap-4">
          <input
            type="text"
            placeholder="按文件名过滤"
            value={fileName}
            onChange={(e) => setFileName(e.target.value)}
            className="px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                       focus:ring-2 focus:ring-blue-500 focus:border-transparent
                       w-60"
          />

          <label className="px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 
                           rounded-lg cursor-pointer hover:bg-gray-200 dark:hover:bg-gray-600">
            <input
              type="file"
              className="hidden"
              onChange={handleFileChange}
              accept=".pdf,.txt,.docx,.md"
            />
            选择文件
          </label>
          {uploadList.length > 0 && (
            <span className="text-sm text-gray-500 dark:text-gray-400">
              已选择: {uploadList[0].name}
            </span>
          )}

          <button
            onClick={() => uploadMutation.mutate()}
            disabled={uploadMutation.isPending || uploadList.length === 0}
            className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600
                       disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            {uploadMutation.isPending ? '上传中...' : '上传'}
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
        {isLoading || isError || records.length === 0 ? (
          <AsyncState
            loading={isLoading}
            error={isError ? (error as Error).message : null}
            empty={!isLoading && !isError}
            emptyDescription="暂无文档数据"
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700/50">
                <tr>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">ID</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">文件名</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">类型</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-28">大小</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-28">状态</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-28">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {records.map((record: Document) => (
                  <tr key={record.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-gray-100">{record.id}</td>
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-gray-100">{record.fileName}</td>
                    <td className="px-4 py-3 text-sm">
                      <span className="px-2 py-1 bg-gray-100 dark:bg-gray-700 rounded text-gray-600 dark:text-gray-400">
                        {record.fileType}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">
                      {record.fileSize} B
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <span className={`px-2 py-1 rounded text-xs ${statusColorMap[record.status || ''] || 'bg-gray-100'}`}>
                        {record.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <button
                        onClick={() => {
                          if (confirm('确定删除此文档？')) {
                            deleteMutation.mutate(record.id)
                          }
                        }}
                        disabled={deleteMutation.isPending}
                        className="text-red-500 hover:text-red-600 disabled:opacity-50"
                      >
                        删除
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
