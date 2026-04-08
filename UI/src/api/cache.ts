import apiClient from './client'
import type { Result, PageResponse } from '@/types/api'
import type {
  CacheEntry,
  CacheStats,
  CacheQueryParams,
  CacheWarmupRequest,
  ClearCacheRequest,
  CacheConfig
} from '@/types/cache'

export const cacheApi = {
  // 获取缓存统计
  getCacheStats: (): Promise<Result<CacheStats>> =>
    apiClient.get('/api/v1/cache/stats'),

  // 获取缓存键列表（分页）
  getCacheKeys: (params: CacheQueryParams): Promise<Result<PageResponse<CacheEntry>>> =>
    apiClient.get('/api/v1/cache/keys', { params }),

  // 获取单个缓存值
  getCacheValue: (key: string): Promise<Result<CacheEntry>> =>
    apiClient.get(`/api/v1/cache/keys/${encodeURIComponent(key)}`),

  // 清除缓存
  clearCache: (request: ClearCacheRequest): Promise<Result<{ clearedCount: number }>> =>
    apiClient.post('/api/v1/cache/clear', request),

  // 清除单个缓存
  deleteCache: (key: string): Promise<Result<void>> =>
    apiClient.delete(`/api/v1/cache/keys/${encodeURIComponent(key)}`),

  // 批量删除缓存
  batchDeleteCache: (keys: string[]): Promise<Result<{ deletedCount: number }>> =>
    apiClient.delete('/api/v1/cache/keys/batch', { data: keys }),

  // 缓存预热
  warmupCache: (request: CacheWarmupRequest): Promise<Result<{ warmedKeys: number }>> =>
    apiClient.post('/api/v1/cache/warmup', request),

  // 获取缓存配置
  getCacheConfig: (): Promise<Result<CacheConfig[]>> =>
    apiClient.get('/api/v1/cache/config'),

  // 更新缓存配置
  updateCacheConfig: (configs: CacheConfig[]): Promise<Result<void>> =>
    apiClient.put('/api/v1/cache/config', configs)
}
