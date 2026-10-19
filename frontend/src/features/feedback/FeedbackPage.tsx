import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ColumnDef } from '@tanstack/react-table'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Copy, Loader2, Mail, Pencil, Printer, Sparkles, Trash2, Undo2 } from 'lucide-react'
import { toast } from 'sonner'
import { chatApi } from '@/api/chatApi'
import { feedbackApi } from '@/api/feedbackApi'
import { notificationsApi } from '@/api/notificationsApi'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { DataTable } from '@/components/DataTable'
import { FormField } from '@/components/FormField'
import { PageHeader } from '@/components/PageHeader'
import { ErrorState } from '@/components/States'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { NativeSelect } from '@/components/ui/native-select'
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle } from '@/components/ui/sheet'
import { Textarea } from '@/components/ui/textarea'
import { useChat } from '@/features/chat/ChatContext'
import { useMonthParam } from '@/hooks/useMonthParam'
import { useClassGroups } from '@/hooks/useReferenceData'
import { formatMonth, isPastMonth, shiftMonth } from '@/lib/dates'
import { applyServerErrors, errorMessage } from '@/lib/errors'
import type { FeedbackRow } from '@/types'

const schema = z.object({
  topicsTaught: z.string().trim().min(1, 'Topics taught is required').max(8000),
  improvementAreas: z.string().max(8000).optional(),
  achievements: z.string().max(8000).optional(),
})
type Values = z.infer<typeof schema>

/**
 * Rough notes in, parent-ready wording out: the local model rewrites the notes (and anything already typed) into the
 * three boxes. Nothing is saved until the admin reviews it and presses Save, and the previous text can be restored.
 */
