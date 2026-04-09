import axios, { AxiosError } from 'axios'
import type { Result } from '@/types/api'

const rawApiBaseURL = import.meta.env.VITE_API_URL
const isLocalGateway =
  rawApiBaseURL === 'http://localhost:8080' ||
  rawApiBaseURL === 'http://127.0.0.1:8080'
const apiBaseURL = import.meta.env.DEV && (!rawApiBaseURL || isLocalGateway)
  ? ''
  : (rawApiBaseURL || '')

const apiClient = axios.create({
  baseURL: apiBaseURL,
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器
apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token && config.headers) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器
apiClient.interceptors.response.use(
  (response) => {
    const result = response.data as Result<unknown>
    if (result.code && result.code !== 200) {
      return Promise.reject(new Error(result.message || '请求失败'))
    }
    return response.data
  },
  async (error: AxiosError<Result<unknown>>) => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken')
      if (refreshToken) {
        try {
          const res = await axios.post(`${apiBaseURL}/api/auth/refresh`, { refreshToken })
          const { accessToken } = res.data.data
          localStorage.setItem('accessToken', accessToken)
          if (error.config) {
            error.config.headers.Authorization = `Bearer ${accessToken}`
            return apiClient.request(error.config)
          }
        } catch {
          localStorage.removeItem('accessToken')
          localStorage.removeItem('refreshToken')
          window.location.href = '/login'
        }
      } else {
        localStorage.removeItem('accessToken')
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default apiClient
