import { http } from './client'
import { compact } from '@/lib/utils'
import { downloadFile } from '@/lib/download'
import type { FeeList, FeeRow, FeeStatus, PaymentInput } from '@/types'

export interface FeeFilters {
  status?: FeeStatus | ''
  groupId?: number | ''
}

export const feesApi = {
  list: (month: string, filters: FeeFilters) =>
    http.get<FeeList>('/fees', { params: compact({ month, ...filters }) }).then((r) => r.data),
  generate: (month: string) =>
    http.post<{ month: string; created: number; updated: number; fees: FeeList }>('/fees/generate', null, { params: { month } })
      .then((r) => r.data),
  update: (id: number, input: { notes?: string | null; rate?: number | null }) =>
    http.put<FeeRow>(`/fees/${id}`, input).then((r) => r.data),
  addPayment: (feeId: number, input: PaymentInput) =>
    http.post<FeeRow>(`/fees/${feeId}/payments`, input).then((r) => r.data),
  updatePayment: (feeId: number, paymentId: number, input: PaymentInput) =>
    http.put<FeeRow>(`/fees/${feeId}/payments/${paymentId}`, input).then((r) => r.data),
  deletePayment: (feeId: number, paymentId: number) =>
    http.delete<FeeRow>(`/fees/${feeId}/payments/${paymentId}`).then((r) => r.data),
  exportXlsx: (month: string, filters: FeeFilters) =>
    downloadFile('/fees/export', compact({ month, ...filters }), `fees-${month}.xlsx`),
  studentHistory: (studentId: number) => http.get<FeeRow[]>(`/fees/students/${studentId}`).then((r) => r.data),
}
