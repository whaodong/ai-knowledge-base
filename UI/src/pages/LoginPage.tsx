import { useNavigate } from 'react-router-dom'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@/api/auth'
import type { LoginRequest } from '@/types/auth'
import { useAuthStore } from '@/stores/authStore'

export default function LoginPage() {
  const navigate = useNavigate()
  const { login } = useAuthStore()

  const loginMutation = useMutation({
    mutationFn: (payload: LoginRequest) => authApi.login(payload),
    onSuccess: (res) => {
      if (res.data?.accessToken) {
        login(
          { id: '', username: res.data.username, role: res.data.role || 'USER', createdAt: '', updatedAt: '' },
          res.data.accessToken,
          res.data.refreshToken
        )
        alert('登录成功')
        navigate('/dashboard')
      }
    },
    onError: (err: Error) => {
      alert(err.message || '登录失败')
    }
  })

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const formData = new FormData(e.currentTarget)
    const values: LoginRequest = {
      username: formData.get('username') as string,
      password: formData.get('password') as string
    }
    loginMutation.mutate(values)
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 dark:from-gray-900 dark:to-gray-800 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-xl p-8">
          <div className="text-center mb-8">
            <h1 className="text-2xl font-bold text-gray-800 dark:text-white mb-2">AI 知识库</h1>
            <p className="text-gray-500 dark:text-gray-400">企业级知识库管理系统</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                用户名
              </label>
              <input
                type="text"
                name="username"
                defaultValue="admin"
                required
                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg
                           bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                           focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="请输入用户名"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                密码
              </label>
              <input
                type="password"
                name="password"
                defaultValue="admin123"
                required
                className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg
                           bg-white dark:bg-gray-700 text-gray-900 dark:text-white
                           focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                placeholder="请输入密码"
              />
            </div>

            <button
              type="submit"
              disabled={loginMutation.isPending}
              className="w-full py-2.5 bg-blue-500 text-white rounded-lg font-medium
                         hover:bg-blue-600 disabled:opacity-50 disabled:cursor-not-allowed
                         transition-colors"
            >
              {loginMutation.isPending ? '登录中...' : '登录'}
            </button>
          </form>

          <div className="mt-6 text-center text-sm text-gray-500 dark:text-gray-400">
            <p>测试账号: admin / admin123</p>
          </div>
        </div>
      </div>
    </div>
  )
}
