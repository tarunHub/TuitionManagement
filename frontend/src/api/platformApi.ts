import { http } from './client'
import type { Centre, CentreInput, MessagingSettings, MessagingSettingsInput, PortalUserRow, UserInput } from '@/types'

/** Platform admin only: centres and every login. */
export const platformApi = {
  centres: () => http.get<Centre[]>('/platform/centres').then((r) => r.data),
  centre: (id: number) => http.get<Centre>(`/platform/centres/${id}`).then((r) => r.data),
  createCentre: (centre: CentreInput, firstUser: UserInput | null) =>
    http.post<Centre>('/platform/centres', { centre, firstUser }).then((r) => r.data),
  updateCentre: (id: number, centre: CentreInput) => http.put<Centre>(`/platform/centres/${id}`, centre).then((r) => r.data),
  centreUsers: (id: number) => http.get<PortalUserRow[]>(`/platform/centres/${id}/users`).then((r) => r.data),
  addCentreUser: (id: number, user: UserInput) => http.post<PortalUserRow>(`/platform/centres/${id}/users`, user).then((r) => r.data),
  admins: () => http.get<PortalUserRow[]>('/platform/admins').then((r) => r.data),
  addAdmin: (user: UserInput) => http.post<PortalUserRow>('/platform/admins', user).then((r) => r.data),
  updateUser: (id: number, input: { fullName?: string | null; enabled?: boolean }) =>
    http.put<PortalUserRow>(`/platform/users/${id}`, input).then((r) => r.data),
  resetPassword: (id: number, newPassword: string) => http.post<void>(`/platform/users/${id}/reset-password`, { newPassword }),
  deleteUser: (id: number) => http.delete<void>(`/platform/users/${id}`),
}

/** The current centre's email and SMS settings. */
export const centreSettingsApi = {
  get: () => http.get<MessagingSettings>('/centre/settings').then((r) => r.data),
  update: (input: MessagingSettingsInput) => http.put<MessagingSettings>('/centre/settings', input).then((r) => r.data),
  testEmail: (to: string) => http.post<void>('/centre/settings/test-email', { to }),
}
