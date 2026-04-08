// 文档响应（匹配后端 DocumentResponse）
export interface Document {
  id: number
  fileName: string
  originalFileName: string
  filePath: string
  fileType: string
  fileSize: number
  checksum: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  createTime: string
  updateTime: string
  metadata?: string
}

// 文档查询参数（匹配后端 DocumentQueryRequest）
export interface DocumentQueryParams extends PageRequest {
  fileName?: string
  fileType?: string
  status?: string
  startDate?: string
  endDate?: string
}

// 文档上传请求
export interface DocumentUploadRequest {
  title?: string
  description?: string
  tags?: string[]
}

// 文档批量上传响应
export interface DocumentBatchUploadResponse {
  total: number
  successCount: number
  failedCount: number
  results: Array<{
    fileName: string
    success: boolean
    documentId?: number
    error?: string
  }>
}

// 文档分块
export interface DocumentChunk {
  id: string
  documentId: number
  content: string
  chunkIndex: number
  tokenCount: number
  vectorStatus: 'PENDING' | 'COMPLETED' | 'FAILED'
}

// 文件类型映射
export const FILE_TYPE_MAP: Record<string, string> = {
  'pdf': 'PDF',
  'doc': 'Word',
  'docx': 'Word',
  'txt': '文本',
  'md': 'Markdown',
  'json': 'JSON'
}

// 文档状态映射
export const DOCUMENT_STATUS_MAP: Record<string, { text: string; color: string }> = {
  'PENDING': { text: '待处理', color: 'default' },
  'PROCESSING': { text: '处理中', color: 'processing' },
  'COMPLETED': { text: '已完成', color: 'success' },
  'FAILED': { text: '失败', color: 'error' }
}
