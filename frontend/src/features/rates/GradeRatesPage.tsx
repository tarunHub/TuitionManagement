import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Check, Pencil, Plus, Trash2, X } from 'lucide-react'
import { toast } from 'sonner'
import { gradeRatesApi, type GradeRateInput } from '@/api/referenceApi'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { PageHeader } from '@/components/PageHeader'
import { ErrorState, TableSkeleton } from '@/components/States'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useGradeRates } from '@/hooks/useReferenceData'
import { apiError, errorMessage } from '@/lib/errors'
import { formatMoney } from '@/lib/money'
import type { GradeRate } from '@/types'

interface Draft {
  grade: string
  perClassRate: string
  notes: string
}

const toDraft = (g?: GradeRate): Draft => ({ grade: g?.grade ?? '', perClassRate: g ? String(g.perClassRate) : '', notes: g?.notes ?? '' })

function validate(d: Draft): string | null {
  if (!d.grade.trim()) return 'Grade is required'
  const rate = Number(d.perClassRate)
  if (!d.perClassRate || Number.isNaN(rate) || rate <= 0) return 'Rate must be greater than 0'
  return null
}

const toInput = (d: Draft): GradeRateInput => ({ grade: d.grade.trim(), perClassRate: Number(d.perClassRate), notes: d.notes.trim() || null })

export function GradeRatesPage() {
  const q = useGradeRates()
  const queryClient = useQueryClient()
  const [editingId, setEditingId] = useState<number | 'new' | null>(null)
  const [draft, setDraft] = useState<Draft>(toDraft())
  const [error, setError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState<GradeRate | null>(null)
  const [blockedBy, setBlockedBy] = useState<{ studentCode: string; fullName: string }[] | null>(null)

  const done = () => {
    setEditingId(null)
    setError(null)
    queryClient.invalidateQueries({ queryKey: ['grade-rates'] })
  }

  const save = useMutation({
    mutationFn: () => (editingId === 'new' ? gradeRatesApi.create(toInput(draft)) : gradeRatesApi.update(editingId as number, toInput(draft))),
    onSuccess: () => {
      toast.success('Rate saved. Existing students and generated fees keep their current rate.')
      done()
    },
    onError: (e) => setError(errorMessage(e)),
  })

  const remove = useMutation({
    mutationFn: (id: number) => gradeRatesApi.remove(id),
    onSuccess: () => {
      toast.success('Grade deleted')
      setDeleting(null)
      done()
    },
    onError: (e) => {
      const api = apiError(e)
      if (api?.error === 'GRADE_IN_USE') setBlockedBy(api.details as { studentCode: string; fullName: string }[])
      else toast.error(errorMessage(e))
      setDeleting(null)
    },
  })

  const startEdit = (g?: GradeRate) => {
    setEditingId(g ? g.id : 'new')
    setDraft(toDraft(g))
    setError(null)
  }

  const submit = () => {
    const v = validate(draft)
    if (v) setError(v)
    else save.mutate()
  }

  const editRow = (key: string) => (
    <TableRow key={key} data-testid="rate-edit-row">
      <TableCell>
        <Input aria-label="Grade" value={draft.grade} onChange={(e) => setDraft({ ...draft, grade: e.target.value })} data-testid="rate-grade" autoFocus />
      </TableCell>
      <TableCell>
        <Input aria-label="Per-class rate" type="number" min="0.01" step="0.01" value={draft.perClassRate} onChange={(e) => setDraft({ ...draft, perClassRate: e.target.value })} data-testid="rate-amount"
          onKeyDown={(e) => e.key === 'Enter' && submit()} />
      </TableCell>
      <TableCell>
        <Input aria-label="Notes" value={draft.notes} onChange={(e) => setDraft({ ...draft, notes: e.target.value })} data-testid="rate-notes" onKeyDown={(e) => e.key === 'Enter' && submit()} />
        {error && <p role="alert" className="mt-1 text-xs text-destructive">{error}</p>}
      </TableCell>
      <TableCell />
      <TableCell className="text-right">
        <div className="flex justify-end gap-1">
          <Button size="icon" variant="ghost" aria-label="Save" onClick={submit} disabled={save.isPending} data-testid="rate-save"><Check /></Button>
          <Button size="icon" variant="ghost" aria-label="Cancel" onClick={() => setEditingId(null)}><X /></Button>
        </div>
      </TableCell>
    </TableRow>
  )

  return (
    <div>
      <PageHeader
        title="Fee Rates"
        description="Per-class rate by grade. New students default to their grade's rate; changes never alter existing students or generated fees."
        actions={<Button onClick={() => startEdit()} disabled={editingId !== null} data-testid="add-rate"><Plus /> Add grade</Button>}
      />
      {q.isLoading ? <TableSkeleton /> : q.isError ? <ErrorState error={q.error} onRetry={() => q.refetch()} /> : (
        <div className="rounded-lg border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-48">Grade</TableHead>
                <TableHead className="w-48">Per-Class Rate ($)</TableHead>
                <TableHead>Notes</TableHead>
                <TableHead className="w-28">Students</TableHead>
                <TableHead className="w-28 text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {editingId === 'new' && editRow('new')}
              {q.data?.map((g) =>
                editingId === g.id ? editRow(String(g.id)) : (
                  <TableRow key={g.id} data-testid={`rate-row-${g.grade}`} onDoubleClick={() => editingId === null && startEdit(g)}>
                    <TableCell className="font-medium">{g.grade}</TableCell>
                    <TableCell>{formatMoney(g.perClassRate)}</TableCell>
                    <TableCell className="text-muted-foreground">{g.notes || '—'}</TableCell>
                    <TableCell>{g.studentCount}</TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-1">
                        <Button size="icon" variant="ghost" aria-label={`Edit ${g.grade}`} onClick={() => startEdit(g)} disabled={editingId !== null}><Pencil /></Button>
                        <Button size="icon" variant="ghost" aria-label={`Delete ${g.grade}`} onClick={() => setDeleting(g)} disabled={editingId !== null}><Trash2 /></Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ),
              )}
            </TableBody>
          </Table>
        </div>
      )}
      <ConfirmDialog
        open={!!deleting}
        onOpenChange={(o) => !o && setDeleting(null)}
        destructive
        title={`Delete ${deleting?.grade}?`}
        description={deleting?.studentCount ? `${deleting.studentCount} student(s) use this grade, so it cannot be deleted.` : 'This grade rate will be removed.'}
        confirmLabel="Delete"
        pending={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting.id)}
      />
      <ConfirmDialog
        open={!!blockedBy}
        onOpenChange={(o) => !o && setBlockedBy(null)}
        title="Grade in use"
        description={
          <div>
            <p className="mb-2">This grade cannot be deleted because these students use it:</p>
            <ul className="list-disc pl-5" data-testid="grade-in-use-list">
              {blockedBy?.map((s) => <li key={s.studentCode}>{s.studentCode} — {s.fullName}</li>)}
            </ul>
          </div>
        }
        confirmLabel="OK"
        onConfirm={() => setBlockedBy(null)}
      />
    </div>
  )
}
