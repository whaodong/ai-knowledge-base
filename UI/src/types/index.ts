// 统一响应格式（匹配后端 Result<T>）
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: string
  traceId?: string
}

// 分页请求（匹配后端 PageRequest）
export interface PageRequest {
  pageNum: number
  pageSize: number
  sortBy?: string
  sortOrder?: 'ASC' | 'DESC'
}

// 分页响应（匹配后端 PageResponse<T>）
export interface PageResponse<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

// 登录请求（匹配后端 LoginRequest）
export interface LoginRequest {
  username: string
  password: string
}

// 认证响应（匹配后端 AuthResponse）
export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  username: string
  role: string
}

// 注册请求（匹配后端 RegisterRequest）
export interface RegisterRequest {
  username: string
  password: string
  email?: string
}

// 用户信息
export interface User {
  id: string
  username: string
  email?: string
  role: 'VIEWER' | 'USER' | 'ADMIN'
  createdAt: string
  updatedAt: string
}

// 错误码枚举（匹配后端 ErrorCode）
export enum ErrorCode {
  SUCCESS = 200,
  BAD_REQUEST = 400,
  UNAUTHORIZED = 401,
  FORBIDDEN = 403,
  NOT_FOUND = 404,
  INTERNAL_SERVER_ERROR = 500,
  SERVICE_UNAVAILABLE = 503,
  DOCUMENT_NOT_FOUND = 2001,
  DOCUMENT_UPLOAD_FAILED = 2002,
  DOCUMENT_PARSE_FAILED = 2003,
  EMBEDDING_GENERATION_FAILED = 3001,
  EMBEDDING_TASK_NOT_FOUND = 3002,
  RAG_QUERY_FAILED = 4001,
  RAG_SESSION_NOT_FOUND = 4002,
  MILVUS_CONNECTION_FAILED = 5001,
  MILVUS_SEARCH_FAILED = 5004
}

export type { Document } from './document'
export type { DocumentQueryParams, DocumentUploadRequest, DocumentBatchUploadResponse, DocumentChunk } from './document'
export { FILE_TYPE_MAP, DOCUMENT_STATUS_MAP } from './document'

export type { TokenUsage, TokenStatsSummary, TokenTrendData, ServiceTokenStats, TopTokenConsumer, TimeRange, TokenQueryParams } from './token'

export type { EmbeddingTask, EmbeddingTaskStatus, EmbeddingTaskQueryParams, EmbeddingTaskStats, BatchEmbeddingRequest } from './embedding'
export { EMBEDDING_TASK_STATUS_MAP } from './embedding'

export type { Collection, CollectionDetail, CollectionStats, IndexType, MetricType, SegmentInfo, FieldSchema, BuildIndexRequest, DeleteCollectionRequest } from './vector'

export type { CacheEntry, CacheStats, CacheType, CacheQueryParams, CacheWarmupRequest, ClearCacheRequest, CacheConfig } from './cache'

export type { User as UserInfo, UserStatus, UserRole, UserQueryParams, CreateUserRequest, UpdateUserRequest, ChangePasswordRequest, BatchUserOperationRequest } from './user'
export { USER_ROLE_MAP, USER_STATUS_MAP } from './user'

// 新增模块导出（兼容新目录结构）
export * from '@/types/common'
export * from '@/types/auth'
export * from '@/types/rag'
export * from '@/types/token-stats'
export * from '@/types/embedding-task'
