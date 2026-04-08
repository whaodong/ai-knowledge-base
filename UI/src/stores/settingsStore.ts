import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface SettingsState {
  theme: 'light' | 'dark'
  language: 'zh-CN' | 'en-US'
  ragConfig: {
    topK: number
    similarityThreshold: number
    hybridSearch: boolean
    rerankEnabled: boolean
    embeddingModel: string
  }
  setTheme: (theme: 'light' | 'dark') => void
  setLanguage: (language: 'zh-CN' | 'en-US') => void
  setRagConfig: (config: Partial<SettingsState['ragConfig']>) => void
}

export const useSettingsStore = create<SettingsState>()(
  persist(
    (set) => ({
      theme: 'light',
      language: 'zh-CN',
      ragConfig: {
        topK: 10,
        similarityThreshold: 0.5,
        hybridSearch: true,
        rerankEnabled: true,
        embeddingModel: 'text-embedding-v3'
      },
      setTheme: (theme) => set({ theme }),
      setLanguage: (language) => set({ language }),
      setRagConfig: (config) => set((state) => ({
        ragConfig: { ...state.ragConfig, ...config }
      }))
    }),
    { name: 'settings-storage' }
  )
)
