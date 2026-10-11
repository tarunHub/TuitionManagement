import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Loader2, Maximize2, RotateCcw, Sparkles, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { ChatConversation } from './ChatConversation'
import { useChat } from './ChatContext'

const OPEN_KEY = 'smartstart.ask.popupOpen'

/** Bottom-right "Ask" button that opens the assistant in a popup on every screen (hidden on the Ask page itself). */
export function ChatWidget() {
  const { turns, status, pending, reset } = useChat()
  const [open, setOpen] = useState(() => {
    try {
      return sessionStorage.getItem(OPEN_KEY) === '1'
    } catch {
      return false
    }
  })

  useEffect(() => {
    try {
      sessionStorage.setItem(OPEN_KEY, open ? '1' : '0')
    } catch {
      // storage unavailable; the popup just starts closed after a reload
    }
  }, [open])

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setOpen(false)
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [open])

  // Switched off in .env: no floating button at all (the Ask page explains how to turn it on).
  if (status && !status.enabled) return null

  return (
    <div className="no-print">
      {open && (
        <section
          role="dialog"
          aria-label="Ask the assistant"
          className="fixed inset-x-2 bottom-20 top-16 z-40 flex flex-col overflow-hidden rounded-xl border bg-background shadow-2xl sm:inset-x-auto sm:right-6 sm:top-auto sm:h-[min(600px,calc(100vh-7rem))] sm:w-[400px]"
          data-testid="chat-widget"
        >
          <header className="flex items-center gap-1 border-b bg-slate-900 px-3 py-2 text-white">
            <Sparkles className="size-4" aria-hidden />
            <h2 className="mr-auto text-sm font-semibold">Ask about your data</h2>
            {turns.length > 0 && (
              <Button variant="ghost" size="icon" className="size-8 text-white hover:bg-white/10 hover:text-white" onClick={reset} disabled={pending} aria-label="New chat" data-testid="chat-new">
                <RotateCcw />
              </Button>
            )}
            <Button variant="ghost" size="icon" className="size-8 text-white hover:bg-white/10 hover:text-white" asChild>
              <Link to="/ask" aria-label="Open full page" onClick={() => setOpen(false)}><Maximize2 /></Link>
            </Button>
            <Button variant="ghost" size="icon" className="size-8 text-white hover:bg-white/10 hover:text-white" onClick={() => setOpen(false)} aria-label="Close assistant">
              <X />
            </Button>
          </header>
          <ChatConversation compact />
        </section>
      )}
      <Button
        className="fixed bottom-4 right-4 z-40 size-14 rounded-full shadow-lg sm:bottom-6 sm:right-6"
        onClick={() => setOpen((o) => !o)}
        aria-label={open ? 'Hide assistant' : 'Ask the assistant'}
        aria-expanded={open}
        data-testid="chat-widget-toggle"
      >
        {open ? <X className="size-6" /> : pending ? <Loader2 className="size-6 animate-spin" /> : <Sparkles className="size-6" />}
      </Button>
    </div>
  )
}
