import { useEffect, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { format } from 'date-fns'
import { Loader2, Mail, Plus, Repeat, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { classCalendarApi } from '@/api/classCalendarApi'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { FormField } from '@/components/FormField'
import { PageHeader } from '@/components/PageHeader'
import { ErrorState, TableSkeleton } from '@/components/States'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { useMonthParam } from '@/hooks/useMonthParam'
import { useClassGroups } from '@/hooks/useReferenceData'
import { formatDate, formatMonth, monthToDate, todayIso } from '@/lib/dates'
import { applyServerErrors, errorMessage } from '@/lib/errors'
import { cn } from '@/lib/utils'
import type { ClassSession, Weekday } from '@/types'

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
function SessionDialog({ open, onOpenChange, session, defaults }: {
  open: boolean
  onOpenChange: (o: boolean) => void
  session: ClassSession | null
  defaults: { date: string; classGroupId: number | '' ; startTime: string; endTime: string }
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
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const e = formState.errors
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg" data-testid="session-dialog">
        <DialogHeader>
          <DialogTitle>{session ? 'Edit class' : 'Add class'}</DialogTitle>
          <DialogDescription>Shows as S on the Attendance page for every student in the batch.</DialogDescription>
        </DialogHeader>
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

export function ClassCalendarPage() {
  const [month] = useMonthParam()
  const [groupId, setGroupId] = useState<number | ''>('')
  const groups = useClassGroups()
  const queryClient = useQueryClient()
  const q = useQuery({ queryKey: ['class-calendar', month, groupId], queryFn: () => classCalendarApi.list(month, groupId) })

  const [editing, setEditing] = useState<{ session: ClassSession | null; date: string } | null>(null)
  const [repeatOpen, setRepeatOpen] = useState(false)
  const [emailOpen, setEmailOpen] = useState(false)

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

  const email = useMutation({
    mutationFn: () => classCalendarApi.email(month, perBatch.map(([id]) => id)),
    onSuccess: (r) => {
      if (r.failed) toast.warning(`${r.sent} schedule email(s) sent, ${r.failed} failed — see Notifications to retry`)
      else toast.success(`${formatMonth(month)} schedule emailed for ${r.sent} batch${r.sent === 1 ? '' : 'es'}`)
      setEmailOpen(false)
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
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
            <Button variant="outline" onClick={() => setRepeatOpen(true)} data-testid="calendar-repeat"><Repeat /> Repeat weekly</Button>
            <Button variant="outline" onClick={() => setEditing({ session: null, date: `${month}-01` })} data-testid="calendar-add"><Plus /> Add class</Button>
            <Button onClick={() => setEmailOpen(true)} disabled={perBatch.length === 0} data-testid="calendar-email"><Mail /> Email schedule to parents</Button>
          </>
        }
      />

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
                    className={cn('group min-h-24 cursor-pointer border-b border-r p-1.5 text-left transition-colors hover:bg-primary/5 [&:nth-child(7n)]:border-r-0', date === today && 'bg-primary/5')}
                    onClick={() => setEditing({ session: null, date })}
                    data-testid={`calendar-day-${date}`}
                  >
                    <div className="mb-1 flex items-center justify-between">
                      <span className={cn('text-xs font-semibold', date === today && 'rounded-full bg-primary px-1.5 text-primary-foreground')}>{Number(date.slice(8))}</span>
                      <Plus className="size-3.5 text-muted-foreground opacity-0 group-hover:opacity-100" aria-hidden />
                    </div>
                    <div className="space-y-1">
                      {sessions.map((s) => (
                        <button
                          key={s.id}
                          type="button"
                          className={cn('block w-full truncate rounded border px-1.5 py-0.5 text-left text-[11px] font-medium leading-tight', colourOf(s.classGroupId))}
                          onClick={(e) => {
                            e.stopPropagation()
                            setEditing({ session: s, date })
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
              ? 'No classes yet this month. Click a day to add one, or use Repeat weekly.'
              : perBatch.map(([, b]) => `${b.name}: ${b.count} class${b.count === 1 ? '' : 'es'}`).join(' · ')}
          </p>
        </>
      )}

      <SessionDialog open={!!editing} onOpenChange={(o) => !o && setEditing(null)} session={editing?.session ?? null} defaults={defaults} />
      <RepeatDialog open={repeatOpen} onOpenChange={setRepeatOpen} month={month} groupId={groupId} />
      <ConfirmDialog
        open={emailOpen}
        onOpenChange={setEmailOpen}
        title={`Email the ${formatMonth(month)} schedule?`}
        description={
          <div className="space-y-2">
            <p>Each batch's parents get one email listing that batch's class dates and times:</p>
            <ul className="list-disc pl-5">
              {perBatch.map(([id, b]) => <li key={id}>{b.name} — {b.count} class{b.count === 1 ? '' : 'es'}</li>)}
            </ul>
            <p className="text-xs">Only parents of active students with an email on file. Sent emails appear under Notifications.</p>
          </div>
        }
        confirmLabel="Send emails"
        pending={email.isPending}
        onConfirm={() => email.mutate()}
      />
    </div>
  )
}
