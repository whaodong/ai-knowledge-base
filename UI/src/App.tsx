import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ConfigProvider, theme } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { useEffect } from 'react'
import Layout from '@/components/common/Layout'
import Login from '@/pages/Login'
import Register from '@/pages/Register'
import Documents from '@/pages/Documents'
import Query from '@/pages/Query'
import Monitoring from '@/pages/Monitoring'
import Settings from '@/pages/Settings'
import { useAuthStore } from '@/stores/authStore'
import { useSettingsStore } from '@/stores/settingsStore'

const queryClient = new QueryClient()

const PrivateRoute = ({ children }: { children: React.ReactNode }) => {
  const { isAuthenticated } = useAuthStore()
  return isAuthenticated ? <>{children}</> : <Navigate to="/login" />
}

const PublicRoute = ({ children }: { children: React.ReactNode }) => {
  const { isAuthenticated } = useAuthStore()
  return isAuthenticated ? <Navigate to="/documents" /> : <>{children}</>
}

function App() {
  const { theme: appTheme } = useSettingsStore()

  useEffect(() => {
    document.documentElement.classList.toggle('dark', appTheme === 'dark')
  }, [appTheme])

  return (
    <ConfigProvider locale={zhCN} theme={{ algorithm: appTheme === 'dark' ? theme.darkAlgorithm : theme.defaultAlgorithm }}>
      <QueryClientProvider client={queryClient}>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<PublicRoute><Login /></PublicRoute>} />
            <Route path="/register" element={<PublicRoute><Register /></PublicRoute>} />
            <Route path="/" element={<PrivateRoute><Layout /></PrivateRoute>}>
              <Route index element={<Navigate to="/documents" />} />
              <Route path="documents" element={<Documents />} />
              <Route path="query" element={<Query />} />
              <Route path="monitoring" element={<Monitoring />} />
              <Route path="settings" element={<Settings />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </QueryClientProvider>
    </ConfigProvider>
  )
}

export default App
