import apiClient from './client'
import type { Result, PageResponse } from '@/types/api'
import type {
  User,
  UserQueryParams,
  CreateUserRequest,
  UpdateUserRequest,
  ChangePasswordRequest,
  BatchUserOperationRequest
} from '@/types/user'

export const userApi = {
  // 获取用户列表（分页）
  getUsers: (params: UserQueryParams): Promise<Result<PageResponse<User>>> =>
    apiClient.get('/api/v1/admin/users', { params }),

  // 获取用户详情
  getUser: (id: string): Promise<Result<User>> =>
    apiClient.get(`/api/v1/admin/users/${id}`),

  // 创建用户
  createUser: (request: CreateUserRequest): Promise<Result<User>> =>
    apiClient.post('/api/v1/admin/users', request),

  // 更新用户
  updateUser: (request: UpdateUserRequest): Promise<Result<User>> =>
    apiClient.put('/api/v1/admin/users', request),

  // 删除用户
  deleteUser: (id: string): Promise<Result<void>> =>
    apiClient.delete(`/api/v1/admin/users/${id}`),

  // 批量删除用户
  batchDeleteUsers: (ids: string[]): Promise<Result<{ deletedCount: number }>> =>
    apiClient.delete('/api/v1/admin/users/batch', { data: ids }),

  // 修改密码
  changePassword: (request: ChangePasswordRequest): Promise<Result<void>> =>
    apiClient.post('/api/v1/admin/users/password', request),

  // 修改用户状态
  changeUserStatus: (id: string, status: string): Promise<Result<void>> =>
    apiClient.post(`/api/v1/admin/users/${id}/status`, { status }),

  // 修改用户角色
  changeUserRole: (id: string, role: string): Promise<Result<void>> =>
    apiClient.post(`/api/v1/admin/users/${id}/role`, { role }),

  // 批量操作
  batchOperation: (request: BatchUserOperationRequest): Promise<Result<{ affectedCount: number }>> =>
    apiClient.post('/api/v1/admin/users/batch-operation', request),

  // 获取当前用户信息
  getCurrentUser: (): Promise<Result<User>> =>
    apiClient.get('/api/v1/admin/users/me')
}
