import apiClient from './client'
import type { Result, PageResponse } from '@/types/api'
import type { Document, DocumentQueryParams, DocumentBatchUploadResponse, DocumentChunk } from '@/types/document'

export const documentsApi = {
  // 获取文档列表（分页）
  getDocuments: (params: DocumentQueryParams): Promise<Result<PageResponse<Document>>> =>
    apiClient.get('/api/v1/documents', { params }),

  // 上传单个文档
  uploadDocument: (file: File, title?: string): Promise<Result<Document>> => {
    const formData = new FormData()
    formData.append('file', file)
    if (title) formData.append('title', title)
    return apiClient.post('/api/v1/documents', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 批量上传文档
  batchUploadDocuments: (files: File[]): Promise<Result<DocumentBatchUploadResponse>> => {
    const formData = new FormData()
    files.forEach(file => formData.append('files', file))
    return apiClient.post('/api/v1/documents/batch', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // 获取文档详情
  getDocument: (id: number): Promise<Result<Document>> =>
    apiClient.get(`/api/v1/documents/${id}`),

  // 获取文档分块列表
  getDocumentChunks: (id: number): Promise<Result<DocumentChunk[]>> =>
    apiClient.get(`/api/v1/documents/${id}/chunks`),

  // 删除文档
  deleteDocument: (id: number): Promise<Result<void>> =>
    apiClient.delete(`/api/v1/documents/${id}`),

  // 批量删除文档
  batchDeleteDocuments: (ids: number[]): Promise<Result<void>> =>
    apiClient.delete('/api/v1/documents/batch', { data: ids }),

  // 健康检查
  health: (): Promise<Record<string, unknown>> =>
    apiClient.get('/api/v1/documents/health')
}
