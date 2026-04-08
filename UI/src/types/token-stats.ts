// Token统计类型定义

// Token统计请求参数
export interface TokenStatsParams {
  timeRange: 'today' | 'week' | 'month'
  serviceType?: 'chat' | 'embedding' | 'all'
}

// Token统计概览
export interface TokenStatsOverview {
  totalTokens: number
  dailyAverage: number
  peakUsage: number
  peakDate: string
  totalRequests: number
  avgTokensPerRequest: number
  estimatedCost: number
}

// 服务类型Token统计
export interface ServiceTokenStats {
  serviceType: 'chat' | 'embedding' | 'other'
  tokenCount: number
  requestCount: number
  avgTokensPerRequest: number
  percentage: number
}

// Token趋势数据
export interface TokenTrendData {
  date: string
  totalTokens: number
  chatTokens: number
  embeddingTokens: number
  requestCount: number
}

// Token消耗Top用户/文档
export interface TokenConsumption {
  id: number
  name: string
  type: 'user' | 'document'
  tokenCount: number
  requestCount: number
  lastUsedAt: string
}

// Token统计响应
export interface TokenStatsResponse {
  overview: TokenStatsOverview
  trends: TokenTrendData[]
  serviceStats: ServiceTokenStats[]
  topConsumers: TokenConsumption[]
}
