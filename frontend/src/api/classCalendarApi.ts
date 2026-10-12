import { http } from './client'
import { compact } from '@/lib/utils'
import type { BulkSendResult, ClassChangeInput, ClassSession, ClassSessionInput, NotificationChannel, ReminderSettings, RepeatWeeklyInput } from '@/types'

export const classCalendarApi = {
  list: (month: string, groupId?: number | '') =>
    http.get<ClassSession[]>('/class-calendar', { params: compact({ month, groupId }) }).then((r) => r.data),
  create: (input: ClassSessionInput) => http.post<ClassSession>('/class-calendar', input).then((r) => r.data),
  update: (id: number, input: ClassSessionInput) => http.put<ClassSession>(`/class-calendar/${id}`, input).then((r) => r.data),
  remove: (id: number) => http.delete<void>(`/class-calendar/${id}`),
  repeatWeekly: (input: RepeatWeeklyInput) =>
    http.post<{ created: number; skipped: number }>('/class-calendar/repeat-weekly', input).then((r) => r.data),
  sendSchedule: (month: string, classGroupIds: number[], channels: NotificationChannel[]) =>
    http.post<BulkSendResult>('/class-calendar/send-schedule', { month, classGroupIds, channels }).then((r) => r.data),
  reminderSettings: () => http.get<ReminderSettings>('/class-calendar/reminder-settings').then((r) => r.data),
  updateReminderSettings: (input: Omit<ReminderSettings, 'lastRunDate' | 'smsAvailable'>) =>
    http.put<ReminderSettings>('/class-calendar/reminder-settings', input).then((r) => r.data),
  notifyChange: (input: ClassChangeInput) => http.post<BulkSendResult>('/class-calendar/notify-change', input).then((r) => r.data),
  remindToday: () => http.post<BulkSendResult>('/class-calendar/remind-today').then((r) => r.data),
  remind: (id: number) => http.post<BulkSendResult>(`/class-calendar/${id}/remind`).then((r) => r.data),
}
