export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PaginatedResponse<T> {
  items: T[]
  total: number
  page: number
  page_size: number
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  user: User
}

export interface RegisterRequest {
  username: string
  password: string
  email: string
  role?: 'USER' | 'ADMIN'
}

export interface User {
  id: string
  username: string
  email: string
  role: 'USER' | 'ADMIN'
  created_at: string
  updated_at: string
}
