import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface SettingsState {
  theme: 'light' | 'dark'
  toggleTheme: () => void
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      theme: 'light',
      toggleTheme: () => set((state) => {
        const newTheme = state.theme === 'light' ? 'dark' : 'light'
        document.documentElement.classList.toggle('dark', newTheme === 'dark')
        return { theme: newTheme }
      })
    }),
    { name: 'settings-storage' }
  )
)
