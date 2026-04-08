import apiClient from './client'
import type { Result } from '@/types/api'
import type { TokenCountResponse, SessionTokenStats, TokenTrend } from '@/types/token'

export const tokensApi = {
  // 计算文本Token数
  countTokens: (text: string, model?: string): Promise<Result<TokenCountResponse>> =>
    apiClient.post('/api/v1/tokens/count', text, {
      params: { model: model || 'qwen-plus' }
    }),

  // 获取会话Token统计
  getSessionStats: (sessionId: string): Promise<Result<SessionTokenStats>> =>
    apiClient.get(`/api/v1/tokens/session/${sessionId}`),

  // 获取实时Token信息
  getRealtimeStats: (sessionId: string): Promise<Result<SessionTokenStats>> =>
    apiClient.get(`/api/v1/tokens/session/${sessionId}/realtime`),

  // 获取使用趋势
  getTrend: (days?: number): Promise<Result<TokenTrend[]>> =>
    apiClient.get('/api/v1/tokens/trend', {
      params: { days: days || 7 }
    })
}
