import { http } from './client'
import type { ClassGroup, GradeRate } from '@/types'

export interface GradeRateInput {
  grade: string
  perClassRate: number
  notes?: string | null
}

export interface ClassGroupInput {
  name: string
  description?: string | null
}

export const gradeRatesApi = {
  list: () => http.get<GradeRate[]>('/grade-rates').then((r) => r.data),
  create: (input: GradeRateInput) => http.post<GradeRate>('/grade-rates', input).then((r) => r.data),
  update: (id: number, input: GradeRateInput) => http.put<GradeRate>(`/grade-rates/${id}`, input).then((r) => r.data),
  remove: (id: number) => http.delete<void>(`/grade-rates/${id}`),
}

export const classGroupsApi = {
  list: () => http.get<ClassGroup[]>('/class-groups').then((r) => r.data),
  create: (input: ClassGroupInput) => http.post<ClassGroup>('/class-groups', input).then((r) => r.data),
  update: (id: number, input: ClassGroupInput) => http.put<ClassGroup>(`/class-groups/${id}`, input).then((r) => r.data),
  remove: (id: number) => http.delete<void>(`/class-groups/${id}`),
}
