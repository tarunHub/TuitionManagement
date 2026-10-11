import { Badge } from '@/components/ui/badge'
import type { FeeStatus, NotificationStatus, StudentStatus } from '@/types'

const FEE: Record<FeeStatus, { label: string; variant: 'green' | 'amber' | 'red' | 'blue' }> = {
  PAID: { label: 'Paid', variant: 'green' },
  PARTIAL: { label: 'Partial', variant: 'amber' },
  UNPAID: { label: 'Unpaid', variant: 'red' },
  CREDIT: { label: 'Credit', variant: 'blue' },
}

/** Status is always shown as text, never by colour alone. */
export function FeeStatusBadge({ status }: { status: FeeStatus }) {
  const s = FEE[status]
  return <Badge variant={s.variant} data-testid={`fee-status-${status}`}>{s.label}</Badge>
}

export function StudentStatusBadge({ status }: { status: StudentStatus }) {
  return <Badge variant={status === 'ACTIVE' ? 'green' : 'gray'}>{status === 'ACTIVE' ? 'Active' : 'Inactive'}</Badge>
}

const NOTIF: Record<NotificationStatus, 'gray' | 'blue' | 'green' | 'red'> = {
  DRAFT: 'gray',
  SCHEDULED: 'blue',
  SENT: 'green',
  FAILED: 'red',
}

export function NotificationStatusBadge({ status }: { status: NotificationStatus }) {
  return <Badge variant={NOTIF[status]} data-testid={`notification-status-${status}`}>{status.charAt(0) + status.slice(1).toLowerCase()}</Badge>
}
