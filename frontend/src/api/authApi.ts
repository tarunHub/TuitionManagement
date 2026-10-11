import { http } from './client'
import type { AdminUser, LoginResponse } from '@/types'

export const authApi = {
  login: (username: string, password: string) =>
    http.post<LoginResponse>('/auth/login', { username, password }).then((r) => r.data),
  me: () => http.get<AdminUser>('/auth/me').then((r) => r.data),
  changePassword: (currentPassword: string, newPassword: string) =>
    http.post<void>('/auth/change-password', { currentPassword, newPassword }),
}
