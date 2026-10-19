import { useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ColumnDef, RowSelectionState } from '@tanstack/react-table'
import { Download, History, Loader2, Mail, Plus, RefreshCw } from 'lucide-react'
import { toast } from 'sonner'
import { feesApi } from '@/api/feesApi'
import { notificationsApi } from '@/api/notificationsApi'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { DataTable } from '@/components/DataTable'
import { PageHeader } from '@/components/PageHeader'
import { EmptyState, ErrorState } from '@/components/States'
import { FeeStatusBadge } from '@/components/StatusBadge'
import { Button } from '@/components/ui/button'
import { NativeSelect } from '@/components/ui/native-select'
import { useMonthParam } from '@/hooks/useMonthParam'
import { useClassGroups } from '@/hooks/useReferenceData'
import { formatDate, formatMonth, isPastMonth } from '@/lib/dates'
import { errorMessage } from '@/lib/errors'
import { formatMoney } from '@/lib/money'
import type { FeeRow, FeeStatus, Payment, PaymentInput } from '@/types'
import { FeeDetailsDialog } from './FeeDetailsDialog'
import { methodLabel, PaymentDialog } from './PaymentDialog'

const money = (v: number) => <span className="tabular-nums">{formatMoney(v)}</span>
const right = { className: 'text-right whitespace-nowrap' }
const nowrap = { className: 'whitespace-nowrap' }

