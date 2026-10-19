import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, ChevronDown, Loader2, Mail, MessageSquare, RotateCcw, Send } from 'lucide-react'
import { toast } from 'sonner'
import { classCalendarApi } from '@/api/classCalendarApi'
import { errorMessage } from '@/lib/errors'
import { ErrorState, TableSkeleton } from '@/components/States'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { cn } from '@/lib/utils'
import type { BulkSendResult, MessagePreview, OutgoingMessage } from '@/types'

/** What to review: a title, an optional note (e.g. what changed) and the preview call that builds the messages. */
export interface ReviewRequest {
  title: string
  description?: string
  /** Used in the result toast, e.g. "Today's reminders". */
  what: string
  load: () => Promise<MessagePreview>
}

type Edit = { subject: string; body: string }
const keyOf = (m: OutgoingMessage) => `${m.studentId}-${m.channel}`

/** "2 sent (1 email, 1 SMS), 1 failed" style summary for the bulk sends. */
export function reportSend(r: BulkSendResult, what: string) {
  const emails = r.notifications.filter((n) => n.channel !== 'SMS').length
  const texts = r.notifications.length - emails
  const parts = [emails && `${emails} email${emails === 1 ? '' : 's'}`, texts && `${texts} SMS`].filter(Boolean).join(' + ')
  if (r.created === 0) toast.info(`No ${what} to send`)
  else if (r.failed) toast.warning(`${what}: ${r.sent} sent, ${r.failed} failed (${parts}) — see Notifications to retry`)
  else toast.success(`${what} sent: ${parts}`)
}

function smsParts(text: string) {
  return text.length <= 160 ? 1 : Math.ceil(text.length / 153)
}

/**
 * Shows every message a manual send would produce, one row per student. Untick students to leave them out, open a
 * row to edit its email or SMS text, then Send. Nothing goes out until Send is clicked.
 */
