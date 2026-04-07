import apiClient from './client'
import type { PaginatedResponse } from '@/types/api'
import type { Document, DocumentQueryParams } from '@/types/document'

export const documentsApi = {
  getDocuments: (params: DocumentQueryParams): Promise<PaginatedResponse<Document>> => 
    apiClient.get('/api/v1/documents', { params }),
  
  uploadDocument: (file: File, title?: string): Promise<Document> => {
    const formData = new FormData()
    formData.append('file', file)
    if (title) formData.append('title', title)
    return apiClient.post('/api/v1/documents/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  
  deleteDocument: (id: string): Promise<void> => 
    apiClient.delete(`/api/v1/documents/${id}`)
}
