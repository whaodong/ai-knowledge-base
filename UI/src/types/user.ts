// 用户管理相关类型

// 用户状态
export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'BANNED' | 'PENDING'

// 用户角色
export type UserRole = 'VIEWER' | 'USER' | 'ADMIN'

// 用户信息
export interface User {
  id: string
  username: string
  email?: string
  phone?: string
  avatar?: string
  role: UserRole
  status: UserStatus
  tokenUsed: number
  tokenLimit: number
  createTime: string
  lastLoginTime?: string
  description?: string
}

// 用户列表查询参数
export interface UserQueryParams {
  pageNum: number
  pageSize: number
  username?: string
  email?: string
  role?: UserRole
  status?: UserStatus
}

// 创建用户请求
export interface CreateUserRequest {
  username: string
  password: string
  email?: string
  phone?: string
  role: UserRole
}

// 更新用户请求
export interface UpdateUserRequest {
  id: string
  email?: string
  phone?: string
  role?: UserRole
  status?: UserStatus
  tokenLimit?: number
  description?: string
}

// 修改密码请求
export interface ChangePasswordRequest {
  userId: string
  oldPassword: string
  newPassword: string
}

// 批量操作请求
export interface BatchUserOperationRequest {
  userIds: string[]
  operation: 'ENABLE' | 'DISABLE' | 'BAN' | 'DELETE'
}

// 用户角色映射
export const USER_ROLE_MAP: Record<string, { text: string; color: string }> = {
  'VIEWER': { text: '访客', color: 'default' },
  'USER': { text: '普通用户', color: 'processing' },
  'ADMIN': { text: '管理员', color: 'success' }
}

// 用户状态映射
export const USER_STATUS_MAP: Record<string, { text: string; color: string }> = {
  'ACTIVE': { text: '正常', color: 'success' },
  'INACTIVE': { text: '未激活', color: 'warning' },
  'BANNED': { text: '已封禁', color: 'error' },
  'PENDING': { text: '待审核', color: 'processing' }
}

// 系统用户（用户管理模块）
export interface SystemUser {
  id: number
  username: string
  email?: string
  role: UserRole
  status: UserStatus
  createdAt: string
  updatedAt: string
}

// 更新用户角色请求
export interface UpdateUserRoleRequest {
  role: UserRole
}

// 更新用户状态请求
export interface UpdateUserStatusRequest {
  status: UserStatus
}

// 用户统计
export interface UserStats {
  totalUsers: number
  activeUsers: number
  inactiveUsers: number
  bannedUsers: number
  adminCount: number
  userCount: number
  viewerCount: number
  todayNewUsers: number
  weekNewUsers: number
  monthNewUsers: number
}
