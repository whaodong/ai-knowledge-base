import type { PageRequest } from '@/types/common'

export interface Document {
  id: number
  fileName: string
  originalFileName: string
  filePath: string
  fileType: string
  fileSize: number
  checksum: string
  status: 'UPLOADED' | 'PROCESSING' | 'PARSED' | 'EMBEDDED' | 'FAILED' | 'DELETED'
  createTime: string
  updateTime: string
  metadata?: string
}

export interface DocumentQueryParams extends PageRequest {
  fileName?: string
  fileType?: string
  status?: string
  startTime?: string
  endTime?: string
}

export interface DocumentUploadRequest {
  metadata?: string
  parseContent?: boolean
  generateEmbedding?: boolean
}

export interface DocumentBatchUploadResponse {
  total: number
  successCount: number
  failedCount: number
  successIds: number[]
  failedFiles: string[]
  errorMessages: string[]
}

export const FILE_TYPE_MAP: Record<string, string> = {
  'pdf': 'PDF',
  'doc': 'Word',
  'docx': 'Word',
  'txt': '文本',
  'md': 'Markdown',
  'json': 'JSON'
}

export const DOCUMENT_STATUS_MAP: Record<string, { text: string; color: string }> = {
  'UPLOADED': { text: '已上传', color: 'default' },
  'PROCESSING': { text: '处理中', color: 'processing' },
  'PARSED': { text: '已解析', color: 'cyan' },
  'EMBEDDED': { text: '已向量化', color: 'success' },
  'FAILED': { text: '失败', color: 'error' },
  'DELETED': { text: '已删除', color: 'warning' }
}

// 文档分块
export interface DocumentChunk {
  id: string
  documentId: number
  content: string
  chunkIndex: number
  tokenCount?: number
  metadata?: Record<string, unknown>
  createdAt: string
}
