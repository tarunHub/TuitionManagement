import { Fragment, useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { AlertTriangle, CalendarSync, ChevronDown, ChevronRight, Loader2, Pencil, Plus, Send, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { notificationsApi } from '@/api/notificationsApi'
import { scheduleApi } from '@/api/scheduleApi'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { FormField } from '@/components/FormField'
import { PageHeader } from '@/components/PageHeader'
import { EmptyState, ErrorState, TableSkeleton } from '@/components/States'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { useClassGroups } from '@/hooks/useReferenceData'
import { formatDate } from '@/lib/dates'
import { applyServerErrors, errorMessage } from '@/lib/errors'
import { SCHEDULE_DAYS, type ScheduleDay, type Slot, type SlotInput, type Term } from '@/types'

const DAY_NAMES: Record<ScheduleDay, string> = { MON: 'Monday', TUE: 'Tuesday', WED: 'Wednesday', THU: 'Thursday', FRI: 'Friday', SAT: 'Saturday', SUN: 'Sunday' }
const EMAIL_RE = /^[A-Za-z0-9._%+'-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/

// ------------------------------------------------------------------ Term card

const termSchema = z
  .object({
    name: z.string().trim().min(1, 'Required').max(100),
    startDate: z.string().min(1, 'Required'),
    endDate: z.string().min(1, 'Required'),
    calendarName: z.string().max(150).optional(),
  })
  .refine((v) => !v.startDate || !v.endDate || v.endDate > v.startDate, { path: ['endDate'], message: 'Must be after the start date' })
type TermValues = z.infer<typeof termSchema>

function TermCard({ term }: { term: Term | null }) {
  const queryClient = useQueryClient()
  const { register, handleSubmit, reset, setError, formState } = useForm<TermValues>({ resolver: zodResolver(termSchema) })
  useEffect(() => {
    reset({ name: term?.name ?? '', startDate: term?.startDate ?? '', endDate: term?.endDate ?? '', calendarName: term?.calendarName ?? 'Smart Start Classes' })
  }, [term, reset])

  const save = useMutation({
    mutationFn: (v: TermValues) => scheduleApi.saveActiveTerm({ ...v, calendarName: v.calendarName?.trim() || null, active: true }),
    onSuccess: () => {
      toast.success('Term saved')
      queryClient.invalidateQueries({ queryKey: ['term'] })
      queryClient.invalidateQueries({ queryKey: ['schedule-slots'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (e) => {
      if (!applyServerErrors(e, setError, ['name', 'startDate', 'endDate', 'calendarName'])) toast.error(errorMessage(e))
    },
  })
  const e = formState.errors

  return (
    <Card>
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle>Term</CardTitle>
        {term?.active && <Badge variant="green">Active</Badge>}
      </CardHeader>
      <CardContent>
        <form noValidate onSubmit={handleSubmit((v) => save.mutate(v))} className="grid items-end gap-3 sm:grid-cols-2 lg:grid-cols-5" data-testid="term-form">
          <FormField id="term-name" label="Term name" required error={e.name?.message}>
            <Input id="term-name" {...register('name')} placeholder="Fall 2026" />
          </FormField>
          <FormField id="term-start" label="Term start" required error={e.startDate?.message}>
            <Input id="term-start" type="date" {...register('startDate')} />
          </FormField>
          <FormField id="term-end" label="Term end" required error={e.endDate?.message}>
            <Input id="term-end" type="date" {...register('endDate')} />
          </FormField>
          <FormField id="term-calendar" label="Calendar name" error={e.calendarName?.message}>
            <Input id="term-calendar" {...register('calendarName')} />
          </FormField>
          <Button type="submit" disabled={save.isPending} data-testid="term-save">
            {save.isPending && <Loader2 className="animate-spin" />} {term ? 'Save term' : 'Create term'}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}

// ------------------------------------------------------------------ Slot dialog

const slotSchema = z
  .object({
    classGroupId: z.string().min(1, 'Select a class group'),
    dayOfWeek: z.enum(['MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN']),
    startTime: z.string().min(1, 'Required'),
    endTime: z.string().min(1, 'Required'),
    subject: z.string().max(150).optional(),
    location: z.string().max(150).optional(),
    teacher: z.string().max(100).optional(),
    extraEmails: z
      .string()
      .max(4000)
      .optional()
      .refine((v) => !v || v.split(/[,;\s]+/).filter(Boolean).every((x) => EMAIL_RE.test(x)), 'Contains an invalid email address'),
    notes: z.string().max(2000).optional(),
  })
  .refine((v) => !v.startTime || !v.endTime || v.endTime > v.startTime, { path: ['endTime'], message: 'Must be after the start time' })
type SlotValues = z.infer<typeof slotSchema>

function SlotDialog({ slot, open, onOpenChange, allSlots }: { slot: Slot | null; open: boolean; onOpenChange: (o: boolean) => void; allSlots: Slot[] }) {
  const groups = useClassGroups()
  const queryClient = useQueryClient()
  const { register, handleSubmit, reset, setError, watch, formState } = useForm<SlotValues>({ resolver: zodResolver(slotSchema) })

  useEffect(() => {
    if (!open) return
    reset(
      slot
        ? {
            classGroupId: String(slot.classGroupId), dayOfWeek: slot.dayOfWeek, startTime: slot.startTime, endTime: slot.endTime,
            subject: slot.subject ?? '', location: slot.location ?? '', teacher: slot.teacher ?? '', extraEmails: slot.extraEmails ?? '', notes: slot.notes ?? '',
          }
        : { classGroupId: '', dayOfWeek: 'MON', startTime: '17:00', endTime: '18:00', subject: '', location: '', teacher: '', extraEmails: '', notes: 'Weekly' },
    )
  }, [open, slot, reset])

  // Client-side preview of the server's overlap warning.
  const [day, start, end, location] = watch(['dayOfWeek', 'startTime', 'endTime', 'location'])
  const overlaps = allSlots.filter(
    (s) => s.id !== slot?.id && s.dayOfWeek === day && location && s.location?.toLowerCase() === location.trim().toLowerCase() && start < s.endTime && s.startTime < end,
  )

  const save = useMutation({
    mutationFn: (v: SlotValues) => {
      const input: SlotInput = {
        classGroupId: Number(v.classGroupId), dayOfWeek: v.dayOfWeek, startTime: v.startTime, endTime: v.endTime,
        subject: v.subject?.trim() || null, location: v.location?.trim() || null, teacher: v.teacher?.trim() || null,
        extraEmails: v.extraEmails?.trim() || null, notes: v.notes?.trim() || null,
      }
      return slot ? scheduleApi.updateSlot(slot.id, input) : scheduleApi.createSlot(input)
    },
    onSuccess: (s) => {
      if (s.overlapWarning) toast.warning(`Saved, but: ${s.overlapWarning}`)
      else toast.success('Slot saved')
      queryClient.invalidateQueries({ queryKey: ['schedule-slots'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      onOpenChange(false)
    },
    onError: (e) => {
      if (!applyServerErrors(e, setError, ['classGroupId', 'dayOfWeek', 'startTime', 'endTime', 'extraEmails', 'termId'])) toast.error(errorMessage(e))
    },
  })
  const e = formState.errors

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl" data-testid="slot-dialog">
        <DialogHeader>
          <DialogTitle>{slot ? 'Edit slot' : 'Add slot'}</DialogTitle>
        </DialogHeader>
        <form noValidate onSubmit={handleSubmit((v) => save.mutate(v))} className="grid gap-3 sm:grid-cols-2">
          <FormField id="slot-group" label="Group / Class" required error={e.classGroupId?.message}>
            <NativeSelect id="slot-group" {...register('classGroupId')} data-testid="slot-group">
              <option value="">Select…</option>
              {groups.data?.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
            </NativeSelect>
          </FormField>
          <FormField id="slot-day" label="Day" required error={e.dayOfWeek?.message}>
            <NativeSelect id="slot-day" {...register('dayOfWeek')} data-testid="slot-day">
              {SCHEDULE_DAYS.map((d) => <option key={d} value={d}>{DAY_NAMES[d]}</option>)}
            </NativeSelect>
          </FormField>
          <FormField id="slot-start" label="Start" required error={e.startTime?.message}>
            <Input id="slot-start" type="time" {...register('startTime')} data-testid="slot-start" />
          </FormField>
          <FormField id="slot-end" label="End" required error={e.endTime?.message}>
            <Input id="slot-end" type="time" {...register('endTime')} data-testid="slot-end" />
          </FormField>
          <FormField id="slot-subject" label="Subject">
            <Input id="slot-subject" {...register('subject')} />
          </FormField>
          <FormField id="slot-location" label="Location">
            <Input id="slot-location" {...register('location')} />
          </FormField>
          <FormField id="slot-teacher" label="Teacher">
            <Input id="slot-teacher" {...register('teacher')} />
          </FormField>
          <FormField id="slot-notes" label="Notes">
            <Input id="slot-notes" {...register('notes')} />
          </FormField>
          <FormField id="slot-extra" label="Extra emails" hint="Comma separated; added to the group's parent emails" error={e.extraEmails?.message} className="sm:col-span-2">
            <Textarea id="slot-extra" rows={2} {...register('extraEmails')} data-testid="slot-extra-emails" />
          </FormField>
          {overlaps.length > 0 && (
            <p role="status" className="flex items-center gap-2 rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900 sm:col-span-2" data-testid="overlap-warning">
              <AlertTriangle className="h-4 w-4 shrink-0" />
              Overlaps at {location} with {overlaps.map((o) => `${o.classGroupName} ${o.startTime}–${o.endTime}`).join(', ')}
            </p>
          )}
          <DialogFooter className="sm:col-span-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" disabled={save.isPending} data-testid="slot-save">{save.isPending && <Loader2 className="animate-spin" />} Save slot</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}

// ------------------------------------------------------------------ Recipients + weekly view

function RecipientsRow({ slotId, colSpan }: { slotId: number; colSpan: number }) {
  const q = useQuery({ queryKey: ['slot-recipients', slotId], queryFn: () => scheduleApi.recipients(slotId) })
  return (
    <TableRow className="bg-muted/30 hover:bg-muted/30">
      <TableCell colSpan={colSpan} className="text-sm" data-testid={`recipients-${slotId}`}>
        {q.isLoading ? 'Loading…' : q.isError ? errorMessage(q.error) : q.data!.emails.length === 0 ? (
          <span className="text-muted-foreground">No recipients: the group has no active students and no extra emails.</span>
        ) : (
          <>
            <span className="font-medium">{q.data!.emails.length} recipient(s)</span>
            <span className="text-muted-foreground"> ({q.data!.fromGroup} from active students, {q.data!.extra} extra): </span>
            {q.data!.emails.join(', ')}
          </>
        )}
      </TableCell>
    </TableRow>
  )
}

function WeeklyView({ slots }: { slots: Slot[] }) {
  return (
    <div className="grid gap-2 md:grid-cols-7" data-testid="weekly-view">
      {SCHEDULE_DAYS.map((d) => {
        const daySlots = slots.filter((s) => s.dayOfWeek === d)
        return (
          <div key={d} className="min-h-32 rounded-lg border bg-card">
            <div className="border-b bg-muted/60 px-2 py-1.5 text-xs font-semibold uppercase">{DAY_NAMES[d]}</div>
            <div className="space-y-2 p-2">
              {daySlots.length === 0 && <p className="text-xs text-muted-foreground">—</p>}
              {daySlots.map((s) => (
                <div key={s.id} className="rounded-md border-l-4 border-primary bg-primary/5 p-2 text-xs">
                  <p className="font-semibold">{s.startTime}–{s.endTime}</p>
                  <p>{s.classGroupName}</p>
                  {s.subject && <p className="text-muted-foreground">{s.subject}</p>}
                  {s.location && <p className="text-muted-foreground">{s.location}</p>}
                  {s.overlapWarning && <p className="mt-1 flex items-center gap-1 text-amber-800"><AlertTriangle className="h-3 w-3" /> Overlap</p>}
                </div>
              ))}
            </div>
          </div>
        )
      })}
    </div>
  )
}

// ------------------------------------------------------------------ Page

export function SchedulePage() {
  const queryClient = useQueryClient()
  const term = useQuery({ queryKey: ['term', 'active'], queryFn: scheduleApi.activeTerm })
  const slots = useQuery({ queryKey: ['schedule-slots', term.data?.id], queryFn: () => scheduleApi.slots(), enabled: term.isSuccess })
  const google = useQuery({ queryKey: ['google-status'], queryFn: scheduleApi.googleStatus, staleTime: 5 * 60_000 })
  const [expanded, setExpanded] = useState<Set<number>>(new Set())
  const [editing, setEditing] = useState<Slot | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const [deleting, setDeleting] = useState<Slot | null>(null)

  const sync = useMutation({
    mutationFn: scheduleApi.syncGoogle,
    onSuccess: (r) => {
      const msg = `Synced to "${r.calendarName}": ${r.created} created, ${r.updated} updated${r.skipped ? `, ${r.skipped} skipped` : ''}`
      if (r.errors.length) toast.warning(`${msg}; ${r.errors.length} error(s): ${r.errors.join('; ')}`)
      else toast.success(msg)
      queryClient.invalidateQueries({ queryKey: ['schedule-slots'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const broadcast = useMutation({
    mutationFn: () => notificationsApi.scheduleBroadcast(),
    onSuccess: (r) => {
      if (r.failed) toast.warning(`${r.sent} schedule email(s) sent, ${r.failed} failed — see Notifications`)
      else toast.success(`Weekly schedule sent to ${r.sent} class group(s)`)
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const remove = useMutation({
    mutationFn: (id: number) => scheduleApi.deleteSlot(id),
    onSuccess: () => {
      toast.success('Slot deleted')
      setDeleting(null)
      queryClient.invalidateQueries({ queryKey: ['schedule-slots'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const toggle = (id: number) =>
    setExpanded((prev) => {
      const next = new Set(prev)
      if (next.has(id)) next.delete(id)
      else next.add(id)
      return next
    })

  const data = slots.data ?? []
  const openDialog = (s: Slot | null) => {
    setEditing(s)
    setDialogOpen(true)
  }

  return (
    <div className="space-y-4">
      <PageHeader
        title="Schedule"
        description={term.data ? `${term.data.name}: ${formatDate(term.data.startDate)} – ${formatDate(term.data.endDate)}` : 'Configure the term, then add weekly class slots.'}
        actions={
          <>
            {google.data?.enabled && (
              <Button variant="outline" onClick={() => sync.mutate()} disabled={sync.isPending || !data.length} data-testid="sync-google">
                {sync.isPending ? <Loader2 className="animate-spin" /> : <CalendarSync />} Sync to Google Calendar
              </Button>
            )}
            <Button variant="outline" onClick={() => broadcast.mutate()} disabled={broadcast.isPending || !data.length} data-testid="send-schedule">
              {broadcast.isPending ? <Loader2 className="animate-spin" /> : <Send />} Email schedule to parents
            </Button>
            <Button onClick={() => openDialog(null)} disabled={!term.data} data-testid="add-slot"><Plus /> Add slot</Button>
          </>
        }
      />
      {term.isLoading ? <TableSkeleton rows={2} /> : term.isError ? <ErrorState error={term.error} /> : <TermCard term={term.data ?? null} />}

      {term.data && (
        <Tabs defaultValue="table">
          <TabsList>
            <TabsTrigger value="table">Slots</TabsTrigger>
            <TabsTrigger value="week" data-testid="tab-weekly">Weekly view</TabsTrigger>
          </TabsList>
          <TabsContent value="table">
            {slots.isLoading ? <TableSkeleton /> : slots.isError ? <ErrorState error={slots.error} /> : data.length === 0 ? (
              <EmptyState title="No slots yet" description="Add the weekly classes for this term." />
            ) : (
              <div className="rounded-lg border bg-card">
                <Table data-testid="slots-table">
                  <TableHeader>
                    <TableRow>
                      <TableHead>Group / Class</TableHead><TableHead>Day</TableHead><TableHead>Start</TableHead><TableHead>End</TableHead>
                      <TableHead>Subject</TableHead><TableHead>Location</TableHead><TableHead>Teacher</TableHead><TableHead>Recipients</TableHead>
                      <TableHead>Notes</TableHead><TableHead />
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {data.map((s) => (
                      <Fragment key={s.id}>
                        <TableRow data-testid={`slot-row-${s.id}`}>
                          <TableCell className="font-medium">{s.classGroupName}</TableCell>
                          <TableCell>{DAY_NAMES[s.dayOfWeek]}</TableCell>
                          <TableCell>{s.startTime}</TableCell>
                          <TableCell>{s.endTime}</TableCell>
                          <TableCell>{s.subject || '—'}</TableCell>
                          <TableCell>
                            {s.location || '—'}
                            {s.overlapWarning && (
                              <Badge variant="amber" className="ml-2" title={s.overlapWarning}><AlertTriangle className="h-3 w-3" /> Overlap</Badge>
                            )}
                          </TableCell>
                          <TableCell>{s.teacher || '—'}</TableCell>
                          <TableCell>
                            <button type="button" className="inline-flex items-center gap-1 text-primary hover:underline" aria-expanded={expanded.has(s.id)} onClick={() => toggle(s.id)} data-testid={`toggle-recipients-${s.id}`}>
                              {expanded.has(s.id) ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                              {s.recipientCount}
                            </button>
                          </TableCell>
                          <TableCell>{s.notes || '—'}</TableCell>
                          <TableCell className="whitespace-nowrap text-right">
                            <Button size="icon" variant="ghost" aria-label="Edit slot" onClick={() => openDialog(s)}><Pencil /></Button>
                            <Button size="icon" variant="ghost" aria-label="Delete slot" onClick={() => setDeleting(s)}><Trash2 /></Button>
                          </TableCell>
                        </TableRow>
                        {expanded.has(s.id) && <RecipientsRow slotId={s.id} colSpan={10} />}
                      </Fragment>
                    ))}
                  </TableBody>
                </Table>
              </div>
            )}
          </TabsContent>
          <TabsContent value="week"><WeeklyView slots={data} /></TabsContent>
        </Tabs>
      )}

      <SlotDialog slot={editing} open={dialogOpen} onOpenChange={setDialogOpen} allSlots={data} />
      <ConfirmDialog
        open={!!deleting}
        onOpenChange={(o) => !o && setDeleting(null)}
        destructive
        title="Delete slot?"
        description={deleting ? `${deleting.classGroupName} on ${DAY_NAMES[deleting.dayOfWeek]} ${deleting.startTime}–${deleting.endTime} will be removed${deleting.googleEventId ? ', along with its Google Calendar event' : ''}.` : ''}
        confirmLabel="Delete"
        pending={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting.id)}
      />
    </div>
  )
}
