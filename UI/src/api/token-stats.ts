import apiClient from './client'
import type { Result } from '@/types/api'
import type { TokenStatsParams, TokenStatsResponse, TokenStatsOverview } from '@/types/token-stats'

export const tokenStatsApi = {
  // 获取Token统计
  getStats: (params: TokenStatsParams): Promise<Result<TokenStatsResponse>> =>
    apiClient.get('/api/v1/tokens/stats', { params }),

  // 获取Token统计概览
  getOverview: (timeRange: 'today' | 'week' | 'month'): Promise<Result<TokenStatsOverview>> =>
    apiClient.get('/api/v1/tokens/stats/overview', {
      params: { timeRange }
    }),

  // 获取Token使用趋势
  getTrend: (params: TokenStatsParams): Promise<Result<TokenStatsResponse['trends']>> =>
    apiClient.get('/api/v1/tokens/stats/trend', { params }),

  // 获取服务类型统计
  getServiceStats: (params: TokenStatsParams): Promise<Result<TokenStatsResponse['serviceStats']>> =>
    apiClient.get('/api/v1/tokens/stats/service', { params }),

  // 获取Top消费者
  getTopConsumers: (params: TokenStatsParams & { limit?: number }): Promise<Result<TokenStatsResponse['topConsumers']>> =>
    apiClient.get('/api/v1/tokens/stats/top', { params })
}
