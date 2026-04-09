import apiClient from './client'

export interface VectorSearchRequest {
  queryVector: number[]
  topK?: number
}

export interface IndexRecommendationResponse {
  success: boolean
  data?: {
    indexType: string
    indexParams: Record<string, unknown>
    searchParams?: Record<string, unknown>
    reason?: string
  }
  message?: string
}

export const milvusApi = {
  getStats: () => apiClient.get('/api/v1/vectors/stats'),
  health: () => apiClient.get('/api/v1/vectors/health'),
  similaritySearch: (payload: VectorSearchRequest) => apiClient.post('/api/v1/vectors/search', payload),
  getIndexRecommendation: (collectionName: string, dimension = 1536): Promise<IndexRecommendationResponse> =>
    apiClient.get('/api/index/recommendation', { params: { collectionName, dimension } })
}
