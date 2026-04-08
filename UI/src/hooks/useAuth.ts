import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { message } from 'antd'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/stores/authStore'
import type { LoginRequest, RegisterRequest } from '@/types/api'

export const useAuth = () => {
  const navigate = useNavigate()
  const { login, logout, user, isAuthenticated } = useAuthStore()

  const loginMutation = useMutation({
    mutationFn: (data: LoginRequest) => authApi.login(data),
    onSuccess: (data) => {
      login(data.user, data.token)
      message.success('登录成功')
      navigate('/documents')
    },
    onError: () => message.error('登录失败')
  })

  const registerMutation = useMutation({
    mutationFn: (data: RegisterRequest) => authApi.register(data),
    onSuccess: () => {
      message.success('注册成功')
      navigate('/login')
    },
    onError: () => message.error('注册失败')
  })

  return {
    user,
    isAuthenticated,
    login: loginMutation.mutateAsync,
    register: registerMutation.mutateAsync,
    logout,
    isLoginLoading: loginMutation.isPending,
    isRegisterLoading: registerMutation.isPending
  }
}
