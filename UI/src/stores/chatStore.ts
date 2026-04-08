import { create } from 'zustand'
import type { Message, ChatSession } from '@/types/chat'

interface ChatState {
  sessions: ChatSession[]
  currentSession: ChatSession | null
  addSession: (session: ChatSession) => void
  setCurrentSession: (session: ChatSession | null) => void
  addMessage: (sessionId: string, message: Message) => void
  upsertMessage: (sessionId: string, message: Message) => void
}

export const useChatStore = create<ChatState>((set) => ({
  sessions: [],
  currentSession: null,
  addSession: (session) => set((state) => ({ sessions: [session, ...state.sessions] })),
  setCurrentSession: (session) => set({ currentSession: session }),
  addMessage: (sessionId, message) => set((state) => {
    const updatedSessions = state.sessions.map((s) =>
      s.id === sessionId ? { ...s, messages: [...s.messages, message], updated_at: message.timestamp } : s
    )
    const updatedCurrentSession = state.currentSession?.id === sessionId
      ? { ...state.currentSession, messages: [...state.currentSession.messages, message], updated_at: message.timestamp }
      : state.currentSession
    return { sessions: updatedSessions, currentSession: updatedCurrentSession }
  }),
  upsertMessage: (sessionId, message) => set((state) => {
    const applyUpsert = (messages: Message[]) => {
      const idx = messages.findIndex((m) => m.id === message.id)
      if (idx === -1) {
        return [...messages, message]
      }
      const next = [...messages]
      next[idx] = { ...next[idx], ...message }
      return next
    }

    const updatedSessions = state.sessions.map((s) =>
      s.id === sessionId ? { ...s, messages: applyUpsert(s.messages), updated_at: message.timestamp } : s
    )
    const updatedCurrentSession = state.currentSession?.id === sessionId
      ? { ...state.currentSession, messages: applyUpsert(state.currentSession.messages), updated_at: message.timestamp }
      : state.currentSession

    return { sessions: updatedSessions, currentSession: updatedCurrentSession }
  })
}))
