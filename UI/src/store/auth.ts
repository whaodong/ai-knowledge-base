import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthState {
  accessToken: string
  refreshToken: string
  username: string
  role: string
  isAuthenticated: boolean
  setAuth: (payload: { accessToken: string; refreshToken: string; username: string; role: string }) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: '',
      refreshToken: '',
      username: '',
      role: '',
      isAuthenticated: false,
      setAuth: ({ accessToken, refreshToken, username, role }) => {
        localStorage.setItem('accessToken', accessToken)
        localStorage.setItem('refreshToken', refreshToken)
        set({
          accessToken,
          refreshToken,
          username,
          role,
          isAuthenticated: true
        })
      },
      logout: () => {
        localStorage.removeItem('accessToken')
        localStorage.removeItem('refreshToken')
        set({
          accessToken: '',
          refreshToken: '',
          username: '',
          role: '',
          isAuthenticated: false
        })
      }
    }),
    { name: 'akb-auth-store' }
  )
)
