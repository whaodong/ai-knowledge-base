export const ragApi = {
  getStreamingUrl: (question: string): string => {
    const token = localStorage.getItem('token')
    const baseUrl = import.meta.env.VITE_API_URL || 'http://localhost:8080'
    return `${baseUrl}/api/v1/rag/stream?question=${encodeURIComponent(question)}&token=${encodeURIComponent(token || '')}`
  }
}
