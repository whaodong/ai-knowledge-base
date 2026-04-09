// 增强版文档列表组件 - 支持批量选择、详情弹窗、批量删除
import { useState, useMemo } from 'react'
import { useDocuments, useDocumentMutations } from '@/hooks/useDocuments'
import DocumentUpload from './DocumentUpload'
import DocumentDetail from './DocumentDetail'
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

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDate = (dateStr: string): string => {
  if (!dateStr) return '-'
  return new Date(dateStr).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const DocumentList = () => {
  const [page, setPage] = useState(1)
  const [pageSize] = useState(10)
  const [uploadOpen, setUploadOpen] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const [selectedDocId, setSelectedDocId] = useState<number | null>(null)
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())
  const [selectAll, setSelectAll] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>('')

  const { data, isLoading, isError, error, refetch } = useDocuments({ 
    pageNum: page, 
    pageSize, 
    sortOrder: 'DESC',
    fileName: searchKeyword || undefined,
    status: statusFilter || undefined
  })
  
  const { delete: deleteDoc, batchDelete, isDeleting } = useDocumentMutations()

  const records = data?.data.records ?? []
  const total = data?.data.total ?? 0

  // 全选逻辑
  const currentPageIds = useMemo(() => new Set(records.map((d: Document) => d.id)), [records])
  
  const handleSelectAll = () => {
    if (selectAll) {
      setSelectedIds(new Set())
    } else {
      setSelectedIds(currentPageIds)
    }
    setSelectAll(!selectAll)
  }

  const handleSelectOne = (id: number) => {
    const newSet = new Set(selectedIds)
    if (newSet.has(id)) {
      newSet.delete(id)
    } else {
      newSet.add(id)
    }
    setSelectedIds(newSet)
    setSelectAll(newSet.size === records.length)
  }

  const handleDelete = (id: number) => {
    if (confirm('确定删除此文档？')) {
      deleteDoc(id)
    }
  }

  const handleBatchDelete = () => {
    if (selectedIds.size === 0) return
    if (confirm(`确定删除选中的 ${selectedIds.size} 个文档？`)) {
      batchDelete(Array.from(selectedIds))
      setSelectedIds(new Set())
      setSelectAll(false)
    }
  }

  const handleViewDetail = (id: number) => {
    setSelectedDocId(id)
    setDetailOpen(true)
  }

  const handleUploadComplete = () => {
    refetch()
  }

  return (
    <>
      <div className="mb-4 flex items-center justify-between gap-4">
        {/* Left actions */}
        <div className="flex items-center gap-2">
          <button
            onClick={() => setUploadOpen(true)}
            className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors flex items-center gap-2"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-8l-4-4m0 0L8 8m4-4v12" />
            </svg>
            上传文档
          </button>
          
          {selectedIds.size > 0 && (
            <button
              onClick={handleBatchDelete}
              disabled={isDeleting}
              className="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors flex items-center gap-2 disabled:opacity-50"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
              批量删除 ({selectedIds.size})
            </button>
          )}
        </div>

        {/* Right filters */}
        <div className="flex items-center gap-2">
          <div className="relative">
            <input
              type="text"
              placeholder="搜索文件名..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              className="pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg 
                         bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                         focus:ring-2 focus:ring-blue-500 focus:border-transparent w-48"
            />
            <svg className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg 
                       bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                       focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="">全部状态</option>
            <option value="UPLOADED">已上传</option>
            <option value="PROCESSING">处理中</option>
            <option value="PARSED">已解析</option>
            <option value="EMBEDDED">已向量化</option>
            <option value="FAILED">失败</option>
          </select>

          <button
            onClick={() => refetch()}
            className="p-2 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-700"
            title="刷新"
          >
            <svg className={`w-5 h-5 text-gray-500 ${isLoading ? 'animate-spin' : ''}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
            </svg>
          </button>
        </div>
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
                  <th className="px-4 py-3 text-left w-10">
                    <input
                      type="checkbox"
                      checked={selectAll}
                      onChange={handleSelectAll}
                      className="w-4 h-4 text-blue-500 border-gray-300 rounded focus:ring-blue-500"
                    />
                  </th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400">文件名</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-20">类型</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">大小</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-24">状态</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-32">创建时间</th>
                  <th className="px-4 py-3 text-left text-sm font-medium text-gray-500 dark:text-gray-400 w-28">操作</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                {records.map((doc: Document) => (
                  <tr 
                    key={doc.id} 
                    className={`hover:bg-gray-50 dark:hover:bg-gray-700/50 ${selectedIds.has(doc.id) ? 'bg-blue-50 dark:bg-blue-900/10' : ''}`}
                  >
                    <td className="px-4 py-3">
                      <input
                        type="checkbox"
                        checked={selectedIds.has(doc.id)}
                        onChange={() => handleSelectOne(doc.id)}
                        className="w-4 h-4 text-blue-500 border-gray-300 rounded focus:ring-blue-500"
                      />
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        <svg className="w-5 h-5 text-gray-400 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                        </svg>
                        <span className="text-sm text-gray-900 dark:text-gray-100 truncate max-w-xs" title={doc.originalFileName}>
                          {doc.originalFileName}
                        </span>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <span className="px-2 py-1 bg-gray-100 dark:bg-gray-700 rounded text-gray-600 dark:text-gray-400">
                        {String(doc.fileType || '').toUpperCase()}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600 dark:text-gray-400">
                      {formatFileSize(doc.fileSize)}
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <span className={`px-2 py-1 rounded text-xs ${statusConfig[doc.status || '']?.color || 'bg-gray-100 text-gray-700'}`}>
                        {statusConfig[doc.status || '']?.text || doc.status}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">
                      {formatDate(doc.createTime)}
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <div className="flex items-center gap-2">
                        <button
                          onClick={() => handleViewDetail(doc.id)}
                          className="text-blue-500 hover:text-blue-600 p-1"
                          title="查看详情"
                        >
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                          </svg>
                        </button>
                        <button
                          onClick={() => handleDelete(doc.id)}
                          className="text-red-500 hover:text-red-600 p-1"
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
                共 {total} 条记录，已选择 {selectedIds.size} 项
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

      <DocumentUpload open={uploadOpen} onClose={() => setUploadOpen(false)} onUploadComplete={handleUploadComplete} />
      <DocumentDetail documentId={selectedDocId} open={detailOpen} onClose={() => setDetailOpen(false)} />
    </>
  )
}

export default DocumentList
