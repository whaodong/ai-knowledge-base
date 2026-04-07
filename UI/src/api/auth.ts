import apiClient from './client'
import type { LoginRequest, LoginResponse, RegisterRequest, User } from '@/types/api'

export const authApi = {
  login: (data: LoginRequest): Promise<LoginResponse> => 
    apiClient.post('/api/v1/auth/login', data),
  
  register: (data: RegisterRequest): Promise<User> => 
    apiClient.post('/api/v1/auth/register', data),
  
  getCurrentUser: (): Promise<User> => 
    apiClient.get('/api/v1/auth/me'),
  
  logout: (): Promise<void> => 
    apiClient.post('/api/v1/auth/logout')
}
