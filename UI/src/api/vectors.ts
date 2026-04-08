import apiClient from './client'
import type { Result, PageResponse } from '@/types/api'
import type { VectorCollection, CollectionQueryParams, CollectionDetail, RebuildIndexRequest } from '@/types/vector'

export const vectorsApi = {
  // 获取集合列表
  getCollections: (params: CollectionQueryParams): Promise<Result<PageResponse<VectorCollection>>> =>
    apiClient.get('/api/v1/vectors/collections', { params }),

  // 获取集合详情
  getCollectionDetail: (name: string): Promise<Result<CollectionDetail>> =>
    apiClient.get(`/api/v1/vectors/collections/${name}`),

  // 删除集合
  deleteCollection: (name: string): Promise<Result<void>> =>
    apiClient.delete(`/api/v1/vectors/collections/${name}`),

  // 重建索引
  rebuildIndex: (request: RebuildIndexRequest): Promise<Result<void>> =>
    apiClient.post('/api/v1/vectors/collections/rebuild-index', request),

  // 获取向量统计
  getStats: (): Promise<Result<{
    totalCollections: number
    totalVectors: number
    totalDimensions: number
  }>> =>
    apiClient.get('/api/v1/vectors/stats'),

  // 加载集合到内存
  loadCollection: (name: string): Promise<Result<void>> =>
    apiClient.post(`/api/v1/vectors/collections/${name}/load`),

  // 释放集合内存
  releaseCollection: (name: string): Promise<Result<void>> =>
    apiClient.post(`/api/v1/vectors/collections/${name}/release`)
}
