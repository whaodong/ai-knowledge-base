import apiClient from './client'
import type { Result, PageResponse } from '@/types/api'
import type {
  TokenUsage,
  TokenStatsSummary,
  TokenTrendData,
  ServiceTokenStats,
  TopTokenConsumer,
  TokenQueryParams
} from '@/types/token'

export const tokenApi = {
  // 获取Token统计汇总
  getTokenStatsSummary: (params: TokenQueryParams): Promise<Result<TokenStatsSummary>> =>
    apiClient.get('/api/v1/stats/token/summary', { params }),

  // 获取Token使用趋势
  getTokenTrend: (params: TokenQueryParams): Promise<Result<TokenTrendData[]>> =>
    apiClient.get('/api/v1/stats/token/trend', { params }),

  // 获取服务类型统计
  getServiceTokenStats: (params: TokenQueryParams): Promise<Result<ServiceTokenStats[]>> =>
    apiClient.get('/api/v1/stats/token/service', { params }),

  // 获取Token消耗Top10
  getTopTokenConsumers: (params: TokenQueryParams): Promise<Result<TopTokenConsumer[]>> =>
    apiClient.get('/api/v1/stats/token/top', { params }),

  // 获取Token使用记录（分页）
  getTokenUsageList: (params: TokenQueryParams & { pageNum: number; pageSize: number }): Promise<Result<PageResponse<TokenUsage>>> =>
    apiClient.get('/api/v1/stats/token/usage', { params })
}
