// Token统计相关类型

// Token使用记录
export interface TokenUsage {
  id: string
  username: string
  serviceType: 'DOCUMENT' | 'EMBEDDING' | 'RAG' | 'SEARCH'
  inputTokens: number
  outputTokens: number
  totalTokens: number
  model: string
  cost: number
  createTime: string
}

// Token统计汇总
export interface TokenStatsSummary {
  totalTokens: number
  inputTokens: number
  outputTokens: number
  totalCost: number
  avgDailyTokens: number
  peakDailyTokens: number
  totalRequests: number
}

// Token使用趋势数据
export interface TokenTrendData {
  date: string
  totalTokens: number
  inputTokens: number
  outputTokens: number
  requestCount: number
}

// 服务类型统计
export interface ServiceTokenStats {
  serviceType: string
  totalTokens: number
  requestCount: number
  avgTokensPerRequest: number
  cost: number
}

// Token消耗Top10
export interface TopTokenConsumer {
  username: string
  totalTokens: number
  requestCount: number
  avgTokensPerRequest: number
}

// 时间范围筛选
export type TimeRange = 'today' | 'week' | 'month' | 'custom'

// Token查询参数
export interface TokenQueryParams {
  timeRange: TimeRange
  startDate?: string
  endDate?: string
  serviceType?: string
  username?: string
}
