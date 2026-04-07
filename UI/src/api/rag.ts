import apiClient from './client'

export const ragApi = {
  getStreamingUrl: (question: string): string => {
    const token = localStorage.getItem('token')
    return `${import.meta.env.VITE_API_URL}/api/v1/rag/stream?question=${encodeURIComponent(question)}&token=${token}`
  }
}
