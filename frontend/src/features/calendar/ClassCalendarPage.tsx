import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ReviewMessagesDialog, type ReviewRequest } from './ReviewMessagesDialog'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { format } from 'date-fns'
import { Bell, Loader2, Plus, Repeat, Send, Settings2, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { classCalendarApi } from '@/api/classCalendarApi'
import { notificationsApi } from '@/api/notificationsApi'
import { FormField } from '@/components/FormField'
import { PageHeader } from '@/components/PageHeader'
import { ErrorState, TableSkeleton } from '@/components/States'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { useMonthParam } from '@/hooks/useMonthParam'
import { useClassGroups } from '@/hooks/useReferenceData'
import { formatDate, formatMonth, monthToDate, isPastMonth, todayIso } from '@/lib/dates'
import { applyServerErrors, errorMessage } from '@/lib/errors'
import { cn } from '@/lib/utils'
import type { ClassChangeInput, ClassSession, ReminderSettings, Weekday } from '@/types'

const WEEKDAYS: { value: Weekday; short: string }[] = [
  { value: 'MONDAY', short: 'Mon' },
  { value: 'TUESDAY', short: 'Tue' },
  { value: 'WEDNESDAY', short: 'Wed' },
  { value: 'THURSDAY', short: 'Thu' },
  { value: 'FRIDAY', short: 'Fri' },
  { value: 'SATURDAY', short: 'Sat' },
  { value: 'SUNDAY', short: 'Sun' },
]

/** One colour per batch so several batches on the same day stay readable. */
const BATCH_COLOURS = [
  'bg-sky-100 text-sky-900 border-sky-200',
  'bg-violet-100 text-violet-900 border-violet-200',
  'bg-emerald-100 text-emerald-900 border-emerald-200',
  'bg-orange-100 text-orange-900 border-orange-200',
  'bg-pink-100 text-pink-900 border-pink-200',
  'bg-teal-100 text-teal-900 border-teal-200',
]

const time = z.string().regex(/^\d{2}:\d{2}$/, 'Enter a time')
const sessionSchema = z
  .object({
    classGroupId: z.string().min(1, 'Choose a batch'),
    date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Choose a date'),
    startTime: time,
    endTime: time,
    location: z.string().max(150).optional(),
    notes: z.string().max(500).optional(),
  })
  .refine((v) => v.endTime > v.startTime, { path: ['endTime'], message: 'Must be after the start time' })
type SessionValues = z.infer<typeof sessionSchema>

const repeatSchema = z
  .object({
    classGroupId: z.string().min(1, 'Choose a batch'),
    weekdays: z.array(z.string()).min(1, 'Pick at least one day'),
    startTime: time,
    endTime: time,
    location: z.string().max(150).optional(),
  })
  .refine((v) => v.endTime > v.startTime, { path: ['endTime'], message: 'Must be after the start time' })
type RepeatValues = z.infer<typeof repeatSchema>

function useInvalidate() {
  const queryClient = useQueryClient()
  return () => {
    queryClient.invalidateQueries({ queryKey: ['class-calendar'] })
    queryClient.invalidateQueries({ queryKey: ['attendance'] })
  }
}

/** Add or edit one class day. */
function SessionDialog({ open, onOpenChange, session, defaults, onChanged, onReview }: {
  open: boolean
  onOpenChange: (o: boolean) => void
  session: ClassSession | null
  defaults: { date: string; classGroupId: number | '' ; startTime: string; endTime: string }
  /** After a save or removal: the page asks whether to tell parents. */
  onChanged: (change: ClassChangeInput) => void
  onReview: (r: ReviewRequest) => void
}) {
  const groups = useClassGroups()
  const invalidate = useInvalidate()
  const { register, handleSubmit, reset, setError, formState } = useForm<SessionValues>({ resolver: zodResolver(sessionSchema) })

  useEffect(() => {
    if (!open) return
    reset(
      session
        ? { classGroupId: String(session.classGroupId), date: session.date, startTime: session.startTime, endTime: session.endTime, location: session.location ?? '', notes: session.notes ?? '' }
        : { classGroupId: defaults.classGroupId === '' ? '' : String(defaults.classGroupId), date: defaults.date, startTime: defaults.startTime, endTime: defaults.endTime, location: '', notes: '' },
    )
  }, [open, session, defaults, reset])

  const save = useMutation({
    mutationFn: (v: SessionValues) => {
      const input = { classGroupId: Number(v.classGroupId), date: v.date, startTime: v.startTime, endTime: v.endTime, location: v.location?.trim() || undefined, notes: v.notes?.trim() || undefined }
      return session ? classCalendarApi.update(session.id, input) : classCalendarApi.create(input)
    },
    onSuccess: (s) => {
      toast.success(`${s.classGroupName} class saved for ${formatDate(s.date, 'EEE, MMM d')}`)
      invalidate()
      onOpenChange(false)
      const base = { classGroupId: s.classGroupId, date: s.date, startTime: s.startTime, endTime: s.endTime, location: s.location }
      if (!session) {
        onChanged({ kind: 'ADDED', ...base })
      } else if (
        session.date !== s.date || session.startTime !== s.startTime || session.endTime !== s.endTime ||
        (session.location ?? null) !== (s.location ?? null) || session.classGroupId !== s.classGroupId
      ) {
        onChanged({
          kind: 'CHANGED', ...base,
          previousDate: session.date, previousStartTime: session.startTime, previousEndTime: session.endTime, previousLocation: session.location,
        })
      }
    },
    onError: (e) => {
      if (!applyServerErrors(e, setError, ['classGroupId', 'date', 'startTime', 'endTime'])) toast.error(errorMessage(e))
    },
  })

  const remove = useMutation({
    mutationFn: () => classCalendarApi.remove(session!.id),
    onSuccess: () => {
      toast.success('Class removed from the calendar')
      invalidate()
      onOpenChange(false)
      const r = session!
      onChanged({ kind: 'CANCELLED', classGroupId: r.classGroupId, date: r.date, startTime: r.startTime, endTime: r.endTime, location: r.location })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const reviewReminder = () => {
    const s = session!
    onReview({
      title: `Reminder for ${s.classGroupName} on ${formatDate(s.date, 'EEE, MMM d')}`,
      what: 'Class reminder',
      load: () => classCalendarApi.previewRemind(s.id),
    })
    onOpenChange(false)
  }

  const e = formState.errors
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg" data-testid="session-dialog">
        <DialogHeader>
          <DialogTitle>{session ? 'Edit class' : 'Add class'}</DialogTitle>
          <DialogDescription>Shows as S on the Attendance page for every student in the batch.</DialogDescription>
        </DialogHeader>
        {session && (
          <div className="flex flex-wrap items-center gap-2 rounded-md border bg-muted/40 px-3 py-2 text-sm">
            <Bell className="size-4 text-muted-foreground" aria-hidden />
            <span className="mr-auto text-muted-foreground">Automatic reminders follow Reminder settings.</span>
            <Button type="button" size="sm" variant="outline" onClick={reviewReminder} data-testid="session-remind">
              <Send /> Send reminder now
            </Button>
          </div>
        )}
        <form noValidate onSubmit={handleSubmit((v) => save.mutate(v))} className="grid gap-3 sm:grid-cols-2">
          <FormField id="session-group" label="Batch" required error={e.classGroupId?.message}>
            <NativeSelect id="session-group" {...register('classGroupId')} data-testid="session-group">
              <option value="">Select…</option>
              {groups.data?.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
            </NativeSelect>
          </FormField>
          <FormField id="session-date" label="Date" required error={e.date?.message}>
            <Input id="session-date" type="date" {...register('date')} data-testid="session-date" />
          </FormField>
          <FormField id="session-start" label="Start" required error={e.startTime?.message}>
            <Input id="session-start" type="time" {...register('startTime')} data-testid="session-start" />
          </FormField>
          <FormField id="session-end" label="End" required error={e.endTime?.message}>
            <Input id="session-end" type="time" {...register('endTime')} data-testid="session-end" />
          </FormField>
          <FormField id="session-location" label="Location">
            <Input id="session-location" {...register('location')} />
          </FormField>
          <FormField id="session-notes" label="Notes" hint="Included in the schedule email">
            <Input id="session-notes" {...register('notes')} placeholder="e.g. Test day" />
          </FormField>
          <DialogFooter className="gap-2 sm:col-span-2">
            {session && (
              <Button type="button" variant="ghost" className="mr-auto text-destructive" onClick={() => remove.mutate()} disabled={remove.isPending} data-testid="session-delete">
                {remove.isPending ? <Loader2 className="animate-spin" /> : <Trash2 />} Remove
              </Button>
            )}
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" disabled={save.isPending} data-testid="session-save">{save.isPending && <Loader2 className="animate-spin" />} Save class</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

/** "Every Tue and Thu, 17:00–18:00" for one batch across the month. */
function RepeatDialog({ open, onOpenChange, month, groupId }: { open: boolean; onOpenChange: (o: boolean) => void; month: string; groupId: number | '' }) {
  const groups = useClassGroups()
  const invalidate = useInvalidate()
  const { register, handleSubmit, reset, setError, formState } = useForm<RepeatValues>({ resolver: zodResolver(repeatSchema) })

  useEffect(() => {
    if (open) reset({ classGroupId: groupId === '' ? '' : String(groupId), weekdays: [], startTime: '17:00', endTime: '18:00', location: '' })
  }, [open, groupId, reset])

  const save = useMutation({
    mutationFn: (v: RepeatValues) =>
      classCalendarApi.repeatWeekly({
        classGroupId: Number(v.classGroupId), month, weekdays: v.weekdays as Weekday[], startTime: v.startTime, endTime: v.endTime, location: v.location?.trim() || undefined,
      }),
    onSuccess: (r) => {
      toast.success(`Added ${r.created} class${r.created === 1 ? '' : 'es'}${r.skipped ? ` (${r.skipped} day${r.skipped === 1 ? '' : 's'} already had one)` : ''}`)
      invalidate()
      onOpenChange(false)
    },
    onError: (e) => {
      if (!applyServerErrors(e, setError, ['classGroupId', 'weekdays', 'startTime', 'endTime'])) toast.error(errorMessage(e))
    },
  })

  const e = formState.errors
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg" data-testid="repeat-dialog">
        <DialogHeader>
          <DialogTitle>Repeat weekly in {formatMonth(month)}</DialogTitle>
          <DialogDescription>Adds a class on every chosen weekday this month. Days that already have a class for the batch are left as they are.</DialogDescription>
        </DialogHeader>
        <form noValidate onSubmit={handleSubmit((v) => save.mutate(v))} className="grid gap-3 sm:grid-cols-2">
          <FormField id="repeat-group" label="Batch" required error={e.classGroupId?.message} className="sm:col-span-2">
            <NativeSelect id="repeat-group" {...register('classGroupId')} data-testid="repeat-group">
              <option value="">Select…</option>
              {groups.data?.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
            </NativeSelect>
          </FormField>
          <fieldset className="space-y-1.5 sm:col-span-2">
            <legend className="text-sm font-medium">Days<span className="ml-0.5 text-destructive" aria-hidden>*</span></legend>
            <div className="flex flex-wrap gap-2">
              {WEEKDAYS.map((d) => (
                <label key={d.value} className="flex cursor-pointer items-center gap-1.5 rounded-md border px-2.5 py-1.5 text-sm has-[:checked]:border-primary has-[:checked]:bg-primary/10">
                  <input type="checkbox" value={d.value} {...register('weekdays')} data-testid={`repeat-day-${d.short.toLowerCase()}`} />
                  {d.short}
                </label>
              ))}
            </div>
            {e.weekdays && <p role="alert" className="text-xs font-medium text-destructive">{e.weekdays.message}</p>}
          </fieldset>
          <FormField id="repeat-start" label="Start" required error={e.startTime?.message}>
            <Input id="repeat-start" type="time" {...register('startTime')} data-testid="repeat-start" />
          </FormField>
          <FormField id="repeat-end" label="End" required error={e.endTime?.message}>
            <Input id="repeat-end" type="time" {...register('endTime')} data-testid="repeat-end" />
          </FormField>
          <FormField id="repeat-location" label="Location" className="sm:col-span-2">
            <Input id="repeat-location" {...register('location')} />
          </FormField>
          <DialogFooter className="sm:col-span-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" disabled={save.isPending} data-testid="repeat-save">{save.isPending && <Loader2 className="animate-spin" />} Add classes</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

const DAYS_BEFORE = [
  { value: 0, label: 'On the day of the class' },
  { value: 1, label: 'The day before' },
  { value: 2, label: 'Two days before' },
]

function to12h(hhmm: string) {
  const [h, m] = hhmm.split(':').map(Number)
  return `${((h + 11) % 12) + 1}:${String(m).padStart(2, '0')} ${h < 12 ? 'AM' : 'PM'}`
}

/** "On, 8:00 AM on the day of the class, by email + SMS" */
function describeReminders(r: ReminderSettings) {
  if (!r.enabled) return 'Off'
  const how = [r.sendEmail && 'email', r.sendSms && r.smsAvailable && 'SMS'].filter(Boolean).join(' + ') || 'no channel selected'
  return `${to12h(r.sendTime)} ${DAYS_BEFORE.find((d) => d.value === r.daysBefore)?.label.toLowerCase()}, by ${how}`
}

/** Automatic class reminder settings (saved in the app; checked every minute by the backend). */
function ReminderSettingsDialog({ open, onOpenChange, current }: { open: boolean; onOpenChange: (o: boolean) => void; current: ReminderSettings | undefined }) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState({ enabled: true, daysBefore: 0, sendTime: '08:00', sendEmail: true, sendSms: true })
  useEffect(() => {
    if (open && current) setForm({ enabled: current.enabled, daysBefore: current.daysBefore, sendTime: current.sendTime, sendEmail: current.sendEmail, sendSms: current.sendSms })
  }, [open, current])

  const save = useMutation({
    mutationFn: () => classCalendarApi.updateReminderSettings(form),
    onSuccess: (r) => {
      queryClient.setQueryData(['reminder-settings'], r)
      toast.success(r.enabled ? `Automatic reminders: ${describeReminders(r)}` : 'Automatic reminders turned off')
      onOpenChange(false)
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const set = <K extends keyof typeof form>(k: K, v: (typeof form)[K]) => setForm((f) => ({ ...f, [k]: v }))
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-md" data-testid="reminder-settings-dialog">
        <DialogHeader>
          <DialogTitle>Automatic class reminders</DialogTitle>
          <DialogDescription>Parents of every batch with a class get one reminder per child, using the Class Reminder templates.</DialogDescription>
        </DialogHeader>
        <div className="space-y-4">
          <label className="flex items-center gap-2 text-sm font-medium">
            <input type="checkbox" checked={form.enabled} onChange={(e) => set('enabled', e.target.checked)} data-testid="reminders-enabled" />
            Send reminders automatically
          </label>
          <fieldset disabled={!form.enabled} className="grid gap-3 disabled:opacity-60 sm:grid-cols-2">
            <FormField id="reminders-when" label="When">
              <NativeSelect id="reminders-when" value={form.daysBefore} onChange={(e) => set('daysBefore', Number(e.target.value))} data-testid="reminders-when">
                {DAYS_BEFORE.map((d) => <option key={d.value} value={d.value}>{d.label}</option>)}
              </NativeSelect>
            </FormField>
            <FormField id="reminders-time" label="At">
              <Input id="reminders-time" type="time" value={form.sendTime} onChange={(e) => set('sendTime', e.target.value)} required data-testid="reminders-time" />
            </FormField>
            <div className="space-y-2 sm:col-span-2">
              <p className="text-sm font-medium">Send by</p>
              <label className="flex items-center gap-2 text-sm">
                <input type="checkbox" checked={form.sendEmail} onChange={(e) => set('sendEmail', e.target.checked)} data-testid="reminders-email" /> Email
              </label>
              <label className={cn('flex items-center gap-2 text-sm', !current?.smsAvailable && 'opacity-60')}>
                <input type="checkbox" checked={form.sendSms} onChange={(e) => set('sendSms', e.target.checked)} data-testid="reminders-sms" /> SMS
                <span className="text-xs text-muted-foreground">{current?.smsAvailable ? '(parents who opted in)' : '(SMS is not set up, so only email goes out)'}</span>
              </label>
            </div>
          </fieldset>
          {current?.lastRunDate && <p className="text-xs text-muted-foreground">Last automatic run: {formatDate(current.lastRunDate, 'EEE, MMM d, yyyy')}. It runs once a day; if the app was off at the set time it catches up when it starts.</p>}
        </div>
        <DialogFooter>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={() => save.mutate()} disabled={save.isPending || !/^\d{2}:\d{2}$/.test(form.sendTime)} data-testid="reminders-save">
            {save.isPending && <Loader2 className="animate-spin" />} Save
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

/** What the "notify parents?" prompt says about a change. */
function describeChange(c: ClassChangeInput, batchName: string) {
  const when = (date: string, start?: string, end?: string) => `${formatDate(date, 'EEE, MMM d')}${start ? `, ${start}–${end}` : ''}`
  if (c.kind === 'ADDED') return `A new ${batchName} class on ${when(c.date, c.startTime, c.endTime)}.`
  if (c.kind === 'CANCELLED') return `The ${batchName} class on ${when(c.date, c.startTime, c.endTime)} was removed (parents will be told it is cancelled).`
  return `The ${batchName} class on ${when(c.previousDate!, c.previousStartTime, c.previousEndTime)} is now on ${when(c.date, c.startTime, c.endTime)}${c.location ? ` at ${c.location}` : ''}.`
}

/** Send the month's class dates to parents by email and/or SMS, one message per batch. */
function SendScheduleDialog({ open, onOpenChange, month, batches, onReview }: {
  open: boolean
  onOpenChange: (o: boolean) => void
  onReview: (r: ReviewRequest) => void
  month: string
  batches: [number, { name: string; count: number }][]
}) {
  const channels = useQuery({ queryKey: ['notification-channels'], queryFn: notificationsApi.channels, enabled: open })
  const [email, setEmail] = useState(true)
  const [sms, setSms] = useState(false)
  useEffect(() => {
    if (open) {
      setEmail(true)
      setSms(!!channels.data?.sms)
    }
  }, [open, channels.data?.sms])

  const review = () => {
    const ids = batches.map(([id]) => id)
    const chosen = [...(email ? ['EMAIL' as const] : []), ...(sms ? ['SMS' as const] : [])]
    onReview({
      title: `${formatMonth(month)} schedule`,
      what: `${formatMonth(month)} schedule`,
      load: () => classCalendarApi.previewSchedule(month, ids, chosen),
    })
    onOpenChange(false)
  }

  const smsReady = !!channels.data?.sms
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg" data-testid="send-schedule-dialog">
        <DialogHeader>
          <DialogTitle>Send the {formatMonth(month)} schedule</DialogTitle>
          <DialogDescription>Each batch's parents get that batch's class dates and times.</DialogDescription>
        </DialogHeader>
        <ul className="list-disc space-y-0.5 pl-5 text-sm">
          {batches.map(([id, b]) => <li key={id}>{b.name} — {b.count} class{b.count === 1 ? '' : 'es'}</li>)}
        </ul>
        <fieldset className="space-y-2">
          <legend className="mb-1 text-sm font-medium">Send by</legend>
          <label className="flex items-center gap-2 text-sm">
            <input type="checkbox" checked={email} onChange={(e) => setEmail(e.target.checked)} data-testid="send-email" /> Email
            <span className="text-xs text-muted-foreground">(parents of active students with an email on file)</span>
          </label>
          <label className={cn('flex items-center gap-2 text-sm', !smsReady && 'opacity-60')}>
            <input type="checkbox" checked={sms} disabled={!smsReady} onChange={(e) => setSms(e.target.checked)} data-testid="send-sms" /> SMS
            <span className="text-xs text-muted-foreground">{smsReady ? '(parents who opted in to texts)' : `(${channels.data?.smsMessage ?? 'SMS is not set up'})`}</span>
          </label>
        </fieldset>
        <DialogFooter>
          <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
          <Button onClick={review} disabled={!email && !sms} data-testid="send-schedule-confirm">
            Review messages
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export function ClassCalendarPage() {
  const [month] = useMonthParam()
  const locked = isPastMonth(month)
  const [groupId, setGroupId] = useState<number | ''>('')
  const groups = useClassGroups()
  const q = useQuery({ queryKey: ['class-calendar', month, groupId], queryFn: () => classCalendarApi.list(month, groupId) })

  const [editing, setEditing] = useState<{ session: ClassSession | null; date: string } | null>(null)
  const [repeatOpen, setRepeatOpen] = useState(false)
  const [emailOpen, setEmailOpen] = useState(false)
  const [settingsOpen, setSettingsOpen] = useState(false)
  const [review, setReview] = useState<ReviewRequest | null>(null)
  const reminders = useQuery({ queryKey: ['reminder-settings'], queryFn: classCalendarApi.reminderSettings })

  // Changes to classes that are already over aren't worth a message.
  const offerNotice = (c: ClassChangeInput) => {
    if (c.date < todayIso() && (!c.previousDate || c.previousDate < todayIso())) return
    setReview({
      title: 'Let parents know?',
      description: describeChange(c, groups.data?.find((g) => g.id === c.classGroupId)?.name ?? 'batch'),
      what: 'Class update',
      load: () => classCalendarApi.previewChange(c),
    })
  }

  const colourOf = (id: number) => BATCH_COLOURS[Math.max(0, groups.data?.findIndex((g) => g.id === id) ?? 0) % BATCH_COLOURS.length]

  // Mon–Sun weeks covering the month; null pads the days before the 1st and after the last.
  const weeks = useMemo(() => {
    const first = monthToDate(month)
    const days = new Date(first.getFullYear(), first.getMonth() + 1, 0).getDate()
    const lead = (first.getDay() + 6) % 7
    const cells: (string | null)[] = [...Array(lead).fill(null)]
    for (let d = 1; d <= days; d++) cells.push(format(new Date(first.getFullYear(), first.getMonth(), d), 'yyyy-MM-dd'))
    while (cells.length % 7) cells.push(null)
    return Array.from({ length: cells.length / 7 }, (_, i) => cells.slice(i * 7, i * 7 + 7))
  }, [month])

  const byDate = useMemo(() => {
    const m = new Map<string, ClassSession[]>()
    q.data?.forEach((s) => m.set(s.date, [...(m.get(s.date) ?? []), s]))
    return m
  }, [q.data])

  // What the email will cover: per batch, how many classes are on the calendar this month.
  const perBatch = useMemo(() => {
    const m = new Map<number, { name: string; count: number }>()
    q.data?.forEach((s) => m.set(s.classGroupId, { name: s.classGroupName, count: (m.get(s.classGroupId)?.count ?? 0) + 1 }))
    return [...m.entries()]
  }, [q.data])

  const remindToday = () => setReview({
    title: "Today's class reminders",
    what: "Today's reminders",
    load: classCalendarApi.previewRemindToday,
  })

  // The last class used for this batch is the most likely time for the next one.
  const last = q.data?.filter((s) => groupId === '' || s.classGroupId === groupId).at(-1)
  const editingDate = editing?.date ?? ''
  const defaults = useMemo(
    () => ({ date: editingDate, classGroupId: groupId, startTime: last?.startTime ?? '17:00', endTime: last?.endTime ?? '18:00' }),
    [editingDate, groupId, last?.startTime, last?.endTime],
  )
  const today = todayIso()

  return (
    <div>
      <PageHeader
        title="Class Calendar"
        description={`${formatMonth(month)} — the days each batch has class. They appear as S on the Attendance page until you mark P or A.`}
        actions={
          <>
            <NativeSelect aria-label="Batch" className="w-40" value={groupId} onChange={(e) => setGroupId(e.target.value ? Number(e.target.value) : '')} data-testid="calendar-group">
              <option value="">All batches</option>
              {groups.data?.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
            </NativeSelect>
            {!locked && (
              <>
                <Button variant="outline" onClick={() => setRepeatOpen(true)} data-testid="calendar-repeat"><Repeat /> Repeat weekly</Button>
                <Button variant="outline" onClick={() => setEditing({ session: null, date: `${month}-01` })} data-testid="calendar-add"><Plus /> Add class</Button>
              </>
            )}
            <Button variant="outline" onClick={() => setSettingsOpen(true)} data-testid="calendar-reminder-settings"><Settings2 /> Reminder settings</Button>
            <Button variant="outline" onClick={remindToday} data-testid="calendar-remind-today" title="Review, then send the reminder to parents of every batch with class today">
              <Bell /> Send today's reminders
            </Button>
            <Button onClick={() => setEmailOpen(true)} disabled={perBatch.length === 0} data-testid="calendar-send-schedule"><Send /> Send schedule to parents</Button>
          </>
        }
      />

      {locked && (
        <p className="mb-3 rounded-md border bg-muted px-3 py-2 text-sm text-muted-foreground" role="status" data-testid="month-locked">
          {formatMonth(month)} is a past month, so its class schedule is view only.
        </p>
      )}
      {q.isLoading ? (
        <TableSkeleton rows={6} />
      ) : q.isError ? (
        <ErrorState error={q.error} onRetry={() => q.refetch()} />
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border bg-card" data-testid="class-calendar">
            <div className="grid min-w-[42rem] grid-cols-7">
              {WEEKDAYS.map((d) => (
                <div key={d.value} className="border-b bg-muted px-2 py-1.5 text-center text-xs font-semibold uppercase text-muted-foreground">{d.short}</div>
              ))}
              {weeks.flat().map((date, i) => {
                if (!date) return <div key={`pad-${i}`} className="min-h-24 border-b border-r bg-muted/30 last:border-r-0" />
                const sessions = byDate.get(date) ?? []
                return (
                  <div
                    key={date}
                    className={cn('group min-h-24 border-b border-r p-1.5 text-left transition-colors [&:nth-child(7n)]:border-r-0', !locked && 'cursor-pointer hover:bg-primary/5', date === today && 'bg-primary/5')}
                    onClick={locked ? undefined : () => setEditing({ session: null, date })}
                    data-testid={`calendar-day-${date}`}
                  >
                    <div className="mb-1 flex items-center justify-between">
                      <span className={cn('text-xs font-semibold', date === today && 'rounded-full bg-primary px-1.5 text-primary-foreground')}>{Number(date.slice(8))}</span>
                      {!locked && <Plus className="size-3.5 text-muted-foreground opacity-0 group-hover:opacity-100" aria-hidden />}
                    </div>
                    <div className="space-y-1">
                      {sessions.map((s) => (
                        <button
                          key={s.id}
                          type="button"
                          className={cn('block w-full truncate rounded border px-1.5 py-0.5 text-left text-[11px] font-medium leading-tight', colourOf(s.classGroupId), locked && 'cursor-default')}
                          onClick={(e) => {
                            e.stopPropagation()
                            if (!locked) setEditing({ session: s, date })
                          }}
                          title={`${s.classGroupName} ${s.startTime}–${s.endTime}${s.location ? ` · ${s.location}` : ''}${s.notes ? ` · ${s.notes}` : ''}`}
                          data-testid={`session-${s.classGroupName}-${date}`}
                        >
                          {groupId === '' && <span className="font-semibold">{s.classGroupName} </span>}
                          {s.startTime}
                        </button>
                      ))}
                    </div>
                  </div>
                )
              })}
            </div>
          </div>
          <p className="mt-3 text-sm text-muted-foreground" data-testid="calendar-summary">
            {perBatch.length === 0
              ? (locked ? 'No classes were scheduled this month.' : 'No classes yet this month. Click a day to add one, or use Repeat weekly.')
              : perBatch.map(([, b]) => `${b.name}: ${b.count} class${b.count === 1 ? '' : 'es'}`).join(' · ')}
          </p>
          {reminders.data && (
            <p className="mt-1 flex items-center gap-1.5 text-sm text-muted-foreground" data-testid="reminder-summary">
              <Bell className="size-3.5" aria-hidden /> Automatic reminders: {describeReminders(reminders.data)}
              <button type="button" className="text-primary underline-offset-2 hover:underline" onClick={() => setSettingsOpen(true)}>Change</button>
            </p>
          )}
        </>
      )}

      <SessionDialog open={!!editing} onOpenChange={(o) => !o && setEditing(null)} session={editing?.session ?? null} defaults={defaults} onChanged={offerNotice} onReview={setReview} />
      <ReminderSettingsDialog open={settingsOpen} onOpenChange={setSettingsOpen} current={reminders.data} />
      <ReviewMessagesDialog request={review} onClose={() => setReview(null)} />
      <RepeatDialog open={repeatOpen} onOpenChange={setRepeatOpen} month={month} groupId={groupId} />
      <SendScheduleDialog open={emailOpen} onOpenChange={setEmailOpen} month={month} batches={perBatch} onReview={setReview} />
    </div>
  )
}
