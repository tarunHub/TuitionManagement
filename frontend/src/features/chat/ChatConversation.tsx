import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { Info, Loader2, Send, Sparkles, TriangleAlert } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Textarea } from '@/components/ui/textarea'
import { cn } from '@/lib/utils'
import { useChat } from './ChatContext'
import { RichText } from './RichText'

const EXAMPLES = [
  'Who still owes fees this month?',
  'How many active students do we have?',
  'How much did we collect in September?',
  'Which students were absent last month?',
  "How is Kaya doing? Show her fees and attendance.",
  'What topics did Batch1 cover in October?',
]

/**
 * The conversation itself (examples, messages, input box). {@code compact} is the bottom-right popup: fewer examples,
 * and the messages scroll inside the panel instead of the page.
 */
export function ChatConversation({ compact = false }: { compact?: boolean }) {
  const { turns, status, unavailable, pending, ask } = useChat()
  const [draft, setDraft] = useState('')
  const bottom = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottom.current?.scrollIntoView?.({ behavior: 'smooth', block: 'end' })
  }, [turns])

  const submit = (text = draft) => {
    if (!text.trim() || pending) return
    setDraft('')
    ask(text)
  }

  return (
    <div className={cn('flex flex-col', compact && 'min-h-0 flex-1')}>
      <div className={cn(compact && 'min-h-0 flex-1 overflow-y-auto p-3')}>
        {unavailable && (
          <p role="alert" className="mb-4 flex items-start gap-2 rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900" data-testid="chat-unavailable">
            <TriangleAlert className="mt-0.5 size-4 shrink-0" /> {status?.message}
          </p>
        )}

        {turns.length === 0 ? (
          <div className={cn('space-y-3', !compact && 'rounded-xl border bg-card p-4')}>
            <p className="flex items-center gap-2 text-sm font-medium"><Sparkles className="size-4 text-primary" /> Try asking</p>
            <div className="flex flex-wrap gap-2">
              {(compact ? EXAMPLES.slice(0, 4) : EXAMPLES).map((q) => (
                <Button key={q} variant="outline" size="sm" className="h-auto whitespace-normal text-left" onClick={() => submit(q)} disabled={unavailable}>
                  {q}
                </Button>
              ))}
            </div>
            <p className="text-xs text-muted-foreground">It can only read data, never change it. Answers usually take 5–20 seconds.</p>
          </div>
        ) : (
          <div className="space-y-4" data-testid="chat-log">
            {turns.map((t, i) => (
              <div key={i} className="space-y-2">
                <div className="ml-auto w-fit max-w-[85%] rounded-2xl rounded-br-sm bg-primary px-4 py-2 text-sm text-primary-foreground" data-testid="chat-question">
                  {t.question}
                </div>
                <div className="w-fit max-w-[95%] rounded-2xl rounded-bl-sm border bg-card px-4 py-2" data-testid={t.answer ? 'chat-answer' : t.error ? 'chat-error' : 'chat-pending'}>
                  {t.answer ? (
                    <>
                      <RichText text={t.answer.answer} />
                      {(t.answer.sources.length > 0 || !t.answer.grounded) && (
                        <div className="mt-2 flex flex-wrap items-center gap-1.5 border-t pt-2 text-xs text-muted-foreground">
                          {t.answer.grounded ? (
                            <>
                              <span>From:</span>
                              {t.answer.sources.map((s) => (
                                <Link key={s.link} to={s.link} className="rounded-full border px-2 py-0.5 text-foreground hover:bg-muted" data-testid="chat-source">
                                  {s.label}
                                </Link>
                              ))}
                            </>
                          ) : (
                            <span className="flex items-center gap-1" data-testid="chat-ungrounded"><Info className="size-3.5" /> Not looked up in your data, so it may be inaccurate.</span>
                          )}
                          <span className="ml-auto">{(t.answer.durationMs / 1000).toFixed(0)}s</span>
                        </div>
                      )}
                    </>
                  ) : t.error ? (
                    <p role="alert" className="text-sm text-destructive">{t.error}</p>
                  ) : (
                    <p className="flex items-center gap-2 text-sm text-muted-foreground"><Loader2 className="size-4 animate-spin" /> Looking that up…</p>
                  )}
                </div>
              </div>
            ))}
            <div ref={bottom} />
          </div>
        )}
      </div>

      <form
        className={cn('flex items-end gap-2 bg-background', compact ? 'border-t p-3' : 'sticky bottom-0 mt-4 py-3')}
        onSubmit={(e) => {
          e.preventDefault()
          submit()
        }}
      >
        <Textarea
          aria-label="Your question"
          rows={2}
          value={draft}
          maxLength={1000}
          placeholder="e.g. Who hasn't paid for October?"
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault()
              submit()
            }
          }}
          disabled={unavailable}
          autoFocus={compact}
          data-testid="chat-input"
        />
        <Button type="submit" disabled={!draft.trim() || pending || unavailable} data-testid="chat-send" aria-label="Send">
          {pending ? <Loader2 className="animate-spin" /> : <Send />}
        </Button>
      </form>
    </div>
  )
}
