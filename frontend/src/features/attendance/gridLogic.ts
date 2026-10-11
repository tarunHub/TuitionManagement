import type { AttendanceGrid, AttendanceStatus } from '@/types'

export type CellValue = AttendanceStatus | null

/** Click cycle: blank (or S) → P → A → blank (or S). */
export function nextStatus(current: CellValue): CellValue {
  if (current === null) return 'PRESENT'
  if (current === 'PRESENT') return 'ABSENT'
  return null
}

/** S = a class is on the calendar for the student's batch that day but nobody has marked P or A yet. */
export function cellLabel(value: CellValue, scheduled = false): string {
  return value === 'PRESENT' ? 'P' : value === 'ABSENT' ? 'A' : scheduled ? 'S' : ''
}

export function isScheduled(grid: AttendanceGrid, rowIndex: number, dayIndex: number): boolean {
  return !!grid.rows[rowIndex].scheduled?.[dayIndex]
}

/** Scheduled classes on or before today that still have no P/A (the "to mark" count). */
export function countToMark(grid: AttendanceGrid, edits: Map<string, CellValue>): number {
  let n = 0
  grid.rows.forEach((_, r) => {
    grid.days.forEach((day, d) => {
      if (!day.future && isScheduled(grid, r, d) && effectiveCell(grid, edits, r, d) === null) n++
    })
  })
  return n
}

/** Edits are keyed by student and ISO date so they survive switching months before saving. */
export const editKey = (studentId: number, date: string) => `${studentId}|${date}`

export function parseEditKey(key: string): { studentId: number; date: string } {
  const [id, date] = key.split('|')
  return { studentId: Number(id), date }
}

export interface RowTotals {
  present: number
  absent: number
  held: number
  pct: number | null
}

export function effectiveCell(grid: AttendanceGrid, edits: Map<string, CellValue>, rowIndex: number, dayIndex: number): CellValue {
  const row = grid.rows[rowIndex]
  const key = editKey(row.studentId, grid.days[dayIndex].date)
  return edits.has(key) ? edits.get(key)! : row.cells[dayIndex]
}

export function computeTotals(grid: AttendanceGrid, edits: Map<string, CellValue>) {
  const rows: RowTotals[] = []
  const presentPerDay = new Array(grid.days.length).fill(0)
  let present = 0
  let absent = 0
  grid.rows.forEach((_, r) => {
    let p = 0
    let a = 0
    grid.days.forEach((__, d) => {
      const v = effectiveCell(grid, edits, r, d)
      if (v === 'PRESENT') {
        p++
        presentPerDay[d]++
      } else if (v === 'ABSENT') {
        a++
      }
    })
    present += p
    absent += a
    rows.push({ present: p, absent: a, held: p + a, pct: p + a === 0 ? null : (p * 100) / (p + a) })
  })
  const held = present + absent
  return { rows, presentPerDay, totals: { present, absent, held, pct: held === 0 ? null : (present * 100) / held } }
}
