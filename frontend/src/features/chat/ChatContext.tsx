import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { chatApi } from '@/api/chatApi'
import { errorMessage } from '@/lib/errors'
import type { ChatAnswer, ChatMessage, ChatStatus } from '@/types'

/** Turns kept per question: enough for follow-ups ("and in September?") without slowing the local model down. */
const HISTORY = 8
const STORAGE_KEY = 'smartstart.ask.conversation'

export interface Turn {
  question: string
  answer?: ChatAnswer
  error?: string
}

interface ChatState {
  turns: Turn[]
  status: ChatStatus | undefined
  /** Set when the assistant is switched off or Ollama can't be reached. */
  unavailable: boolean
  pending: boolean
  ask: (question: string) => void
  reset: () => void
}

const ChatContext = createContext<ChatState | null>(null)

function loadTurns(): Turn[] {
  try {
    return JSON.parse(sessionStorage.getItem(STORAGE_KEY) ?? '[]') as Turn[]
  } catch {
    return []
  }
}

/** One conversation shared by the Ask page and the bottom-right popup, so either can pick up where the other left off. */
export function ChatProvider({ children }: { children: ReactNode }) {
  const status = useQuery({ queryKey: ['chat-status'], queryFn: chatApi.status, staleTime: 60_000 })
  const [turns, setTurns] = useState<Turn[]>(loadTurns)

  useEffect(() => {
    try {
      sessionStorage.setItem(STORAGE_KEY, JSON.stringify(turns))
    } catch {
      // storage unavailable (private mode); the conversation just won't survive a reload
    }
  }, [turns])

  const mutation = useMutation({
    mutationFn: (question: string) => {
      const history: ChatMessage[] = turns
        .filter((t) => t.answer)
        .slice(-HISTORY / 2)
        .flatMap((t) => [{ role: 'USER' as const, content: t.question }, { role: 'ASSISTANT' as const, content: t.answer!.answer }])
      return chatApi.ask([...history, { role: 'USER', content: question }])
    },
    onMutate: (question) => setTurns((t) => [...t, { question }]),
    onSuccess: (answer) => setTurns((t) => t.map((x, i) => (i === t.length - 1 ? { ...x, answer } : x))),
    onError: (e) => setTurns((t) => t.map((x, i) => (i === t.length - 1 ? { ...x, error: errorMessage(e) } : x))),
  })

  const value: ChatState = {
    turns,
    status: status.data,
    unavailable: !!status.data && (!status.data.enabled || !status.data.available),
    pending: mutation.isPending,
    ask: (question) => {
      const q = question.trim()
      if (q && !mutation.isPending) mutation.mutate(q)
    },
    reset: () => setTurns([]),
  }
  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>
}

export function useChat() {
  const ctx = useContext(ChatContext)
  if (!ctx) throw new Error('useChat must be used inside ChatProvider')
  return ctx
}
