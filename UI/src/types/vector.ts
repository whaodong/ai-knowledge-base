// 向量管理相关类型

// 索引类型
export type IndexType = 'FLAT' | 'IVF_FLAT' | 'IVF_SQ8' | 'HNSW' | 'DISKANN'

// 度量类型
export type MetricType = 'L2' | 'IP' | 'COSINE'

// Collection信息
export interface Collection {
  name: string
  dimension: number
  indexType: IndexType
  metricType: MetricType
  vectorCount: number
  totalChunks: number
  usingField: string
  status: 'READY' | 'BUILDING' | 'FAILED'
  createTime: string
  description?: string
}

// Collection详情
export interface CollectionDetail extends Collection {
  segments: SegmentInfo[]
  indexParams: Record<string, string | number>
  fieldSchema: FieldSchema[]
}

// 分段信息
export interface SegmentInfo {
  name: string
  vectorCount: number
  numRows: number
  indexName: string
  state: string
}

// 字段模式
export interface FieldSchema {
  name: string
  type: string
  isPrimary: boolean
  isVector: boolean
  description?: string
}

// Collection统计
export interface CollectionStats {
  totalCollections: number
  totalVectors: number
  totalDimension: number
}

// 索引构建请求
export interface BuildIndexRequest {
  collectionName: string
  fieldName: string
  indexType: IndexType
  metricType?: MetricType
  params?: Record<string, string | number>
}

// 删除Collection请求
export interface DeleteCollectionRequest {
  collectionName: string
  confirmName: string
}
