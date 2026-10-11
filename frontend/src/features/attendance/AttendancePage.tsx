import { useCallback, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Download, Loader2, Save, Undo2 } from 'lucide-react'
import { toast } from 'sonner'
import { attendanceApi } from '@/api/attendanceApi'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { PageHeader } from '@/components/PageHeader'
import { EmptyState, ErrorState, TableSkeleton } from '@/components/States'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { NativeSelect } from '@/components/ui/native-select'
import { useMonthParam } from '@/hooks/useMonthParam'
import { useClassGroups } from '@/hooks/useReferenceData'
import { useUnsavedChangesWarning } from '@/hooks/useUnsavedChangesWarning'
import { formatMonth } from '@/lib/dates'
import { errorMessage } from '@/lib/errors'
import { AttendanceGrid } from './AttendanceGrid'
import { countToMark, editKey, parseEditKey, type CellValue } from './gridLogic'

export function AttendancePage() {
  const [month] = useMonthParam()
  const [groupId, setGroupId] = useState<number | ''>('')
  const groups = useClassGroups()
  const queryClient = useQueryClient()
  // Pending edits; an entry equal to the server value is dropped so the counter stays honest.
  const [edits, setEdits] = useState<Map<string, CellValue>>(new Map())
  // Server value of each edited cell, captured on first edit (idempotent, so safe in StrictMode updaters).
  const original = useRef<Map<string, CellValue>>(new Map())

  const q = useQuery({ queryKey: ['attendance', month, groupId], queryFn: () => attendanceApi.grid(month, groupId) })

  const dirty = edits.size > 0
  const toMark = q.data ? countToMark(q.data, edits) : 0
  const blocker = useUnsavedChangesWarning(dirty)

  const onChange = useCallback(
    (changes: { studentId: number; date: string; value: CellValue }[]) => {
      if (!q.data) return
      const grid = q.data
      setEdits((prev) => {
        const next = new Map(prev)
        const orig = original.current
        for (const c of changes) {
          const key = editKey(c.studentId, c.date)
          if (!orig.has(key)) {
            const row = grid.rows.find((r) => r.studentId === c.studentId)
            const dayIndex = grid.days.findIndex((d) => d.date === c.date)
            orig.set(key, row && dayIndex >= 0 ? row.cells[dayIndex] : null)
          }
          if (orig.get(key) === c.value) next.delete(key)
          else next.set(key, c.value)
        }
        return next
      })
    },
    [q.data],
  )

  const save = useMutation({
    mutationFn: () =>
      attendanceApi.bulk([...edits.entries()].map(([key, status]) => ({ ...parseEditKey(key), status }))),
    onSuccess: (r) => {
      toast.success(`Attendance saved (${r.created} added, ${r.updated} changed, ${r.removed} cleared)`)
      setEdits(new Map())
      original.current = new Map()
      queryClient.invalidateQueries({ queryKey: ['attendance'] })
      queryClient.invalidateQueries({ queryKey: ['attendance-history'] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['fees'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const discard = () => {
    setEdits(new Map())
    original.current = new Map()
  }

  const exportXlsx = async () => {
    try {
      await attendanceApi.exportXlsx(month, groupId)
    } catch (e) {
      toast.error(errorMessage(e))
    }
  }

  return (
    <div>
      <PageHeader
        title="Attendance"
        description={`${formatMonth(month)} — click a cell to cycle S/blank → P → A, or use the keyboard (P, A, Delete, arrows). S days come from the Class Calendar.`}
        actions={
          <>
            <NativeSelect aria-label="Filter by class group" className="w-44" value={groupId} onChange={(e) => setGroupId(e.target.value ? Number(e.target.value) : '')} data-testid="attendance-group">
              <option value="">All groups</option>
              {groups.data?.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
            </NativeSelect>
            <Button variant="outline" onClick={exportXlsx} data-testid="export-attendance"><Download /> Export Excel</Button>
            {dirty && (
              <>
                <Badge variant="amber" data-testid="unsaved-count">{edits.size} unsaved change{edits.size === 1 ? '' : 's'}</Badge>
                <Button variant="ghost" onClick={discard} disabled={save.isPending}><Undo2 /> Discard</Button>
              </>
            )}
            <Button onClick={() => save.mutate()} disabled={!dirty || save.isPending} data-testid="save-attendance">
              {save.isPending ? <Loader2 className="animate-spin" /> : <Save />} Save
            </Button>
          </>
        }
      />
      <div className="mb-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-muted-foreground" data-testid="attendance-legend">
        <span className="flex items-center gap-1.5"><span className="inline-flex size-5 items-center justify-center rounded bg-sky-50 font-bold text-sky-700">S</span> Scheduled</span>
        <span className="flex items-center gap-1.5"><span className="inline-flex size-5 items-center justify-center rounded bg-amber-100 font-bold text-amber-800">S</span> Class held, not marked</span>
        <span className="flex items-center gap-1.5"><span className="inline-flex size-5 items-center justify-center rounded bg-green-100 font-bold text-green-800">P</span> Present</span>
        <span className="flex items-center gap-1.5"><span className="inline-flex size-5 items-center justify-center rounded bg-red-100 font-bold text-red-800">A</span> Absent</span>
        {toMark > 0 && <Badge variant="amber" data-testid="to-mark-count">{toMark} to mark</Badge>}
      </div>
      {q.isLoading ? (
        <TableSkeleton rows={8} />
      ) : q.isError ? (
        <ErrorState error={q.error} onRetry={() => q.refetch()} />
      ) : q.data && q.data.rows.length === 0 ? (
        <EmptyState title="No active students" description="Only active students appear in the attendance grid." />
      ) : q.data ? (
        <AttendanceGrid grid={q.data} edits={edits} onChange={onChange} />
      ) : null}

      <ConfirmDialog
        open={blocker.state === 'blocked'}
        onOpenChange={(o) => !o && blocker.state === 'blocked' && blocker.reset()}
        destructive
        title="Leave without saving?"
        description={`You have ${edits.size} unsaved attendance change(s). They will be lost.`}
        confirmLabel="Leave"
        onConfirm={() => blocker.state === 'blocked' && blocker.proceed()}
      />
    </div>
  )
}
