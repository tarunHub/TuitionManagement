import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Loader2 } from 'lucide-react'
import { FormField } from '@/components/FormField'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { todayIso } from '@/lib/dates'
import { formatMoney } from '@/lib/money'
import type { FeeRow, Payment, PaymentInput, PaymentMethod } from '@/types'

export const METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'CASH', label: 'Cash' },
  { value: 'E_TRANSFER', label: 'e-Transfer' },
  { value: 'CHEQUE', label: 'Cheque' },
  { value: 'CARD', label: 'Card' },
  { value: 'OTHER', label: 'Other' },
]

export const methodLabel = (m: PaymentMethod | null) => METHODS.find((x) => x.value === m)?.label ?? '—'

const schema = z.object({
  amount: z.coerce.number({ invalid_type_error: 'Enter an amount' }).gt(0, 'Must be greater than 0'),
  paymentDate: z.string().min(1, 'Required').refine((v) => v <= todayIso(), 'Cannot be in the future'),
  method: z.enum(['CASH', 'E_TRANSFER', 'CHEQUE', 'CARD', 'OTHER']),
  reference: z.string().max(100).optional(),
})

type Values = z.infer<typeof schema>

interface Props {
  fee: FeeRow | null
  payment?: Payment | null
  open: boolean
  pending: boolean
  onOpenChange: (open: boolean) => void
  onSubmit: (input: PaymentInput) => void
}

export function PaymentDialog({ fee, payment, open, pending, onOpenChange, onSubmit }: Props) {
  const { register, handleSubmit, reset, formState } = useForm<Values>({ resolver: zodResolver(schema) })

  useEffect(() => {
    if (!open || !fee) return
    reset(
      payment
        ? { amount: payment.amount, paymentDate: payment.paymentDate, method: payment.method, reference: payment.reference ?? '' }
        : { amount: fee.balance > 0 ? fee.balance : undefined, paymentDate: todayIso(), method: 'E_TRANSFER', reference: '' },
    )
  }, [open, fee, payment, reset])

  const e = formState.errors
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent data-testid="payment-dialog">
        <DialogHeader>
          <DialogTitle>{payment ? 'Edit payment' : 'Record payment'}</DialogTitle>
          {fee && (
            <DialogDescription>
              {fee.studentCode} {fee.fullName} — due {formatMoney(fee.amountDue)}, paid {formatMoney(fee.amountPaid)}, balance {formatMoney(fee.balance)}
            </DialogDescription>
          )}
        </DialogHeader>
        <form
          noValidate
          className="space-y-3"
          onSubmit={handleSubmit((v) => onSubmit({ amount: v.amount, paymentDate: v.paymentDate, method: v.method, reference: v.reference?.trim() || null }))}
        >
          <div className="grid gap-3 sm:grid-cols-2">
            <FormField id="pay-amount" label="Amount ($)" required error={e.amount?.message}>
              <Input id="pay-amount" type="number" step="0.01" min="0.01" {...register('amount')} data-testid="payment-amount" autoFocus />
            </FormField>
            <FormField id="pay-date" label="Payment date" required error={e.paymentDate?.message}>
              <Input id="pay-date" type="date" max={todayIso()} {...register('paymentDate')} data-testid="payment-date" />
            </FormField>
            <FormField id="pay-method" label="Method" required error={e.method?.message}>
              <NativeSelect id="pay-method" {...register('method')} data-testid="payment-method">
                {METHODS.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}
              </NativeSelect>
            </FormField>
            <FormField id="pay-reference" label="Reference" error={e.reference?.message}>
              <Input id="pay-reference" {...register('reference')} data-testid="payment-reference" />
            </FormField>
          </div>
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" disabled={pending} data-testid="payment-submit">
              {pending && <Loader2 className="animate-spin" />}
              {payment ? 'Save payment' : 'Record payment'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
