import { http } from './client'
import type { ChatAnswer, ChatMessage, ChatStatus, FeedbackDraft, FeedbackDraftInput } from '@/types'

export const chatApi = {
  status: () => http.get<ChatStatus>('/chat/status').then((r) => r.data),
  ask: (messages: ChatMessage[]) => http.post<ChatAnswer>('/chat', { messages }).then((r) => r.data),
  feedbackDraft: (input: FeedbackDraftInput) => http.post<FeedbackDraft>('/chat/feedback-draft', input).then((r) => r.data),
}