function AiDraft({ studentId, month, current, onDraft }: {
  studentId: number
  month: string
  current: () => Values
  onDraft: (v: Values) => void
}) {
  const { status, unavailable } = useChat()
  const [notes, setNotes] = useState('')
  const [before, setBefore] = useState<Values | null>(null)

  useEffect(() => {
    setNotes('')
    setBefore(null)
  }, [studentId, month])

  const draft = useMutation({
    mutationFn: () => {
      const v = current()
      return chatApi.feedbackDraft({
        studentId,
        month,
        notes,
        topicsTaught: v.topicsTaught ?? '',
        improvementAreas: v.improvementAreas ?? '',
        achievements: v.achievements ?? '',
      })
    },
    onSuccess: (d) => {
      setBefore(current())
      onDraft({ topicsTaught: d.topicsTaught, improvementAreas: d.improvementAreas, achievements: d.achievements })
      toast.success('Draft written — review it, then Save feedback')
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  if (status && !status.enabled) return null

  return (
    <div className="space-y-2 rounded-lg border border-primary/30 bg-primary/5 p-3" data-testid="feedback-ai">
      <label htmlFor="feedback-ai-notes" className="flex items-center gap-2 text-sm font-medium">
        <Sparkles className="size-4 text-primary" /> Write with AI
      </label>
      <Textarea
        id="feedback-ai-notes"
        rows={3}
        maxLength={4000}
        value={notes}
        onChange={(e) => setNotes(e.target.value)}
        placeholder="Rough notes, e.g. did fractions + word problems, gets confused with borrowing, much faster at times tables, missed 2 classes"
        data-testid="feedback-ai-notes"
      />
      <div className="flex flex-wrap items-center gap-2">
        <Button type="button" size="sm" onClick={() => draft.mutate()} disabled={draft.isPending || unavailable} data-testid="feedback-ai-draft">
          {draft.isPending ? <Loader2 className="animate-spin" /> : <Sparkles />} {draft.isPending ? 'Writing…' : 'Draft feedback'}
        </Button>
        {before && !draft.isPending && (
          <Button
            type="button"
            size="sm"
            variant="ghost"
            onClick={() => {
              onDraft(before)
              setBefore(null)
            }}
            data-testid="feedback-ai-undo"
          >
            <Undo2 /> Undo draft
          </Button>
        )}
        <span className="text-xs text-muted-foreground">
          {unavailable ? status?.message : 'Uses your notes and anything already in the boxes below. Takes 5–20 seconds.'}
        </span>
      </div>
    </div>
  )
}

const clip = (s: string | null) => (s ? <span className="line-clamp-2 whitespace-pre-wrap">{s}</span> : <span className="text-muted-foreground">—</span>)

function FeedbackDrawer({ row, month, onClose }: { row: FeedbackRow | null; month: string; onClose: () => void }) {
  const queryClient = useQueryClient()
  const locked = isPastMonth(month)
  const { register, handleSubmit, reset, setValue, getValues, setError, formState } = useForm<Values>({ resolver: zodResolver(schema) })

  useEffect(() => {
    if (row) reset({ topicsTaught: row.topicsTaught ?? '', improvementAreas: row.improvementAreas ?? '', achievements: row.achievements ?? '' })
  }, [row, reset])

  const save = useMutation({
    mutationFn: (v: Values) => feedbackApi.upsert(row!.studentId, month, v),
    onSuccess: () => {
      toast.success(`Feedback saved for ${row?.fullName}`)
      queryClient.invalidateQueries({ queryKey: ['feedback'] })
      queryClient.invalidateQueries({ queryKey: ['feedback-history'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      onClose()
    },
    onError: (e) => {
      if (!applyServerErrors(e, setError, ['topicsTaught', 'improvementAreas', 'achievements'])) toast.error(errorMessage(e))
    },
  })

  return (
    <Sheet open={!!row} onOpenChange={(o) => !o && onClose()}>
      <SheetContent data-testid="feedback-drawer">
        <SheetHeader>
          <SheetTitle>{row?.studentCode} {row?.fullName}</SheetTitle>
          <SheetDescription>
            Feedback for {formatMonth(month)}{locked && ' — past month, view only'}
          </SheetDescription>
        </SheetHeader>
        <form id="feedback-form" className="flex flex-1 flex-col gap-4" noValidate onSubmit={handleSubmit((v) => save.mutate(v))}>
          {row && !locked && (
            <AiDraft
              studentId={row.studentId}
              month={month}
              current={getValues}
              onDraft={(v) => {
                setValue('topicsTaught', v.topicsTaught ?? '', { shouldDirty: true, shouldValidate: !!v.topicsTaught })
                setValue('improvementAreas', v.improvementAreas ?? '', { shouldDirty: true })
                setValue('achievements', v.achievements ?? '', { shouldDirty: true })
              }}
            />
          )}
          <FormField id="topicsTaught" label="Topics taught" required error={formState.errors.topicsTaught?.message}>
            <Textarea id="topicsTaught" rows={5} readOnly={locked} {...register('topicsTaught')} data-testid="feedback-topics" />
          </FormField>
          {row?.previousMonthTopics && !locked && (
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="self-start"
              onClick={() => setValue('topicsTaught', row.previousMonthTopics ?? '', { shouldDirty: true })}
              data-testid="copy-previous-topics"
            >
              <Copy /> Copy topics from {formatMonth(shiftMonth(month, -1))}
            </Button>
          )}
          <FormField id="improvementAreas" label="Improvement areas" error={formState.errors.improvementAreas?.message}>
            <Textarea id="improvementAreas" rows={4} readOnly={locked} {...register('improvementAreas')} data-testid="feedback-improvement" />
          </FormField>
          <FormField id="achievements" label="Achievements" error={formState.errors.achievements?.message}>
            <Textarea id="achievements" rows={4} readOnly={locked} {...register('achievements')} data-testid="feedback-achievements" />
          </FormField>
        </form>
        <SheetFooter>
          <Button variant="outline" onClick={onClose}>{locked ? 'Close' : 'Cancel'}</Button>
          {!locked && (
            <Button type="submit" form="feedback-form" disabled={save.isPending} data-testid="feedback-save">
              {save.isPending && <Loader2 className="animate-spin" />} Save feedback
            </Button>
          )}
        </SheetFooter>
      </SheetContent>
    </Sheet>
  )
}

export function FeedbackPage() {
  const [month] = useMonthParam()
  const locked = isPastMonth(month)
  const [groupId, setGroupId] = useState<number | ''>('')
  const [editing, setEditing] = useState<FeedbackRow | null>(null)
  const [deleting, setDeleting] = useState<FeedbackRow | null>(null)
  const groups = useClassGroups()
  const queryClient = useQueryClient()
  const q = useQuery({ queryKey: ['feedback', month, groupId], queryFn: () => feedbackApi.list(month, groupId) })

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['feedback'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const copyAll = useMutation({
    mutationFn: () => feedbackApi.copyTopics(month),
    onSuccess: (r) => {
      toast.success(r.copied ? `Copied topics for ${r.copied} student(s)` : 'Nothing to copy — every student already has feedback or no previous topics')
      invalidate()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const remove = useMutation({
    mutationFn: (id: number) => feedbackApi.remove(id),
    onSuccess: () => {
      toast.success('Feedback deleted')
      setDeleting(null)
      invalidate()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const email = useMutation({
    mutationFn: (row: FeedbackRow) => notificationsApi.feedbackEmail(row.studentId, month),
    onSuccess: (n) => {
      if (n.status === 'SENT') toast.success('Feedback emailed to parent')
      else toast.error(`Email failed: ${n.errorMessage ?? 'unknown error'} (retry from Notifications)`)
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const missing = q.data?.filter((r) => r.id === null).length ?? 0

  const columns: ColumnDef<FeedbackRow>[] = [
    { accessorKey: 'studentCode', header: 'Student ID', enableHiding: false },
    {
      accessorKey: 'fullName',
      header: 'Name',
      enableHiding: false,
      cell: ({ row }) => (
        <div>
          <p className="font-medium">{row.original.fullName}</p>
          {row.original.id === null && <Badge variant="amber">Not entered</Badge>}
        </div>
      ),
    },
    { accessorKey: 'topicsTaught', header: 'Topics Taught', cell: ({ getValue }) => clip(getValue<string | null>()) },
    { accessorKey: 'improvementAreas', header: 'Improvement Areas', cell: ({ getValue }) => clip(getValue<string | null>()) },
    { accessorKey: 'achievements', header: 'Achievements', cell: ({ getValue }) => clip(getValue<string | null>()) },
    {
      id: 'actions',
      header: '',
      enableHiding: false,
      enableSorting: false,
      meta: { className: 'text-right whitespace-nowrap' },
      cell: ({ row }) => {
        const r = row.original
        return (
          <div className="flex justify-end gap-1" onClick={(e) => e.stopPropagation()}>
            {!locked && <Button size="icon" variant="ghost" aria-label={`Edit feedback for ${r.fullName}`} onClick={() => setEditing(r)} data-testid={`edit-feedback-${r.studentCode}`}><Pencil /></Button>}
            {r.id !== null && (
              <>
                <Button size="icon" variant="ghost" aria-label={`Email feedback to ${r.guardianName}`} disabled={email.isPending} onClick={() => email.mutate(r)} data-testid={`email-feedback-${r.studentCode}`}><Mail /></Button>
                <Button size="icon" variant="ghost" aria-label="Printable report" asChild>
                  <Link to={`/feedback/${r.studentId}/report?month=${month}`}><Printer /></Link>
                </Button>
                {!locked && <Button size="icon" variant="ghost" aria-label={`Delete feedback for ${r.fullName}`} onClick={() => setDeleting(r)}><Trash2 /></Button>}
              </>
            )}
          </div>
        )
      },
    },
  ]

  return (
    <div>
      <PageHeader
        title="Monthly Feedback"
        description={`${formatMonth(month)}${missing && !locked ? ` — ${missing} student(s) still need feedback` : ''}`}
        actions={
          <>
            <NativeSelect aria-label="Filter by class group" className="w-44" value={groupId} onChange={(e) => setGroupId(e.target.value ? Number(e.target.value) : '')}>
              <option value="">All groups</option>
              {groups.data?.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
            </NativeSelect>
            {!locked && (
              <Button variant="outline" onClick={() => copyAll.mutate()} disabled={copyAll.isPending} data-testid="copy-all-topics">
                <Copy /> Copy topics from previous month
              </Button>
            )}
          </>
        }
      />
      {locked && (
        <p className="mb-3 rounded-md border bg-muted px-3 py-2 text-sm text-muted-foreground" role="status" data-testid="month-locked">
          {formatMonth(month)} is a past month, so its feedback is view only.
        </p>
      )}
      {q.isError ? (
        <ErrorState error={q.error} onRetry={() => q.refetch()} />
      ) : (
        <DataTable
          testId="feedback-table"
          columns={columns}
          data={q.data ?? []}
          loading={q.isLoading}
          paginate={false}
          searchPlaceholder="Search student"
          rowTestId={(r) => `feedback-row-${r.studentCode}`}
          onRowClick={setEditing}
          emptyTitle="No active students"
        />
      )}
      <FeedbackDrawer row={editing} month={month} onClose={() => setEditing(null)} />
      <ConfirmDialog
        open={!!deleting}
        onOpenChange={(o) => !o && setDeleting(null)}
        destructive
        title="Delete feedback?"
        description={`${deleting?.fullName}'s feedback for ${formatMonth(month)} will be removed.`}
        confirmLabel="Delete"
        pending={remove.isPending}
        onConfirm={() => deleting?.id && remove.mutate(deleting.id)}
      />
    </div>
  )
}
