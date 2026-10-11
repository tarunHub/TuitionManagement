import { http } from './client'
import type { GoogleStatus, Recipients, Slot, SlotInput, SyncResult, Term, TermInput } from '@/types'

export const scheduleApi = {
  activeTerm: () => http.get<Term | ''>('/terms/active').then((r) => (r.status === 204 || !r.data ? null : r.data)),
  saveActiveTerm: (input: TermInput) => http.put<Term>('/terms/active', input).then((r) => r.data),
  terms: () => http.get<Term[]>('/terms').then((r) => r.data),
  slots: (termId?: number) => http.get<Slot[]>('/schedule-slots', { params: termId ? { termId } : {} }).then((r) => r.data),
  createSlot: (input: SlotInput) => http.post<Slot>('/schedule-slots', input).then((r) => r.data),
  updateSlot: (id: number, input: SlotInput) => http.put<Slot>(`/schedule-slots/${id}`, input).then((r) => r.data),
  deleteSlot: (id: number) => http.delete<void>(`/schedule-slots/${id}`),
  recipients: (id: number) => http.get<Recipients>(`/schedule-slots/${id}/recipients`).then((r) => r.data),
  googleStatus: () => http.get<GoogleStatus>('/schedule/google-status').then((r) => r.data),
  syncGoogle: () => http.post<SyncResult>('/schedule/sync-google').then((r) => r.data),
}
