import { create } from 'zustand'
import type { Message, ChatSession } from '@/types/chat'

interface ChatState {
  sessions: ChatSession[]
  currentSession: ChatSession | null
  addSession: (session: ChatSession) => void
  setCurrentSession: (session: ChatSession | null) => void
  addMessage: (sessionId: string, message: Message) => void
}

export const useChatStore = create<ChatState>((set) => ({
  sessions: [],
  currentSession: null,
  addSession: (session) => set((state) => ({ sessions: [session, ...state.sessions] })),
  setCurrentSession: (session) => set({ currentSession: session }),
  addMessage: (sessionId, message) => set((state) => {
    const updatedSessions = state.sessions.map((s) =>
      s.id === sessionId ? { ...s, messages: [...s.messages, message] } : s
    )
    const updatedCurrentSession = state.currentSession?.id === sessionId
      ? { ...state.currentSession, messages: [...state.currentSession.messages, message] }
      : state.currentSession
    return { sessions: updatedSessions, currentSession: updatedCurrentSession }
  })
}))
