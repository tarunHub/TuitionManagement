import { http } from './client'
import { compact } from '@/lib/utils'
import type { Feedback, FeedbackInput, FeedbackRow } from '@/types'

export const feedbackApi = {
  list: (month: string, groupId?: number | '') =>
    http.get<FeedbackRow[]>('/feedback', { params: compact({ month, groupId }) }).then((r) => r.data),
  upsert: (studentId: number, month: string, input: FeedbackInput) =>
    http.put<Feedback>(`/feedback/${studentId}`, input, { params: { month } }).then((r) => r.data),
  remove: (id: number) => http.delete<void>(`/feedback/${id}`),
  copyTopics: (month: string) =>
    http.post<{ copied: number }>('/feedback/copy-topics', null, { params: { month } }).then((r) => r.data),
  studentHistory: (studentId: number) => http.get<Feedback[]>(`/feedback/students/${studentId}`).then((r) => r.data),
}
