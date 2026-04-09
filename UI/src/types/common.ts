export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp: string
  traceId?: string
}

export interface PageRequest {
  pageNum: number
  pageSize: number
  sortBy?: string
  sortOrder?: 'ASC' | 'DESC'
}

export interface PageResponse<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  totalPages: number
  hasNext: boolean
  hasPrevious: boolean
}

export enum ErrorCode {
  SUCCESS = 200,
  BAD_REQUEST = 400,
  UNAUTHORIZED = 401,
  FORBIDDEN = 403,
  NOT_FOUND = 404,
  METHOD_NOT_ALLOWED = 405,
  CONFLICT = 409,
  INTERNAL_SERVER_ERROR = 500,
  SERVICE_UNAVAILABLE = 503
}
