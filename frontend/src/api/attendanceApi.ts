import { http } from './client'
import { compact } from '@/lib/utils'
import { downloadFile } from '@/lib/download'
import type { AttendanceGrid, AttendanceUpdate, StudentAttendanceHistory } from '@/types'

export const attendanceApi = {
  grid: (month: string, groupId?: number | '') =>
    http.get<AttendanceGrid>('/attendance', { params: compact({ month, groupId }) }).then((r) => r.data),
  bulk: (updates: AttendanceUpdate[]) =>
    http.put<{ created: number; updated: number; removed: number }>('/attendance/bulk', updates).then((r) => r.data),
  exportXlsx: (month: string, groupId?: number | '') =>
    downloadFile('/attendance/export', compact({ month, groupId }), `attendance-${month}.xlsx`),
  history: (studentId: number) =>
    http.get<StudentAttendanceHistory>(`/attendance/students/${studentId}`).then((r) => r.data),
}
