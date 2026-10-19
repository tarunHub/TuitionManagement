import { useEffect, useState } from 'react'
import { Pencil, Trash2 } from 'lucide-react'
import { FeeStatusBadge } from '@/components/StatusBadge'
import { FormField } from '@/components/FormField'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { formatDate, formatMonth, isPastMonth } from '@/lib/dates'
import { formatMoney } from '@/lib/money'
import type { FeeRow, Payment } from '@/types'
import { methodLabel } from './PaymentDialog'

interface Props {
  fee: FeeRow | null
  open: boolean
  onOpenChange: (open: boolean) => void
  onEditPayment: (payment: Payment) => void
  onDeletePayment: (payment: Payment) => void
  onSaveFee: (input: { notes: string | null; rate: number }) => void
  savingFee: boolean
}

/** Payment history for one fee row, plus editing of notes / rate snapshot. */
export function FeeDetailsDialog({ fee, open, onOpenChange, onEditPayment, onDeletePayment, onSaveFee, savingFee }: Props) {
  const [notes, setNotes] = useState('')
  const [rate, setRate] = useState('')

  useEffect(() => {
    if (fee && open) {
      setNotes(fee.notes ?? '')
      setRate(String(fee.rate))
    }
  }, [fee, open])

  if (!fee) return null
  const locked = isPastMonth(fee.month)
  const rateValid = rate !== '' && Number(rate) >= 0

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl" data-testid="fee-details-dialog">
        <DialogHeader>
          <DialogTitle>{fee.studentCode} {fee.fullName} — {formatMonth(fee.month)}</DialogTitle>
          <DialogDescription className="flex flex-wrap items-center gap-2">
            Due {formatMoney(fee.amountDue)} · Paid {formatMoney(fee.amountPaid)} · Balance {formatMoney(fee.balance)} <FeeStatusBadge status={fee.status} />
          </DialogDescription>
        </DialogHeader>

        <section>
          <h3 className="mb-2 text-sm font-semibold">Payments</h3>
          {fee.payments.length === 0 ? (
            <p className="text-sm text-muted-foreground">No payments recorded.</p>
          ) : (
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow><TableHead>Date</TableHead><TableHead className="text-right">Amount</TableHead><TableHead>Method</TableHead><TableHead>Reference</TableHead><TableHead /></TableRow>
                </TableHeader>
                <TableBody>
                  {fee.payments.map((p) => (
                    <TableRow key={p.id} data-testid="payment-row">
                      <TableCell>{formatDate(p.paymentDate)}</TableCell>
                      <TableCell className="text-right">{formatMoney(p.amount)}</TableCell>
                      <TableCell>{methodLabel(p.method)}</TableCell>
                      <TableCell>{p.reference || '—'}</TableCell>
                      <TableCell className="text-right">
                        <Button size="icon" variant="ghost" aria-label="Edit payment" onClick={() => onEditPayment(p)}><Pencil /></Button>
                        <Button size="icon" variant="ghost" aria-label="Delete payment" onClick={() => onDeletePayment(p)}><Trash2 /></Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </section>

        <section className="space-y-3 border-t pt-4">
          <h3 className="text-sm font-semibold">Fee details</h3>
          <p className="text-xs text-muted-foreground">
            Amount due = {fee.classesScheduled ?? 0} classes scheduled on the Class Calendar × rate. Classes held ({fee.classesHeld}) come from attendance. The rate is a snapshot taken when the fee was generated.
            {locked && ' This is a past month, so the rate and notes can no longer be changed.'}
          </p>
          <div className="grid gap-3 sm:grid-cols-2">
            <FormField id="fee-rate" label="Rate ($/class)" error={rateValid ? undefined : 'Must be ≥ 0'}>
              <Input id="fee-rate" type="number" min="0" step="0.01" value={rate} disabled={locked} onChange={(e) => setRate(e.target.value)} />
            </FormField>
            <FormField id="fee-notes" label="Notes">
              <Input id="fee-notes" value={notes} disabled={locked} onChange={(e) => setNotes(e.target.value)} />
            </FormField>
          </div>
          {!locked && <div className="flex justify-end">
            <Button
              disabled={savingFee || !rateValid}
              onClick={() => onSaveFee({ notes: notes.trim() || null, rate: Number(rate) })}
            >
              Save fee details
            </Button>
          </div>}
        </section>
      </DialogContent>
    </Dialog>
  )
}
