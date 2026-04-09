import { useState } from 'react'
import { useDocuments, useDocumentMutations } from '@/hooks/useDocuments'
import DocumentUpload from './DocumentUpload'
import type { Document } from '@/types/document'
import AsyncState from '@/components/common/AsyncState'

const statusConfig: Record<string, { color: string; text: string }> = {
  UPLOADED: { color: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300', text: '已上传' },
  PROCESSING: { color: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400', text: '处理中' },
  PARSED: { color: 'bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-400', text: '已解析' },
  EMBEDDED: { color: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400', text: '已向量化' },
  FAILED: { color: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400', text: '失败' },
  DELETED: { color: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400', text: '已删除' },
}

const DocumentList = () => {
  const [page, setPage] = useState(1)
  const [pageSize] = useState(10)
  const [uploadOpen, setUploadOpen] = useState(false)

  const { data, isLoading, isError, error } = useDocuments({ pageNum: page, pageSize, sortOrder: 'DESC' })
  const { delete: deleteDoc } = useDocumentMutations()

  const records = data?.data.records ?? []
  const total = data?.data.total ?? 0

  const handleDelete = (id: number) => {
    if (confirm('确定删除此文档？')) {
      deleteDoc(id)
    }
  }

  return (
    <>
      <div className="mb-4 flex items-center justify-between">
        <button
          onClick={() => setUploadOpen(true)}
          className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors flex items-center gap-2"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
          </svg>
          上传文档
        </button>
      </div>

      {isLoading || isError || records.length === 0 ? (
        <AsyncState
          loading={isLoading}
          error={isError ? (error as Error).message : null}
          empty={!isLoading && !isError}
          emptyDescription="暂无文档数据"
        />
      ) : (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow border border-gray-200 dark:border-gray-700 overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50 dark:bg-gray-700/50">
                <tr>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">标题</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-20">类型</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">状态</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-32">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {records.map((doc: Document) => (
                  <tr key={doc.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                    <td className="px-4 py-3 text-sm text-gray-900 dark:text-gray-100">
                      {doc.originalFileName}
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <span className="px-2 py-1 bg-gray-100 dark:bg-gray-700 rounded text-gray-600 dark:text-gray-400">
                        {String(doc.fileType || '').toUpperCase()}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <span className={`px-2 py-1 rounded text-xs ${statusConfig[doc.status || '']?.color || 'bg-gray-100 text-gray-700'}`}>
                        {statusConfig[doc.status || '']?.text || doc.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <div className="flex items-center gap-2">
                        <button
                          className="text-blue-500 hover:text-blue-600"
                          title="预览"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                          </svg>
                        </button>
                        <button
                          onClick={() => handleDelete(doc.id)}
                          className="text-red-500 hover:text-red-600"
                          title="删除"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {total > pageSize && (
            <div className="px-4 py-3 border-t border-gray-200 dark:border-gray-700 flex items-center justify-between">
              <div className="text-sm text-gray-500 dark:text-gray-400">
                共 {total} 条记录
              </div>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setPage(p => Math.max(1, p - 1))}
                  disabled={page === 1}
                  className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded-md text-sm
                             disabled:opacity-50 disabled:cursor-not-allowed
                             hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  上一页
                </button>
                <span className="text-sm text-gray-600 dark:text-gray-400">
                  第 {page} / {Math.ceil(total / pageSize)} 页
                </span>
                <button
                  onClick={() => setPage(p => p + 1)}
                  disabled={page >= Math.ceil(total / pageSize)}
                  className="px-3 py-1 border border-gray-300 dark:border-gray-600 rounded-md text-sm
                             disabled:opacity-50 disabled:cursor-not-allowed
                             hover:bg-gray-50 dark:hover:bg-gray-700"
                >
                  下一页
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      <DocumentUpload open={uploadOpen} onClose={() => setUploadOpen(false)} />
    </>
  )
}

export default DocumentList
