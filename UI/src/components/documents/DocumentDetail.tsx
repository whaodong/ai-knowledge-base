// 文档详情弹窗组件
import { useEffect, useState } from 'react'
import { documentsApi } from '@/api/documents'
import type { Document, DocumentChunk } from '@/types/document'

interface Props {
  documentId: number | null
  open: boolean
  onClose: () => void
}

const statusConfig: Record<string, { color: string; text: string }> = {
  UPLOADED: { color: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300', text: '已上传' },
  PROCESSING: { color: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400', text: '处理中' },
  PARSED: { color: 'bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-400', text: '已解析' },
  EMBEDDED: { color: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400', text: '已向量化' },
  FAILED: { color: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400', text: '失败' },
  DELETED: { color: 'bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400', text: '已删除' },
}

const DocumentDetail = ({ documentId, open, onClose }: Props) => {
  const [document, setDocument] = useState<Document | null>(null)
  const [chunks, setChunks] = useState<DocumentChunk[]>([])
  const [loading, setLoading] = useState(false)
  const [activeTab, setActiveTab] = useState<'info' | 'chunks'>('info')

  useEffect(() => {
    if (!open || !documentId) return
    
    const fetchData = async () => {
      setLoading(true)
      try {
        const [docRes, chunksRes] = await Promise.all([
          documentsApi.getDocument(documentId),
          documentsApi.getDocumentChunks(documentId)
        ])
        if (docRes.data) {
          setDocument(docRes.data)
        }
        if (chunksRes.data) {
          // 支持两种响应格式：数组或 { chunks: [] } 对象
          if (Array.isArray(chunksRes.data)) {
            setChunks(chunksRes.data)
          } else {
            setChunks((chunksRes.data as { chunks?: DocumentChunk[] }).chunks || [])
          }
        }
      } catch (error) {
        console.error('Failed to fetch document details:', error)
      } finally {
        setLoading(false)
      }
    }
    
    fetchData()
  }, [open, documentId])

  if (!open) return null

  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  const formatDate = (dateStr: string) => {
    if (!dateStr) return '-'
    return new Date(dateStr).toLocaleString('zh-CN')
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />
      <div className="relative bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-3xl mx-4 max-h-[85vh] flex flex-col">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <svg className="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            <h3 className="text-lg font-medium text-gray-900 dark:text-white">文档详情</h3>
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Tabs */}
        <div className="px-6 border-b border-gray-200 dark:border-gray-700">
          <div className="flex gap-4">
            <button
              onClick={() => setActiveTab('info')}
              className={`py-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === 'info'
                  ? 'border-blue-500 text-blue-500'
                  : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
              }`}
            >
              基本信息
            </button>
            <button
              onClick={() => setActiveTab('chunks')}
              className={`py-3 text-sm font-medium border-b-2 transition-colors ${
                activeTab === 'chunks'
                  ? 'border-blue-500 text-blue-500'
                  : 'border-transparent text-gray-500 hover:text-gray-700 dark:hover:text-gray-300'
              }`}
            >
              分块列表 ({chunks.length})
            </button>
          </div>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-auto p-6">
          {loading ? (
            <div className="flex items-center justify-center py-12">
              <div className="w-8 h-8 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
            </div>
          ) : document ? (
            activeTab === 'info' ? (
              <div className="space-y-6">
                {/* Document Info */}
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">文件名</label>
                    <p className="text-gray-900 dark:text-white">{document.originalFileName}</p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">文件类型</label>
                    <p className="text-gray-900 dark:text-white">{document.fileType?.toUpperCase() || '-'}</p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">文件大小</label>
                    <p className="text-gray-900 dark:text-white">{formatFileSize(document.fileSize)}</p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">状态</label>
                    <span className={`inline-block px-2 py-1 rounded text-xs ${statusConfig[document.status]?.color || 'bg-gray-100'}`}>
                      {statusConfig[document.status]?.text || document.status}
                    </span>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">创建时间</label>
                    <p className="text-gray-900 dark:text-white">{formatDate(document.createTime)}</p>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">更新时间</label>
                    <p className="text-gray-900 dark:text-white">{formatDate(document.updateTime)}</p>
                  </div>
                </div>

                {/* Metadata */}
                {document.metadata && (
                  <div>
                    <label className="block text-sm font-medium text-gray-500 dark:text-gray-400 mb-1">元数据</label>
                    <pre className="p-3 bg-gray-50 dark:bg-gray-700 rounded-lg text-sm text-gray-600 dark:text-gray-400 overflow-auto">
                      {typeof document.metadata === 'string' 
                        ? document.metadata 
                        : JSON.stringify(document.metadata, null, 2)}
                    </pre>
                  </div>
                )}

                {/* Vectorization Status */}
                <div className="border-t border-gray-200 dark:border-gray-700 pt-4">
                  <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-3">向量化状态</h4>
                  <div className="flex items-center gap-4">
                    <div className={`w-3 h-3 rounded-full ${
                      document.status === 'EMBEDDED' 
                        ? 'bg-green-500' 
                        : document.status === 'PROCESSING'
                        ? 'bg-yellow-500 animate-pulse'
                        : 'bg-gray-400'
                    }`}></div>
                    <span className="text-sm text-gray-600 dark:text-gray-400">
                      {document.status === 'EMBEDDED' 
                        ? '已完成向量化'
                        : document.status === 'PROCESSING'
                        ? '正在向量化...'
                        : '尚未向量化'}
                    </span>
                  </div>
                </div>
              </div>
            ) : (
              <div className="space-y-4">
                {chunks.length === 0 ? (
                  <div className="text-center py-12">
                    <svg className="w-12 h-12 mx-auto text-gray-400 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
                    </svg>
                    <p className="text-gray-500 dark:text-gray-400">暂无分块数据</p>
                    <p className="text-sm text-gray-400 dark:text-gray-500 mt-1">文档分块将在处理完成后显示</p>
                  </div>
                ) : (
                  chunks.map((chunk, index) => (
                    <div
                      key={chunk.id || index}
                      className="p-4 bg-gray-50 dark:bg-gray-700/50 rounded-lg border border-gray-200 dark:border-gray-600"
                    >
                      <div className="flex items-center justify-between mb-2">
                        <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                          分块 #{chunk.chunkIndex + 1}
                        </span>
                        {chunk.tokenCount && (
                          <span className="text-xs text-gray-500 dark:text-gray-400">
                            {chunk.tokenCount} tokens
                          </span>
                        )}
                      </div>
                      <p className="text-sm text-gray-600 dark:text-gray-400 line-clamp-4">
                        {chunk.content}
                      </p>
                    </div>
                  ))
                )}
              </div>
            )
          ) : (
            <div className="text-center py-12">
              <p className="text-gray-500 dark:text-gray-400">无法加载文档详情</p>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded-lg hover:bg-gray-200 dark:hover:bg-gray-600"
          >
            关闭
          </button>
        </div>
      </div>
    </div>
  )
}

export default DocumentDetail
