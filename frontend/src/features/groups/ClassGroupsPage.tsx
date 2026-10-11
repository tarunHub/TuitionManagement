import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { ColumnDef } from '@tanstack/react-table'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { classGroupsApi } from '@/api/referenceApi'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { DataTable } from '@/components/DataTable'
import { PageHeader } from '@/components/PageHeader'
import { ErrorState } from '@/components/States'
import { FormField } from '@/components/FormField'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { useClassGroups } from '@/hooks/useReferenceData'
import { errorMessage } from '@/lib/errors'
import type { ClassGroup } from '@/types'

export function ClassGroupsPage() {
  const q = useClassGroups()
  const queryClient = useQueryClient()
  const [editing, setEditing] = useState<ClassGroup | 'new' | null>(null)
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [deleting, setDeleting] = useState<ClassGroup | null>(null)

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['class-groups'] })
    queryClient.invalidateQueries({ queryKey: ['schedule-slots'] })
  }

  const open = (g: ClassGroup | 'new') => {
    setEditing(g)
    setName(g === 'new' ? '' : g.name)
    setDescription(g === 'new' ? '' : g.description ?? '')
    setError(null)
  }

  const save = useMutation({
    mutationFn: () => {
      const input = { name: name.trim(), description: description.trim() || null }
      return editing === 'new' ? classGroupsApi.create(input) : classGroupsApi.update((editing as ClassGroup).id, input)
    },
    onSuccess: () => {
      toast.success('Class group saved')
      setEditing(null)
      invalidate()
    },
    onError: (e) => setError(errorMessage(e)),
  })

  const remove = useMutation({
    mutationFn: (id: number) => classGroupsApi.remove(id),
    onSuccess: () => {
      toast.success('Class group deleted')
      setDeleting(null)
      invalidate()
    },
    onError: (e) => {
      toast.error(errorMessage(e))
      setDeleting(null)
    },
  })

  const columns: ColumnDef<ClassGroup>[] = [
    { accessorKey: 'name', header: 'Name', enableHiding: false, cell: ({ getValue }) => <span className="font-medium">{getValue<string>()}</span> },
    { accessorKey: 'description', header: 'Description', cell: ({ getValue }) => getValue<string>() || '—' },
    { accessorKey: 'activeStudentCount', header: 'Active students' },
    {
      id: 'actions',
      header: '',
      enableHiding: false,
      enableSorting: false,
      meta: { className: 'text-right' },
      cell: ({ row }) => (
        <div className="flex justify-end gap-1">
          <Button size="icon" variant="ghost" aria-label={`Edit ${row.original.name}`} onClick={() => open(row.original)}><Pencil /></Button>
          <Button size="icon" variant="ghost" aria-label={`Delete ${row.original.name}`} onClick={() => setDeleting(row.original)}><Trash2 /></Button>
        </div>
      ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Class Groups"
        description="Batches/classes used for attendance filters, schedule slots and notification recipients."
        actions={<Button onClick={() => open('new')} data-testid="add-group"><Plus /> Add group</Button>}
      />
      {q.isError ? <ErrorState error={q.error} onRetry={() => q.refetch()} /> : (
        <DataTable columns={columns} data={q.data ?? []} loading={q.isLoading} rowTestId={(g) => `group-row-${g.name}`} emptyTitle="No class groups yet" />
      )}
      <Dialog open={editing !== null} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing === 'new' ? 'Add class group' : 'Edit class group'}</DialogTitle>
          </DialogHeader>
          <form
            className="space-y-3"
            onSubmit={(e) => {
              e.preventDefault()
              if (!name.trim()) setError('Name is required')
              else save.mutate()
            }}
          >
            <FormField id="group-name" label="Name" required error={error ?? undefined}>
              <Input id="group-name" value={name} onChange={(e) => setName(e.target.value)} maxLength={100} data-testid="group-name" />
            </FormField>
            <FormField id="group-description" label="Description">
              <Textarea id="group-description" value={description} onChange={(e) => setDescription(e.target.value)} rows={3} />
            </FormField>
            <DialogFooter>
              <Button type="submit" disabled={save.isPending} data-testid="group-save">Save</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
      <ConfirmDialog
        open={!!deleting}
        onOpenChange={(o) => !o && setDeleting(null)}
        destructive
        title={`Delete ${deleting?.name}?`}
        description="Groups that still have students or schedule slots cannot be deleted."
        confirmLabel="Delete"
        pending={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting.id)}
      />
    </div>
  )
}
