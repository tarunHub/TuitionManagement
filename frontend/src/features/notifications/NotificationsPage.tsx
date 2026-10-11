import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ColumnDef, PaginationState } from '@tanstack/react-table'
import { FileText, Loader2, Pencil, PenSquare, RotateCw, Send, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { notificationsApi } from '@/api/notificationsApi'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { DataTable } from '@/components/DataTable'
import { PageHeader } from '@/components/PageHeader'
import { ErrorState } from '@/components/States'
import { NotificationStatusBadge } from '@/components/StatusBadge'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { NativeSelect } from '@/components/ui/native-select'
import { formatDateTime } from '@/lib/dates'
import { errorMessage } from '@/lib/errors'
import { NOTIFICATION_TYPES, type Notification, type NotificationChannel, type NotificationStatus, type NotificationType } from '@/types'

export const typeLabel = (t: NotificationType) => t.charAt(0) + t.slice(1).toLowerCase().replace(/_/g, ' ')

export function NotificationsPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [status, setStatus] = useState<NotificationStatus | ''>('')
  const [type, setType] = useState<NotificationType | ''>('')
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 20 })
  const [selectedId, setSelectedId] = useState<number | null>(null)
  const [deleting, setDeleting] = useState<Notification | null>(null)

  const q = useQuery({
    queryKey: ['notifications', status, type, pagination],
    queryFn: () => notificationsApi.list({ status, type, page: pagination.pageIndex, size: pagination.pageSize }),
    placeholderData: keepPreviousData,
  })
  const selected = q.data?.content.find((n) => n.id === selectedId) ?? null

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ['notifications'] })

  const act = useMutation({
    mutationFn: ({ id, action }: { id: number; action: 'send' | 'retry' }) => (action === 'send' ? notificationsApi.send(id) : notificationsApi.retry(id)),
    onSuccess: (n) => {
      if (n.status === 'SENT') toast.success('Notification sent')
      else toast.error(`Sending failed: ${n.errorMessage}`)
      invalidate()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const remove = useMutation({
    mutationFn: (id: number) => notificationsApi.remove(id),
    onSuccess: () => {
      toast.success('Notification deleted')
      setDeleting(null)
      setSelectedId(null)
      invalidate()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const columns: ColumnDef<Notification>[] = [
    { accessorKey: 'type', header: 'Type', enableSorting: false, cell: ({ getValue }) => typeLabel(getValue<NotificationType>()) },
    { accessorKey: 'channel', header: 'Channel', enableSorting: false, cell: ({ getValue }) => <Badge variant="outline">{getValue<NotificationChannel>() === 'SMS' ? 'SMS' : 'Email'}</Badge> },
    { accessorKey: 'subject', header: 'Subject', enableSorting: false, enableHiding: false, cell: ({ getValue }) => <span className="font-medium">{getValue<string>()}</span> },
    { accessorKey: 'recipientCount', header: 'Recipients', enableSorting: false },
    { accessorKey: 'status', header: 'Status', enableSorting: false, cell: ({ getValue }) => <NotificationStatusBadge status={getValue<NotificationStatus>()} /> },
    {
      id: 'when',
      header: 'Sent / Scheduled',
      enableSorting: false,
      cell: ({ row }) => {
        const n = row.original
        if (n.sentAt) return formatDateTime(n.sentAt)
        if (n.scheduledAt) return <span>Scheduled {formatDateTime(n.scheduledAt)}</span>
        return <span className="text-muted-foreground">Created {formatDateTime(n.createdAt)}</span>
      },
    },
    {
      id: 'actions',
      header: '',
      enableHiding: false,
      enableSorting: false,
      meta: { className: 'text-right' },
      cell: ({ row }) =>
        row.original.status === 'FAILED' ? (
          <Button size="sm" variant="outline" onClick={(e) => { e.stopPropagation(); act.mutate({ id: row.original.id, action: 'retry' }) }} disabled={act.isPending} data-testid={`retry-${row.original.id}`}>
            <RotateCw /> Retry
          </Button>
        ) : null,
    },
  ]

  return (
    <div>
      <PageHeader
        title="Notifications"
        description="Parent emails and SMS: compose, schedule, and track delivery."
        actions={
          <>
            <Button variant="outline" asChild><Link to="/notifications/templates"><FileText /> Templates</Link></Button>
            <Button asChild data-testid="compose-notification"><Link to="/notifications/compose"><PenSquare /> Compose</Link></Button>
          </>
        }
      />
      {q.isError ? <ErrorState error={q.error} onRetry={() => q.refetch()} /> : (
        <DataTable
          testId="notifications-table"
          columns={columns}
          data={q.data?.content ?? []}
          loading={q.isLoading}
          searchable={false}
          rowTestId={(n) => `notification-row-${n.id}`}
          onRowClick={(n) => setSelectedId(n.id)}
          emptyTitle="No notifications yet"
          server={{
            pageCount: q.data?.totalPages ?? 0,
            totalElements: q.data?.totalElements ?? 0,
            pagination,
            onPaginationChange: setPagination,
            sorting: [],
            onSortingChange: () => undefined,
          }}
          toolbar={
            <>
              <NativeSelect aria-label="Filter by status" className="w-36" value={status} onChange={(e) => { setStatus(e.target.value as NotificationStatus | ''); setPagination((p) => ({ ...p, pageIndex: 0 })) }}>
                <option value="">All statuses</option>
                <option value="DRAFT">Draft</option>
                <option value="SCHEDULED">Scheduled</option>
                <option value="SENT">Sent</option>
                <option value="FAILED">Failed</option>
              </NativeSelect>
              <NativeSelect aria-label="Filter by type" className="w-44" value={type} onChange={(e) => { setType(e.target.value as NotificationType | ''); setPagination((p) => ({ ...p, pageIndex: 0 })) }}>
                <option value="">All types</option>
                {NOTIFICATION_TYPES.map((t) => <option key={t} value={t}>{typeLabel(t)}</option>)}
              </NativeSelect>
            </>
          }
        />
      )}

      <Dialog open={!!selected} onOpenChange={(o) => !o && setSelectedId(null)}>
        <DialogContent className="max-w-2xl" data-testid="notification-detail">
          {selected && (
            <>
              <DialogHeader>
                <DialogTitle className="pr-6">{selected.subject}</DialogTitle>
                <DialogDescription className="flex flex-wrap items-center gap-2">
                  {typeLabel(selected.type)} · {selected.channel === 'SMS' ? 'SMS' : 'Email'} · <NotificationStatusBadge status={selected.status} />
                  {selected.sentAt && <span>sent {formatDateTime(selected.sentAt)}</span>}
                  {selected.scheduledAt && !selected.sentAt && <span>scheduled for {formatDateTime(selected.scheduledAt)}</span>}
                </DialogDescription>
              </DialogHeader>
              {selected.errorMessage && (
                <p role="alert" className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive" data-testid="notification-error">
                  {selected.errorMessage}
                </p>
              )}
              <div>
                <p className="mb-1 text-xs font-semibold uppercase text-muted-foreground">Recipients ({selected.recipientCount})</p>
                <p className="text-sm">{selected.recipients.join(', ') || '—'}</p>
              </div>
              <pre className="max-h-80 overflow-auto whitespace-pre-wrap rounded-md border bg-muted/40 p-3 font-mono text-xs">{selected.body}</pre>
              <DialogFooter>
                {(selected.status === 'DRAFT' || selected.status === 'SCHEDULED') && (
                  <>
                    <Button variant="destructive" onClick={() => setDeleting(selected)}><Trash2 /> Delete</Button>
                    <Button variant="outline" onClick={() => navigate(`/notifications/${selected.id}/edit`)}><Pencil /> Edit</Button>
                    <Button onClick={() => act.mutate({ id: selected.id, action: 'send' })} disabled={act.isPending}>
                      {act.isPending ? <Loader2 className="animate-spin" /> : <Send />} Send now
                    </Button>
                  </>
                )}
                {selected.status === 'FAILED' && (
                  <Button onClick={() => act.mutate({ id: selected.id, action: 'retry' })} disabled={act.isPending} data-testid="retry-detail">
                    {act.isPending ? <Loader2 className="animate-spin" /> : <RotateCw />} Retry
                  </Button>
                )}
              </DialogFooter>
            </>
          )}
        </DialogContent>
      </Dialog>
      <ConfirmDialog
        open={!!deleting}
        onOpenChange={(o) => !o && setDeleting(null)}
        destructive
        title="Delete notification?"
        description={`"${deleting?.subject}" will be deleted and not sent.`}
        confirmLabel="Delete"
        pending={remove.isPending}
        onConfirm={() => deleting && remove.mutate(deleting.id)}
      />
    </div>
  )
}
