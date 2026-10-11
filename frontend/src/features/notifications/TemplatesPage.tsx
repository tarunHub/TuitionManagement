import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ColumnDef } from '@tanstack/react-table'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2, Pencil, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { notificationsApi, templatesApi } from '@/api/notificationsApi'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { DataTable } from '@/components/DataTable'
import { FormField } from '@/components/FormField'
import { PageHeader } from '@/components/PageHeader'
import { ErrorState } from '@/components/States'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'
import { applyServerErrors, errorMessage } from '@/lib/errors'
import { NOTIFICATION_TYPES, type Template } from '@/types'
import { typeLabel } from './NotificationsPage'

const schema = z.object({
  name: z.string().trim().min(1, 'Required').max(100),
  type: z.enum(['SCHEDULE', 'SCHEDULE_CHANGE', 'CLASS_CANCELLED', 'FEE_REMINDER', 'FEEDBACK', 'GENERAL']),
  subject: z.string().trim().min(1, 'Required').max(200),
  body: z.string().trim().min(1, 'Required').max(20000),
})
type Values = z.infer<typeof schema>

export function TemplatesPage() {
  const queryClient = useQueryClient()
  const q = useQuery({ queryKey: ['templates'], queryFn: templatesApi.list })
  const placeholders = useQuery({ queryKey: ['placeholders'], queryFn: notificationsApi.placeholders, staleTime: Infinity })
  const [editing, setEditing] = useState<Template | 'new' | null>(null)
  const [deleting, setDeleting] = useState<Template | null>(null)
  const { register, handleSubmit, reset, setError, formState } = useForm<Values>({ resolver: zodResolver(schema) })

  useEffect(() => {
    if (editing === 'new') reset({ name: '', type: 'GENERAL', subject: '', body: '' })
    else if (editing) reset({ name: editing.name, type: editing.type, subject: editing.subject, body: editing.body })
  }, [editing, reset])

  const save = useMutation({
    mutationFn: (v: Values) => (editing === 'new' ? templatesApi.create(v) : templatesApi.update((editing as Template).id, v)),
    onSuccess: () => {
      toast.success('Template saved')
      setEditing(null)
      queryClient.invalidateQueries({ queryKey: ['templates'] })
    },
    onError: (e) => {
      if (!applyServerErrors(e, setError, ['name', 'type', 'subject', 'body'])) toast.error(errorMessage(e))
    },
  })

  const remove = useMutation({
    mutationFn: (id: number) => templatesApi.remove(id),
    onSuccess: () => {
      toast.success('Template deleted')
      setDeleting(null)
      queryClient.invalidateQueries({ queryKey: ['templates'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const columns: ColumnDef<Template>[] = [
    { accessorKey: 'name', header: 'Name', enableHiding: false, cell: ({ getValue }) => <span className="font-medium">{getValue<string>()}</span> },
    { accessorKey: 'type', header: 'Type', cell: ({ row }) => typeLabel(row.original.type) },
    { accessorKey: 'subject', header: 'Subject' },
    {
      id: 'actions',
      header: '',
      enableSorting: false,
      enableHiding: false,
      meta: { className: 'text-right' },
      cell: ({ row }) => (
        <div className="flex justify-end gap-1">
          <Button size="icon" variant="ghost" aria-label={`Edit ${row.original.name}`} onClick={() => setEditing(row.original)}><Pencil /></Button>
          <Button size="icon" variant="ghost" aria-label={`Delete ${row.original.name}`} onClick={() => setDeleting(row.original)}><Trash2 /></Button>
        </div>
      ),
    },
  ]
  const e = formState.errors

  return (
    <div>
      <PageHeader
        title="Notification templates"
        description={<>Reusable messages. Automated reminders use the templates named “Class Reminder”, “Fee Reminder”, “Weekly Schedule” and “Monthly Feedback” when present. <Link to="/notifications" className="text-primary hover:underline">Back to notifications</Link></>}
        actions={<Button onClick={() => setEditing('new')} data-testid="add-template"><Plus /> New template</Button>}
      />
      {q.isError ? <ErrorState error={q.error} onRetry={() => q.refetch()} /> : (
        <DataTable columns={columns} data={q.data ?? []} loading={q.isLoading} rowTestId={(t) => `template-row-${t.id}`} onRowClick={setEditing} emptyTitle="No templates yet" />
      )}
      <Dialog open={editing !== null} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{editing === 'new' ? 'New template' : 'Edit template'}</DialogTitle>
            <DialogDescription>
              Placeholders: {placeholders.data?.map((p) => `{{${p.name}}}`).join(' ')}
            </DialogDescription>
          </DialogHeader>
          <form noValidate onSubmit={handleSubmit((v) => save.mutate(v))} className="space-y-3">
            <div className="grid gap-3 sm:grid-cols-2">
              <FormField id="t-name" label="Name" required error={e.name?.message}>
                <Input id="t-name" {...register('name')} data-testid="template-name" />
              </FormField>
              <FormField id="t-type" label="Type" required error={e.type?.message}>
                <NativeSelect id="t-type" {...register('type')}>
                  {NOTIFICATION_TYPES.map((t) => <option key={t} value={t}>{typeLabel(t)}</option>)}
                </NativeSelect>
              </FormField>
            </div>
            <FormField id="t-subject" label="Subject" required error={e.subject?.message}>
              <Input id="t-subject" {...register('subject')} data-testid="template-subject" />
            </FormField>
            <FormField id="t-body" label="Body" required error={e.body?.message}>
              <Textarea id="t-body" rows={12} className="font-mono text-xs" {...register('body')} data-testid="template-body" />
            </FormField>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setEditing(null)}>Cancel</Button>
              <Button type="submit" disabled={save.isPending} data-testid="template-save">{save.isPending && <Loader2 className="animate-spin" />} Save template</Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
      <ConfirmDialog
        open={!!deleting}
        onOpenChange={(o) => !o && setDeleting(null)}
        destructive
        title={`Delete "${deleting?.name}"?`}
        description="Existing notifications keep their text; automated reminders fall back to built-in wording if a named template is removed."
        confirmLabel="Delete"
        pending={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting.id)}
      />
    </div>
  )
}