export function ReviewMessagesDialog({ request, onClose }: { request: ReviewRequest | null; onClose: () => void }) {
  const queryClient = useQueryClient()
  const [nonce, setNonce] = useState(0)
  useEffect(() => {
    if (request) setNonce((n) => n + 1)
  }, [request])

  const preview = useQuery({
    queryKey: ['message-preview', nonce],
    queryFn: () => request!.load(),
    enabled: !!request,
    staleTime: Infinity,
    gcTime: 0,
    retry: false,
  })

  const [excluded, setExcluded] = useState<Set<number>>(new Set())
  const [expanded, setExpanded] = useState<Set<number>>(new Set())
  const [edits, setEdits] = useState<Record<string, Edit>>({})
  useEffect(() => {
    setExcluded(new Set())
    setEdits({})
    const first = preview.data?.messages[0]?.studentId
    setExpanded(new Set(first === undefined ? [] : [first]))
  }, [preview.data])

  // Messages grouped by student, in the order the server returned them.
  const families = useMemo(() => {
    const m = new Map<number, OutgoingMessage[]>()
    preview.data?.messages.forEach((msg) => m.set(msg.studentId, [...(m.get(msg.studentId) ?? []), msg]))
    return [...m.entries()]
  }, [preview.data])
  const showBatch = new Set(preview.data?.messages.map((m) => m.className)).size > 1

  const current = (m: OutgoingMessage): Edit => edits[keyOf(m)] ?? { subject: m.subject, body: m.body }
  const chosen = preview.data?.messages.filter((m) => !excluded.has(m.studentId)) ?? []
  const emails = chosen.filter((m) => m.channel === 'EMAIL').length
  const invalid = chosen.some((m) => !current(m).body.trim())

  const send = useMutation({
    mutationFn: () => classCalendarApi.sendMessages(chosen.map((m) => ({
      studentId: m.studentId, channel: m.channel, type: m.type, templateId: m.templateId, ...current(m),
    }))),
    onSuccess: (r) => {
      reportSend(r, request!.what)
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      onClose()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const toggle = (set: Set<number>, id: number) => {
    const next = new Set(set)
    if (next.has(id)) next.delete(id)
    else next.add(id)
    return next
  }

  return (
    <Dialog open={!!request} onOpenChange={(o) => !o && !send.isPending && onClose()}>
      <DialogContent className="flex max-h-[90vh] max-w-2xl flex-col" data-testid="review-messages-dialog">
        <DialogHeader>
          <DialogTitle>{request?.title}</DialogTitle>
          <DialogDescription>
            {request?.description && <span className="mb-1 block text-foreground">{request.description}</span>}
            Check what each family will get. Untick anyone to leave them out, or open a message to edit it. Nothing is sent until you click Send.
          </DialogDescription>
        </DialogHeader>

        {preview.isLoading ? (
          <TableSkeleton rows={4} />
        ) : preview.isError ? (
          <ErrorState error={preview.error} />
        ) : preview.data && (
          <>
            <div className="flex flex-wrap items-center gap-2 text-sm">
              <span className="mr-auto text-muted-foreground" data-testid="review-summary">
                {families.length - new Set([...excluded].filter((id) => families.some(([f]) => f === id))).size} of {families.length} families ·{' '}
                {emails} email{emails === 1 ? '' : 's'}, {chosen.length - emails} SMS
              </span>
              <Button type="button" size="sm" variant="ghost" onClick={() => setExcluded(new Set())}>Select all</Button>
              <Button type="button" size="sm" variant="ghost" onClick={() => setExcluded(new Set(families.map(([id]) => id)))}>Select none</Button>
            </div>

            {preview.data.skipped.length > 0 && (
              <p className="flex items-start gap-2 rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-xs text-amber-900 dark:border-amber-700 dark:bg-amber-950/40 dark:text-amber-200" data-testid="review-skipped">
                <AlertTriangle className="mt-0.5 size-3.5 shrink-0" aria-hidden />
                No parent email or SMS opt-in on file, so these students won't get anything: {preview.data.skipped.join(', ')}.
              </p>
            )}

            <ul className="-mx-1 min-h-0 flex-1 space-y-2 overflow-y-auto px-1" data-testid="review-list">
              {families.map(([studentId, msgs]) => {
                const off = excluded.has(studentId)
                const open = expanded.has(studentId) && !off
                return (
                  <li key={studentId} className={cn('rounded-md border', off && 'bg-muted/40')} data-testid="review-family">
                    <div className="flex items-center gap-3 px-3 py-2">
                      <input
                        type="checkbox"
                        className="size-4"
                        checked={!off}
                        onChange={() => setExcluded((s) => toggle(s, studentId))}
                        aria-label={`Send to ${msgs[0].studentName}'s family`}
                        data-testid="review-include"
                      />
                      <button type="button" className="flex min-w-0 flex-1 items-center gap-2 text-left" onClick={() => !off && setExpanded((s) => toggle(s, studentId))} disabled={off}>
                        <span className={cn('truncate font-medium', off && 'text-muted-foreground line-through')}>{msgs[0].studentName}</span>
                        {showBatch && <span className="text-xs text-muted-foreground">{msgs[0].className}</span>}
                        <span className="ml-auto flex shrink-0 items-center gap-2 text-xs text-muted-foreground">
                          {msgs.map((m) => (
                            <span key={m.channel} className="inline-flex items-center gap-1" title={m.recipient}>
                              {m.channel === 'EMAIL' ? <Mail className="size-3.5" /> : <MessageSquare className="size-3.5" />}
                              {m.channel === 'EMAIL' ? 'Email' : 'SMS'}
                              {edits[keyOf(m)] && <span className="text-primary">· edited</span>}
                            </span>
                          ))}
                          {!off && <ChevronDown className={cn('size-4 transition-transform', open && 'rotate-180')} aria-hidden />}
                        </span>
                      </button>
                    </div>
                    {open && (
                      <div className="space-y-3 border-t px-3 py-3">
                        {msgs.map((m) => {
                          const k = keyOf(m)
                          const v = current(m)
                          const set = (patch: Partial<Edit>) => setEdits((e) => ({ ...e, [k]: { ...v, ...patch } }))
                          return (
                            <div key={k} className="space-y-1.5" data-testid={`review-${m.channel.toLowerCase()}`}>
                              <div className="flex items-center gap-2 text-xs text-muted-foreground">
                                {m.channel === 'EMAIL' ? <Mail className="size-3.5" /> : <MessageSquare className="size-3.5" />}
                                <span className="mr-auto">{m.channel === 'EMAIL' ? 'Email' : 'SMS'} to <span className="font-medium text-foreground">{m.recipient}</span></span>
                                {edits[k] && (
                                  <button type="button" className="inline-flex items-center gap-1 text-primary hover:underline" onClick={() => setEdits(({ [k]: _, ...rest }) => rest)}>
                                    <RotateCcw className="size-3" /> Reset
                                  </button>
                                )}
                              </div>
                              {m.channel === 'EMAIL' && (
                                <Input aria-label="Email subject" value={v.subject} maxLength={200} onChange={(e) => set({ subject: e.target.value })} />
                              )}
                              <Textarea
                                aria-label={m.channel === 'EMAIL' ? 'Email message' : 'SMS message'}
                                value={v.body}
                                rows={m.channel === 'EMAIL' ? 9 : 3}
                                className="font-[inherit] text-sm"
                                onChange={(e) => set({ body: e.target.value })}
                              />
                              {m.channel === 'SMS' && (
                                <p className="text-right text-xs text-muted-foreground">{v.body.length} characters · {smsParts(v.body)} SMS</p>
                              )}
                              {!v.body.trim() && <p className="text-xs text-destructive">The message can't be empty</p>}
                            </div>
                          )
                        })}
                      </div>
                    )}
                  </li>
                )
              })}
            </ul>
          </>
        )}

        <DialogFooter>
          <Button type="button" variant="outline" onClick={onClose} disabled={send.isPending}>Cancel</Button>
          <Button onClick={() => send.mutate()} disabled={send.isPending || !preview.data || chosen.length === 0 || invalid} data-testid="review-send">
            {send.isPending ? <Loader2 className="animate-spin" /> : <Send />} Send {chosen.length} message{chosen.length === 1 ? '' : 's'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
