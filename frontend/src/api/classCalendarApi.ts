import { http } from './client'
import { compact } from '@/lib/utils'
import type { BulkSendResult, ClassSession, ClassSessionInput, RepeatWeeklyInput } from '@/types'

export const classCalendarApi = {
  list: (month: string, groupId?: number | '') =>
    http.get<ClassSession[]>('/class-calendar', { params: compact({ month, groupId }) }).then((r) => r.data),
  create: (input: ClassSessionInput) => http.post<ClassSession>('/class-calendar', input).then((r) => r.data),
  update: (id: number, input: ClassSessionInput) => http.put<ClassSession>(`/class-calendar/${id}`, input).then((r) => r.data),
  remove: (id: number) => http.delete<void>(`/class-calendar/${id}`),
  repeatWeekly: (input: RepeatWeeklyInput) =>
    http.post<{ created: number; skipped: number }>('/class-calendar/repeat-weekly', input).then((r) => r.data),
  email: (month: string, classGroupIds: number[]) =>
    http.post<BulkSendResult>('/class-calendar/email', { month, classGroupIds }).then((r) => r.data),
}
