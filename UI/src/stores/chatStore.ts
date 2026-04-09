import { create } from 'zustand'
import type { Message, ChatSession } from '@/types/chat'

interface ChatState {
  messages: Message[]
  sessionId: string | null
  sessions: ChatSession[]
  currentSession: ChatSession | null
  addSession: (session: ChatSession) => void
  setCurrentSession: (session: ChatSession | null) => void
  addMessage: (...args: [Message] | [string, Message]) => void
  upsertMessage: (sessionId: string, message: Message) => void
  updateLastMessage: (content: string, finished: boolean, references?: Message['references']) => void
  clearMessages: () => void
  setSessionId: (id: string | null) => void
}

export const useChatStore = create<ChatState>((set) => ({
  messages: [],
  sessionId: null,
  sessions: [],
  currentSession: null,
  
  addSession: (session) => {
    set((state) => ({ sessions: [...state.sessions, session] }))
  },

  setCurrentSession: (session) => {
    set({ currentSession: session })
  },

  addMessage: (...args) => {
    set((state) => {
      if (args.length === 1) {
        const message = args[0]
        return { messages: [...state.messages, message] }
      }

      const [targetSessionId, message] = args
      const sessions = state.sessions.map((session) => {
        if (session.id !== targetSessionId) {
          return session
        }
        return {
          ...session,
          messages: [...session.messages, message],
          updated_at: new Date().toISOString()
        }
      })

      const currentSession = state.currentSession?.id === targetSessionId
        ? sessions.find((s) => s.id === targetSessionId) ?? null
        : state.currentSession

      return { sessions, currentSession }
    })
  },

  upsertMessage: (targetSessionId, message) => {
    set((state) => {
      const sessions = state.sessions.map((session) => {
        if (session.id !== targetSessionId) {
          return session
        }
        const exists = session.messages.some((m) => m.id === message.id)
        const nextMessages = exists
          ? session.messages.map((m) => (m.id === message.id ? message : m))
          : [...session.messages, message]

        return {
          ...session,
          messages: nextMessages,
          updated_at: new Date().toISOString()
        }
      })

      const currentSession = state.currentSession?.id === targetSessionId
        ? sessions.find((s) => s.id === targetSessionId) ?? null
        : state.currentSession

      return { sessions, currentSession }
    })
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
    set({ messages: [], sessionId: null, sessions: [], currentSession: null })
  },
  
  setSessionId: (id) => {
    set({ sessionId: id })
  }
}))
