export type { Result, PageRequest, PageResponse } from '@/types/common'
export { ErrorCode } from '@/types/common'
export type { LoginRequest, RegisterRequest, RefreshTokenRequest, AuthResponse } from '@/types/auth'

export interface User {
  id?: string
  username: string
  email?: string
  role: 'VIEWER' | 'USER' | 'ADMIN' | string
  createdAt?: string
  updatedAt?: string
}
