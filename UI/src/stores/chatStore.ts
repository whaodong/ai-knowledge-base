import { create } from 'zustand'
import type { Message } from '@/types/chat'

interface ChatState {
  messages: Message[]
  sessionId: string | null
  addMessage: (message: Message) => void
  updateLastMessage: (content: string, finished: boolean, references?: Message['references']) => void
  clearMessages: () => void
  setSessionId: (id: string | null) => void
}

export const useChatStore = create<ChatState>((set, get) => ({
  messages: [],
  sessionId: null,
  
  addMessage: (message) => {
    set((state) => ({
      messages: [...state.messages, message]
    }))
  },
  
  updateLastMessage: (content, finished, references) => {
    set((state) => {
      const messages = [...state.messages]
      const lastIndex = messages.length - 1
      if (lastIndex >= 0 && messages[lastIndex].role === 'assistant') {
        messages[lastIndex] = {
          ...messages[lastIndex],
          content,
          finished,
          references
        }
      }
      return { messages }
    })
  },
  
  clearMessages: () => {
    set({ messages: [], sessionId: null })
  },
  
  setSessionId: (id) => {
    set({ sessionId: id })
  }
}))