export function FeesPage() {
  const [month] = useMonthParam()
  const locked = isPastMonth(month)
  const [status, setStatus] = useState<FeeStatus | ''>('')
  const [groupId, setGroupId] = useState<number | ''>('')
  const [selection, setSelection] = useState<RowSelectionState>({})
  const [paymentFee, setPaymentFee] = useState<FeeRow | null>(null)
  const [editingPayment, setEditingPayment] = useState<Payment | null>(null)
  const [detailsId, setDetailsId] = useState<number | null>(null)
  const [deletingPayment, setDeletingPayment] = useState<Payment | null>(null)
  const groups = useClassGroups()
  const queryClient = useQueryClient()

  const q = useQuery({ queryKey: ['fees', month, status, groupId], queryFn: () => feesApi.list(month, { status, groupId }) })
  const rows = useMemo(() => q.data?.rows ?? [], [q.data])
  const detailsFee = rows.find((r) => r.id === detailsId) ?? null

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['fees'] })
    queryClient.invalidateQueries({ queryKey: ['fees-history'] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const generate = useMutation({
    mutationFn: () => feesApi.generate(month),
    onSuccess: (r) => {
      toast.success(`Fees for ${formatMonth(month)}: ${r.created} created, ${r.updated} refreshed`)
      invalidate()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const savePayment = useMutation({
    mutationFn: (input: PaymentInput) =>
      editingPayment ? feesApi.updatePayment(paymentFee!.id, editingPayment.id, input) : feesApi.addPayment(paymentFee!.id, input),
    onSuccess: (row) => {
      toast.success(`${editingPayment ? 'Payment updated' : 'Payment recorded'} — ${row.fullName} is now ${row.status.toLowerCase()}`)
      setPaymentFee(null)
      setEditingPayment(null)
      invalidate()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const deletePayment = useMutation({
    mutationFn: (p: Payment) => feesApi.deletePayment(p.monthlyFeeId, p.id),
    onSuccess: () => {
      toast.success('Payment deleted')
      setDeletingPayment(null)
      invalidate()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const saveFee = useMutation({
    mutationFn: (input: { notes: string | null; rate: number }) => feesApi.update(detailsId!, input),
    onSuccess: () => {
      toast.success('Fee updated')
      invalidate()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const selected = rows.filter((r) => selection[String(r.id)])
  const remind = useMutation({
    mutationFn: () => notificationsApi.feeReminders(selected.map((r) => r.id)),
    onSuccess: (r) => {
      // Opted-in parents also get an SMS, so report emails and texts separately.
      const sent = (channel: 'EMAIL' | 'SMS') => r.notifications.filter((n) => n.channel === channel && n.status === 'SENT').length
      const texts = sent('SMS') ? ` and ${sent('SMS')} SMS` : ''
      if (r.failed) toast.warning(`${sent('EMAIL')} email(s)${texts} sent, ${r.failed} failed — see Notifications`)
      else toast.success(`${sent('EMAIL')} fee reminder email(s)${texts} sent`)
      setSelection({})
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const exportXlsx = async () => {
    try {
      await feesApi.exportXlsx(month, { status, groupId })
    } catch (e) {
      toast.error(errorMessage(e))
    }
  }

  const columns: ColumnDef<FeeRow>[] = [
    {
      id: 'select',
      enableSorting: false,
      enableHiding: false,
      header: ({ table }) => (
        <input
          type="checkbox"
          aria-label="Select all unpaid or partial"
          checked={table.getIsAllRowsSelected() && rows.some((r) => r.status === 'UNPAID' || r.status === 'PARTIAL')}
          onChange={table.getToggleAllRowsSelectedHandler()}
        />
      ),
      cell: ({ row }) => (
        <input
          type="checkbox"
          aria-label={`Select ${row.original.fullName}`}
          checked={row.getIsSelected()}
          disabled={!row.getCanSelect()}
          onChange={row.getToggleSelectedHandler()}
          onClick={(e) => e.stopPropagation()}
          data-testid={`select-fee-${row.original.studentCode}`}
        />
      ),
    },
    { accessorKey: 'studentCode', header: 'Student ID', enableHiding: false },
    { accessorKey: 'fullName', header: 'Name', enableHiding: false, meta: nowrap, cell: ({ getValue }) => <span className="font-medium">{getValue<string>()}</span> },
    { accessorKey: 'grade', header: 'Grade' },
    { accessorKey: 'rate', header: 'Rate ($/class)', cell: ({ getValue }) => money(getValue<number>()), meta: right },
    { accessorKey: 'classesHeld', header: 'Classes Held', meta: right },
    { accessorKey: 'classesScheduled', header: 'Classes Scheduled (billed)', cell: ({ getValue }) => getValue<number | null>() ?? '—', meta: right },
    { accessorKey: 'amountDue', header: 'Amount Due', cell: ({ getValue }) => money(getValue<number>()), meta: right },
    { accessorKey: 'amountPaid', header: 'Amount Paid', cell: ({ getValue }) => money(getValue<number>()), meta: right },
    {
      accessorKey: 'balance',
      header: 'Balance',
      meta: right,
      cell: ({ row }) => <span data-testid={`balance-${row.original.studentCode}`}>{money(row.original.balance)}</span>,
    },
    { accessorKey: 'status', header: 'Status', cell: ({ row }) => <span data-testid={`status-${row.original.studentCode}`}><FeeStatusBadge status={row.original.status} /></span> },
    { accessorKey: 'lastPaymentDate', header: 'Last Payment Date', meta: nowrap, cell: ({ getValue }) => formatDate(getValue<string | null>()) },
    { accessorKey: 'lastPaymentMethod', header: 'Method', meta: nowrap, cell: ({ row }) => methodLabel(row.original.lastPaymentMethod) },
    { accessorKey: 'notes', header: 'Notes', cell: ({ getValue }) => getValue<string | null>() || '—' },
    {
      id: 'actions',
      header: '',
      enableHiding: false,
      enableSorting: false,
      meta: right,
      cell: ({ row }) => (
        <div className="flex justify-end gap-1">
          <Button size="sm" variant="outline" onClick={() => { setEditingPayment(null); setPaymentFee(row.original) }} data-testid={`record-payment-${row.original.studentCode}`}>
            <Plus /> Payment
          </Button>
          <Button size="icon" variant="ghost" aria-label={`Payments and details for ${row.original.fullName}`} onClick={() => setDetailsId(row.original.id)} data-testid={`fee-details-${row.original.studentCode}`}>
            <History />
          </Button>
        </div>
      ),
    },
  ]

  const totals = q.data?.totals

  return (
    <div>
      <PageHeader
        title="Monthly Fees"
        description={`${formatMonth(month)} — amount due = classes scheduled on the Class Calendar × rate.`}
        actions={
          <>
            <Button variant="outline" onClick={exportXlsx} data-testid="export-fees"><Download /> Export Excel</Button>
            <Button variant="outline" disabled={selected.length === 0 || remind.isPending} onClick={() => remind.mutate()} data-testid="send-reminders">
              {remind.isPending ? <Loader2 className="animate-spin" /> : <Mail />} Send fee reminder{selected.length ? ` (${selected.length})` : ''}
            </Button>
            {!locked && (
              <Button onClick={() => generate.mutate()} disabled={generate.isPending} data-testid="generate-fees">
                {generate.isPending ? <Loader2 className="animate-spin" /> : <RefreshCw />} Generate / Refresh fees
              </Button>
            )}
          </>
        }
      />
      {locked && (
        <p className="mb-3 rounded-md border bg-muted px-3 py-2 text-sm text-muted-foreground" role="status" data-testid="month-locked">
          {formatMonth(month)} is a past month, so its fees are final. You can still record payments.
        </p>
      )}
      {!locked && q.data && q.data.activeStudentsWithoutFee > 0 && q.data.rows.length > 0 && (
        <p className="mb-3 rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-900" role="status">
          {q.data.activeStudentsWithoutFee} active student(s) have no fee for this month yet. Use “Generate / Refresh fees”.
        </p>
      )}
      {q.isError ? (
        <ErrorState error={q.error} onRetry={() => q.refetch()} />
      ) : !q.isLoading && rows.length === 0 && !status && !groupId ? (
        <EmptyState
          title={`No fees for ${formatMonth(month)}`}
          description={locked
            ? 'No fees were generated for this month.'
            : "Generate fees to create one row per active student from this month's attendance. Re-running is safe and never removes payments."}
          action={locked ? undefined : <Button onClick={() => generate.mutate()} disabled={generate.isPending}><RefreshCw /> Generate fees</Button>}
        />
      ) : (
        <DataTable
          testId="fees-table"
          columns={columns}
          data={rows}
          loading={q.isLoading}
          paginate={false}
          searchPlaceholder="Search student"
          getRowId={(r) => String(r.id)}
          rowTestId={(r) => `fee-row-${r.studentCode}`}
          rowSelection={selection}
          onRowSelectionChange={setSelection}
          enableRowSelection={(row) => row.original.status === 'UNPAID' || row.original.status === 'PARTIAL'}
          initialVisibility={{ notes: false }}
          emptyTitle="No fees match the filters"
          toolbar={
            <>
              <NativeSelect aria-label="Filter by status" className="w-36" value={status} onChange={(e) => setStatus(e.target.value as FeeStatus | '')} data-testid="fees-filter-status">
                <option value="">All statuses</option>
                <option value="UNPAID">Unpaid</option>
                <option value="PARTIAL">Partial</option>
                <option value="PAID">Paid</option>
                <option value="CREDIT">Credit</option>
              </NativeSelect>
              <NativeSelect aria-label="Filter by group" className="w-40" value={groupId} onChange={(e) => setGroupId(e.target.value ? Number(e.target.value) : '')} data-testid="fees-filter-group">
                <option value="">All groups</option>
                {groups.data?.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
              </NativeSelect>
            </>
          }
          footerRow={
            totals && {
              studentCode: <span data-testid="fees-total-row">TOTAL ({totals.count})</span>,
              amountDue: <span data-testid="total-due">{money(totals.amountDue)}</span>,
              amountPaid: <span data-testid="total-paid">{money(totals.amountPaid)}</span>,
              balance: <span data-testid="total-balance">{money(totals.balance)}</span>,
            }
          }
        />
      )}

      <PaymentDialog
        open={!!paymentFee}
        fee={paymentFee}
        payment={editingPayment}
        pending={savePayment.isPending}
        onOpenChange={(o) => { if (!o) { setPaymentFee(null); setEditingPayment(null) } }}
        onSubmit={(input) => savePayment.mutate(input)}
      />
      <FeeDetailsDialog
        open={detailsId !== null}
        fee={detailsFee}
        onOpenChange={(o) => !o && setDetailsId(null)}
        onEditPayment={(p) => { setEditingPayment(p); setPaymentFee(detailsFee) }}
        onDeletePayment={setDeletingPayment}
        onSaveFee={(input) => saveFee.mutate(input)}
        savingFee={saveFee.isPending}
      />
      <ConfirmDialog
        open={!!deletingPayment}
        onOpenChange={(o) => !o && setDeletingPayment(null)}
        destructive
        title="Delete payment?"
        description={deletingPayment ? `${formatMoney(deletingPayment.amount)} paid on ${formatDate(deletingPayment.paymentDate)} will be removed.` : ''}
        confirmLabel="Delete"
        pending={deletePayment.isPending}
        onConfirm={() => deletingPayment && deletePayment.mutate(deletingPayment)}
      />
    </div>
  )
}
