import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ConfigProvider, theme } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { useEffect } from 'react'
import { AppRouter } from '@/router'
import { useSettingsStore } from '@/stores/settingsStore'

const queryClient = new QueryClient()

function App() {
  const { theme: appTheme } = useSettingsStore()

  useEffect(() => {
    document.documentElement.classList.toggle('dark', appTheme === 'dark')
  }, [appTheme])

  return (
    <ConfigProvider locale={zhCN} theme={{ algorithm: appTheme === 'dark' ? theme.darkAlgorithm : theme.defaultAlgorithm }}>
      <QueryClientProvider client={queryClient}>
        <AppRouter />
      </QueryClientProvider>
    </ConfigProvider>
  )
}

export default App
