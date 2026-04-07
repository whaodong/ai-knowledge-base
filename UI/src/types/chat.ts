export interface Message {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: string
  references?: Reference[]
}

export interface Reference {
  document_id: string
  document_title: string
  chunk_id: string
  content: string
  score: number
}

export interface ChatSession {
  id: string
  title: string
  messages: Message[]
  created_at: string
  updated_at: string
}
