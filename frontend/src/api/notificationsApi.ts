import { http } from './client'
import { compact } from '@/lib/utils'
import type {
  BulkSendResult,
  ChannelStatus,
  NotificationChannel,
  ComposeInput,
  Notification,
  NotificationStatus,
  NotificationType,
  Page,
  Placeholder,
  PreviewResult,
  RecipientSelection,
  Template,
  TemplateInput,
} from '@/types'

export const notificationsApi = {
  list: (params: { page?: number; size?: number; status?: NotificationStatus | ''; type?: NotificationType | '' }) =>
    http.get<Page<Notification>>('/notifications', { params: compact({ ...params }) }).then((r) => r.data),
  get: (id: number) => http.get<Notification>(`/notifications/${id}`).then((r) => r.data),
  create: (input: ComposeInput) => http.post<Notification>('/notifications', input).then((r) => r.data),
  update: (id: number, input: ComposeInput) => http.put<Notification>(`/notifications/${id}`, input).then((r) => r.data),
  preview: (input: { templateId?: number | null; subject?: string; body?: string; channel?: NotificationChannel; recipients?: RecipientSelection; month?: string | null }) =>
    http.post<PreviewResult>('/notifications/preview', input).then((r) => r.data),
  channels: () => http.get<ChannelStatus>('/notifications/channels').then((r) => r.data),
  placeholders: () => http.get<Placeholder[]>('/notifications/placeholders').then((r) => r.data),
  send: (id: number) => http.post<Notification>(`/notifications/${id}/send`).then((r) => r.data),
  retry: (id: number) => http.post<Notification>(`/notifications/${id}/retry`).then((r) => r.data),
  remove: (id: number) => http.delete<void>(`/notifications/${id}`),
  feeReminders: (feeIds: number[]) =>
    http.post<BulkSendResult>('/notifications/fee-reminders', { feeIds }).then((r) => r.data),
  feedbackEmail: (studentId: number, month: string) =>
    http.post<Notification>('/notifications/feedback-email', { studentId, month }).then((r) => r.data),
  scheduleBroadcast: (templateId?: number | null) =>
    http.post<BulkSendResult>('/notifications/schedule-broadcast', { templateId: templateId ?? null }).then((r) => r.data),
}

export const templatesApi = {
  list: () => http.get<Template[]>('/notification-templates').then((r) => r.data),
  create: (input: TemplateInput) => http.post<Template>('/notification-templates', input).then((r) => r.data),
  update: (id: number, input: TemplateInput) => http.put<Template>(`/notification-templates/${id}`, input).then((r) => r.data),
  remove: (id: number) => http.delete<void>(`/notification-templates/${id}`),
}
