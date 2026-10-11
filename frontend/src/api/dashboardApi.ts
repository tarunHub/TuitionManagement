import { http } from './client'
import type { Dashboard, MonthlySummary } from '@/types'

export const dashboardApi = {
  get: (month: string) => http.get<Dashboard>('/dashboard', { params: { month } }).then((r) => r.data),
  monthlySummary: (year: number) =>
    http.get<MonthlySummary>('/dashboard/monthly-summary', { params: { year } }).then((r) => r.data),
}
