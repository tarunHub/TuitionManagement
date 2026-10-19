import { http } from './client'
import { compact } from '@/lib/utils'
import type { BulkSendResult, ClassChangeInput, ClassSession, ClassSessionInput, MessagePreview, MessageToSend, NotificationChannel, ReminderSettings, RepeatWeeklyInput } from '@/types'

export const classCalendarApi = {
  list: (month: string, groupId?: number | '') =>
    http.get<ClassSession[]>('/class-calendar', { params: compact({ month, groupId }) }).then((r) => r.data),
  create: (input: ClassSessionInput) => http.post<ClassSession>('/class-calendar', input).then((r) => r.data),
  update: (id: number, input: ClassSessionInput) => http.put<ClassSession>(`/class-calendar/${id}`, input).then((r) => r.data),
  remove: (id: number) => http.delete<void>(`/class-calendar/${id}`),
  repeatWeekly: (input: RepeatWeeklyInput) =>
    http.post<{ created: number; skipped: number }>('/class-calendar/repeat-weekly', input).then((r) => r.data),
  reminderSettings: () => http.get<ReminderSettings>('/class-calendar/reminder-settings').then((r) => r.data),
  updateReminderSettings: (input: Omit<ReminderSettings, 'lastRunDate' | 'smsAvailable'>) =>
    http.put<ReminderSettings>('/class-calendar/reminder-settings', input).then((r) => r.data),
  // Previews build the exact messages without sending; sendMessages sends what the admin kept after review.
  previewSchedule: (month: string, classGroupIds: number[], channels: NotificationChannel[]) =>
    http.post<MessagePreview>('/class-calendar/send-schedule/preview', { month, classGroupIds, channels }).then((r) => r.data),
  previewChange: (input: ClassChangeInput) => http.post<MessagePreview>('/class-calendar/notify-change/preview', input).then((r) => r.data),
  previewRemindToday: () => http.post<MessagePreview>('/class-calendar/remind-today/preview').then((r) => r.data),
  previewRemind: (id: number) => http.post<MessagePreview>(`/class-calendar/${id}/remind/preview`).then((r) => r.data),
  sendMessages: (messages: MessageToSend[]) =>
    http.post<BulkSendResult>('/class-calendar/send-messages', { messages }).then((r) => r.data),
}
