import apiClient from './client'
import type { Result, PageResponse } from '@/types/api'
import type {
  Collection,
  CollectionDetail,
  CollectionStats,
  BuildIndexRequest,
  DeleteCollectionRequest
} from '@/types/vector'

export const vectorApi = {
  // 获取Collection列表
  getCollections: (): Promise<Result<Collection[]>> =>
    apiClient.get('/api/v1/milvus/collections'),

  // 获取Collection详情
  getCollectionDetail: (name: string): Promise<Result<CollectionDetail>> =>
    apiClient.get(`/api/v1/milvus/collections/${name}`),

  // 获取Collection统计
  getCollectionStats: (): Promise<Result<CollectionStats>> =>
    apiClient.get('/api/v1/milvus/stats'),

  // 获取Collection数量
  getCollectionCount: (name: string): Promise<Result<{ count: number }>> =>
    apiClient.get(`/api/v1/milvus/collections/${name}/count`),

  // 搜索向量
  searchVectors: (params: {
    collectionName: string
    vector: number[]
    topK?: number
  }): Promise<Result<{ results: Array<{ id: string; score: number }> }>> =>
    apiClient.post('/api/v1/milvus/search', params),

  // 删除Collection
  deleteCollection: (request: DeleteCollectionRequest): Promise<Result<void>> =>
    apiClient.delete(`/api/v1/milvus/collections/${request.collectionName}`),

  // 构建索引
  buildIndex: (request: BuildIndexRequest): Promise<Result<{ taskId: string }>> =>
    apiClient.post('/api/v1/milvus/collections/index', request),

  // 获取索引状态
  getIndexStatus: (collectionName: string): Promise<Result<{ status: string; progress: number }>> =>
    apiClient.get(`/api/v1/milvus/collections/${collectionName}/index/status`),

  // 查询向量列表（分页）
  queryVectors: (params: {
    collectionName: string
    pageNum: number
    pageSize: number
  }): Promise<Result<PageResponse<Record<string, unknown>>>> =>
    apiClient.get(`/api/v1/milvus/collections/${params.collectionName}/vectors`, { params })
}
