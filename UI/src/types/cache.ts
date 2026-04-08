// 缓存管理相关类型

// 缓存条目
export interface CacheEntry {
  key: string
  value: string
  size: number
  hitCount: number
  lastAccessTime: string
  createTime: string
  ttl: number
  expired: boolean
}

// 缓存统计
export interface CacheStats {
  hitCount: number
  missCount: number
  hitRate: number
  totalKeys: number
  totalSize: number
  avgTtl: number
  memoryUsed: number
  memoryLimit: number
}

// 缓存类型
export type CacheType = 'EMBEDDING' | 'QUERY' | 'DOCUMENT' | 'VECTOR' | 'ALL'

// 缓存键列表查询参数
export interface CacheQueryParams {
  pageNum: number
  pageSize: number
  cacheType?: CacheType
  keyPrefix?: string
  searchKey?: string
}

// 缓存预热请求
export interface CacheWarmupRequest {
  cacheType: CacheType
  keys?: string[]
  priority?: 'HIGH' | 'NORMAL' | 'LOW'
}

// 清除缓存请求
export interface ClearCacheRequest {
  cacheType: CacheType
  keyPattern?: string
  confirmClear: boolean
}

// 缓存配置
export interface CacheConfig {
  type: CacheType
  maxSize: number
  ttl: number
  enabled: boolean
  evictionPolicy: 'LRU' | 'LFU' | 'FIFO'
}
