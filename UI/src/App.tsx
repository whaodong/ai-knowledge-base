import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useEffect } from 'react'
import { AppRouter } from '@/router'
import { useSettingsStore } from '@/stores/settingsStore'

const queryClient = new QueryClient()

function App() {
  const { theme } = useSettingsStore()

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark')
  }, [theme])

  return (
    <QueryClientProvider client={queryClient}>
      <AppRouter />
    </QueryClientProvider>
  )
}

export default App
