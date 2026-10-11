import { http } from './client'
import { compact } from '@/lib/utils'
import { downloadFile } from '@/lib/download'
import type { Page, Student, StudentFilters, StudentInput, StudentStatus } from '@/types'

export interface StudentListParams extends StudentFilters {
  page?: number
  size?: number
  sort?: string
}

export const studentsApi = {
  list: (params: StudentListParams) =>
    http.get<Page<Student>>('/students', { params: compact({ ...params }) }).then((r) => r.data),
  get: (id: number) => http.get<Student>(`/students/${id}`).then((r) => r.data),
  create: (input: StudentInput) => http.post<Student>('/students', input).then((r) => r.data),
  update: (id: number, input: StudentInput) => http.put<Student>(`/students/${id}`, input).then((r) => r.data),
  setStatus: (id: number, status: StudentStatus) =>
    http.patch<Student>(`/students/${id}/status`, { status }).then((r) => r.data),
  remove: (id: number) => http.delete<void>(`/students/${id}`),
  exportCsv: (filters: StudentFilters) => downloadFile('/students/export', compact({ ...filters }), 'students.csv'),
}
