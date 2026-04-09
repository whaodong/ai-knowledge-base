// 增强版文档上传组件 - 支持批量上传和进度条
import { useState, useRef, useCallback } from 'react'
import { useDocumentMutations } from '@/hooks/useDocuments'

interface UploadFile {
  id: string
  file: File
  progress: number
  status: 'pending' | 'uploading' | 'success' | 'error'
  error?: string
}

interface Props {
  open: boolean
  onClose: () => void
  onUploadComplete?: () => void
}

const ACCEPTED_TYPES = [
  'application/pdf',
  'text/plain',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/markdown',
  'application/json'
]

const ACCEPTED_EXTENSIONS = ['.pdf', '.txt', '.docx', '.md', '.json']

const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const DocumentUpload = ({ open, onClose, onUploadComplete }: Props) => {
  const [fileList, setFileList] = useState<UploadFile[]>([])
  const [isDragging, setIsDragging] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)
  const { uploadWithProgress } = useDocumentMutations()

  const validateFile = (file: File): boolean => {
    const ext = '.' + file.name.split('.').pop()?.toLowerCase()
    return ACCEPTED_TYPES.includes(file.type) || ACCEPTED_EXTENSIONS.includes(ext)
  }

  const handleFileSelect = useCallback((files: FileList | null) => {
    if (!files) return
    const newFiles: UploadFile[] = []
    
    Array.from(files).forEach(file => {
      if (validateFile(file)) {
        newFiles.push({
          id: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
          file,
          progress: 0,
          status: 'pending'
        })
      }
    })
    
    setFileList(prev => [...prev, ...newFiles])
  }, [])

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(false)
    handleFileSelect(e.dataTransfer.files)
  }, [handleFileSelect])

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    setIsDragging(true)
  }, [])

  const handleDragLeave = useCallback(() => {
    setIsDragging(false)
  }, [])

  const handleRemove = (id: string) => {
    setFileList(prev => prev.filter(f => f.id !== id))
  }

  const handleUpload = async () => {
    if (fileList.length === 0) return

    // 更新状态为上传中
    setFileList(prev => prev.map(f => ({ ...f, status: 'uploading' as const })))

    for (const uploadFile of fileList) {
      if (uploadFile.status === 'success') continue

      try {
        await uploadWithProgress(uploadFile.file, (progress) => {
          setFileList(prev => prev.map(f => 
            f.id === uploadFile.id ? { ...f, progress } : f
          ))
        })
        
        setFileList(prev => prev.map(f => 
          f.id === uploadFile.id ? { ...f, status: 'success' as const, progress: 100 } : f
        ))
      } catch (error) {
        setFileList(prev => prev.map(f => 
          f.id === uploadFile.id ? { 
            ...f, 
            status: 'error' as const, 
            error: error instanceof Error ? error.message : '上传失败' 
          } : f
        ))
      }
    }

    // 触发完成回调
    onUploadComplete?.()
  }

  const handleClearAll = () => {
    setFileList([])
  }

  const allSuccess = fileList.length > 0 && fileList.every(f => f.status === 'success')
  const hasUploading = fileList.some(f => f.status === 'uploading')
  const canUpload = fileList.length > 0 && !hasUploading

  if (!open) return null

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />
      <div className="relative bg-white dark:bg-gray-800 rounded-lg shadow-xl w-full max-w-2xl mx-4 max-h-[85vh] flex flex-col">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <svg className="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
            </svg>
            <h3 className="text-lg font-medium text-gray-900 dark:text-white">上传文档</h3>
            {fileList.length > 0 && (
              <span className="px-2 py-0.5 bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 text-xs rounded-full">
                {fileList.length} 个文件
              </span>
            )}
          </div>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        {/* Drop zone */}
        <div className="p-6">
          <div
            onDrop={handleDrop}
            onDragOver={handleDragOver}
            onDragLeave={handleDragLeave}
            onClick={() => fileInputRef.current?.click()}
            className={`
              border-2 border-dashed rounded-lg p-8 text-center cursor-pointer transition-colors
              ${isDragging
                ? 'border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                : 'border-gray-300 dark:border-gray-600 hover:border-blue-400 dark:hover:border-blue-500'
              }
            `}
          >
            <input
              ref={fileInputRef}
              type="file"
              multiple
              accept=".pdf,.txt,.docx,.md,.json"
              onChange={(e) => handleFileSelect(e.target.files)}
              className="hidden"
            />
            <svg className="w-12 h-12 mx-auto mb-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
            </svg>
            <p className="text-gray-600 dark:text-gray-400 mb-1">点击或拖拽文件上传</p>
            <p className="text-sm text-gray-400 dark:text-gray-500">支持 PDF、TXT、Word、Markdown、JSON</p>
          </div>
        </div>

        {/* File list */}
        {fileList.length > 0 && (
          <div className="px-6 pb-4 max-h-64 overflow-auto">
            <div className="flex items-center justify-between mb-2">
              <span className="text-sm font-medium text-gray-700 dark:text-gray-300">文件列表</span>
              <button
                onClick={handleClearAll}
                className="text-xs text-gray-500 hover:text-red-500 dark:text-gray-400 dark:hover:text-red-400"
              >
                清空列表
              </button>
            </div>
            <div className="space-y-2">
              {fileList.map((uploadFile) => (
                <div 
                  key={uploadFile.id} 
                  className="flex items-center gap-3 p-3 bg-gray-50 dark:bg-gray-700/50 rounded-lg"
                >
                  {/* File icon */}
                  <div className="flex-shrink-0">
                    {uploadFile.status === 'success' ? (
                      <svg className="w-5 h-5 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                      </svg>
                    ) : uploadFile.status === 'error' ? (
                      <svg className="w-5 h-5 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    ) : (
                      <svg className="w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
                      </svg>
                    )}
                  </div>

                  {/* File info */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center justify-between">
                      <span className="text-sm text-gray-700 dark:text-gray-300 truncate">
                        {uploadFile.file.name}
                      </span>
                      <span className="text-xs text-gray-500 dark:text-gray-400 ml-2 flex-shrink-0">
                        {formatFileSize(uploadFile.file.size)}
                      </span>
                    </div>
                    
                    {/* Progress bar */}
                    {(uploadFile.status === 'uploading' || uploadFile.progress > 0) && (
                      <div className="mt-2">
                        <div className="h-1.5 bg-gray-200 dark:bg-gray-600 rounded-full overflow-hidden">
                          <div
                            className={`h-full transition-all duration-300 ${
                              uploadFile.status === 'error'
                                ? 'bg-red-500'
                                : uploadFile.status === 'success'
                                ? 'bg-green-500'
                                : 'bg-blue-500'
                            }`}
                            style={{ width: `${uploadFile.progress}%` }}
                          />
                        </div>
                        <div className="flex justify-between mt-1">
                          <span className="text-xs text-gray-500 dark:text-gray-400">
                            {uploadFile.status === 'error' 
                              ? uploadFile.error 
                              : `${uploadFile.progress}%`}
                          </span>
                        </div>
                      </div>
                    )}
                  </div>

                  {/* Remove button */}
                  {uploadFile.status !== 'uploading' && (
                    <button
                      onClick={() => handleRemove(uploadFile.id)}
                      className="p-1 text-gray-400 hover:text-red-500 flex-shrink-0"
                    >
                      <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                      </svg>
                    </button>
                  )}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Footer */}
        <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-between items-center">
          <div className="text-sm text-gray-500 dark:text-gray-400">
            {fileList.length > 0 && (
              <>
                待上传: {fileList.filter(f => f.status === 'pending').length} | 
                成功: {fileList.filter(f => f.status === 'success').length} | 
                失败: {fileList.filter(f => f.status === 'error').length}
              </>
            )}
          </div>
          <div className="flex gap-3">
            <button
              onClick={() => { handleClearAll(); onClose() }}
              className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700"
            >
              {allSuccess ? '完成' : '取消'}
            </button>
            {!allSuccess && (
              <button
                onClick={handleUpload}
                disabled={!canUpload}
                className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
              >
                {hasUploading && (
                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                )}
                {hasUploading ? '上传中...' : '开始上传'}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default DocumentUpload
