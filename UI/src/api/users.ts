import apiClient from './client'
import type { Result, PageResponse } from '@/types/api'
import type { SystemUser, UserQueryParams, CreateUserRequest, UpdateUserRoleRequest, UpdateUserStatusRequest, UserStats } from '@/types/user'

export const usersApi = {
  // 获取用户列表
  getUsers: (params: UserQueryParams): Promise<Result<PageResponse<SystemUser>>> =>
    apiClient.get('/api/v1/users', { params }),

  // 获取用户详情
  getUserDetail: (id: number): Promise<Result<SystemUser>> =>
    apiClient.get(`/api/v1/users/${id}`),

  // 创建用户
  createUser: (request: CreateUserRequest): Promise<Result<SystemUser>> =>
    apiClient.post('/api/v1/users', request),

  // 更新用户角色
  updateUserRole: (id: number, request: UpdateUserRoleRequest): Promise<Result<SystemUser>> =>
    apiClient.put(`/api/v1/users/${id}/role`, request),

  // 更新用户状态
  updateUserStatus: (id: number, request: UpdateUserStatusRequest): Promise<Result<SystemUser>> =>
    apiClient.put(`/api/v1/users/${id}/status`, request),

  // 删除用户
  deleteUser: (id: number): Promise<Result<void>> =>
    apiClient.delete(`/api/v1/users/${id}`),

  // 获取用户统计
  getStats: (): Promise<Result<UserStats>> =>
    apiClient.get('/api/v1/users/stats'),

  // 重置用户密码
  resetPassword: (id: number): Promise<Result<{ newPassword: string }>> =>
    apiClient.post(`/api/v1/users/${id}/reset-password`),

  // 批量启用/禁用用户
  batchUpdateStatus: (ids: number[], status: 'ACTIVE' | 'INACTIVE' | 'BANNED'): Promise<Result<{ successCount: number; failedCount: number }>> =>
    apiClient.put('/api/v1/users/batch/status', { userIds: ids, status })
}
