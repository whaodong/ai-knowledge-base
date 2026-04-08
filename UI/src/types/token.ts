// Token统计响应
export interface TokenUsageSummary {
  totalTokens: number
  promptTokens: number
  completionTokens: number
  requestCount: number
  avgTokensPerRequest: number
  estimatedCost: number
}

// Token计数请求
export interface TokenCountRequest {
  text: string
  model?: string
}

// Token计数响应
export interface TokenCountResponse {
  text: string
  textLength: number
  tokenCount: number
  modelName: string
  maxContextTokens: number
  usagePercentage: string
}

// 会话Token统计
export interface SessionTokenStats {
  sessionId: string
  totalTokens: number
  promptTokens: number
  completionTokens: number
  messageCount: number
  startTime: string
  endTime?: string
}

// Token使用趋势
export interface TokenTrend {
  date: string
  totalTokens: number
  requestCount: number
  avgTokensPerRequest: number
}
