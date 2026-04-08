import apiClient from './client'
import type { Result } from '@/types/api'
import type { LoginRequest, AuthResponse, RegisterRequest, User } from '@/types/api'

export const authApi = {
  // 登录
  login: (data: LoginRequest): Promise<Result<AuthResponse>> =>
    apiClient.post('/api/auth/login', data),

  // 注册
  register: (data: RegisterRequest): Promise<Result<User>> =>
    apiClient.post('/api/auth/register', data),

  // 刷新Token
  refreshToken: (refreshToken: string): Promise<Result<AuthResponse>> =>
    apiClient.post('/api/auth/refresh', { refreshToken }),

  // 获取当前用户
  getCurrentUser: (): Promise<Result<User>> =>
    apiClient.get('/api/auth/me'),

  // 登出
  logout: (): Promise<Result<void>> =>
    apiClient.post('/api/auth/logout')
}
