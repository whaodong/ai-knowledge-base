import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { type ReactNode } from 'react'
import AdminLayout from '@/layouts/AdminLayout'
import LoginPage from '@/pages/LoginPage'
import DashboardPage from '@/pages/DashboardPage'
import DocumentsPage from '@/pages/DocumentsPage'
import RagWorkbenchPage from '@/pages/RagWorkbenchPage'
import EmbeddingsPage from '@/pages/EmbeddingsPage'
import EmbeddingTasksPage from '@/pages/EmbeddingTasks'
import TokenStatsPage from '@/pages/TokenStats'
import CachePage from '@/pages/Cache'
import VectorsPage from '@/pages/Vectors'
import UsersPage from '@/pages/Users'
import SettingsPage from '@/pages/Settings'
import QueryPage from '@/pages/Query'
import MonitoringPage from '@/pages/Monitoring'
import NotFoundPage from '@/pages/NotFoundPage'
import { useAuthStore } from '@/stores/authStore'

function RequireAuth({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuthStore()
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}

function PublicOnly({ children }: { children: ReactNode }) {
  const { isAuthenticated } = useAuthStore()
  if (isAuthenticated) {
    return <Navigate to="/dashboard" replace />
  }
  return <>{children}</>
}

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route
          path="/login"
          element={
            <PublicOnly>
              <LoginPage />
            </PublicOnly>
          }
        />
        <Route
          path="/"
          element={
            <RequireAuth>
              <AdminLayout />
            </RequireAuth>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<DashboardPage />} />
          <Route path="documents" element={<DocumentsPage />} />
          <Route path="query" element={<QueryPage />} />
          <Route path="monitoring" element={<MonitoringPage />} />
          <Route path="token-stats" element={<TokenStatsPage />} />
          <Route path="embedding-tasks" element={<EmbeddingTasksPage />} />
          <Route path="vectors" element={<VectorsPage />} />
          <Route path="cache" element={<CachePage />} />
          <Route path="users" element={<UsersPage />} />
          <Route path="settings" element={<SettingsPage />} />
          <Route path="rag" element={<RagWorkbenchPage />} />
          <Route path="embeddings" element={<EmbeddingsPage />} />
        </Route>
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  )
}
