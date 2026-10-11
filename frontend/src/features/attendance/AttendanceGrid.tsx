import { useCallback, useEffect, useMemo, useRef, useState, type KeyboardEvent } from 'react'
import { CheckCheck } from 'lucide-react'
import { formatPercent } from '@/lib/money'
import { cn } from '@/lib/utils'
import type { AttendanceGrid as Grid } from '@/types'
import { cellLabel, computeTotals, editKey, effectiveCell, isScheduled, nextStatus, type CellValue } from './gridLogic'

interface Props {
  grid: Grid
  edits: Map<string, CellValue>
  onChange: (changes: { studentId: number; date: string; value: CellValue }[]) => void
}

/** Spreadsheet-style monthly grid. Keyboard: arrows move, P/A set, Delete clears, Space/Enter cycles. */
export function AttendanceGrid({ grid, edits, onChange }: Props) {
  const [focus, setFocus] = useState<{ r: number; d: number }>({ r: 0, d: 0 })
  const cellRefs = useRef<Map<string, HTMLTableCellElement>>(new Map())
  const { rows: rowTotals, presentPerDay, totals } = useMemo(() => computeTotals(grid, edits), [grid, edits])

  useEffect(() => {
    // Keep the roving tab stop on a valid, editable day when the month changes.
    const firstEditable = grid.days.findIndex((d) => !d.future)
    setFocus({ r: 0, d: Math.max(0, firstEditable) })
  }, [grid.month, grid.days])

  const setCell = useCallback(
    (r: number, d: number, value: CellValue) => {
      if (grid.days[d].future) return
      onChange([{ studentId: grid.rows[r].studentId, date: grid.days[d].date, value }])
    },
    [grid, onChange],
  )

  const moveTo = (r: number, d: number) => {
    const nr = Math.min(Math.max(r, 0), grid.rows.length - 1)
    const nd = Math.min(Math.max(d, 0), grid.days.length - 1)
    setFocus({ r: nr, d: nd })
    cellRefs.current.get(`${nr}:${nd}`)?.focus()
  }

  const onKeyDown = (e: KeyboardEvent, r: number, d: number) => {
    const key = e.key.toLowerCase()
    const handled = true
    switch (key) {
      case 'arrowright': moveTo(r, d + 1); break
      case 'arrowleft': moveTo(r, d - 1); break
      case 'arrowdown': moveTo(r + 1, d); break
      case 'arrowup': moveTo(r - 1, d); break
      case 'p': setCell(r, d, 'PRESENT'); break
      case 'a': setCell(r, d, 'ABSENT'); break
      case 'delete':
      case 'backspace': setCell(r, d, null); break
      case ' ':
      case 'enter': setCell(r, d, nextStatus(effectiveCell(grid, edits, r, d))); break
      default: return
    }
    if (handled) e.preventDefault()
  }

  /** Marks the students scheduled that day; on a day with nothing on the class calendar, everyone. */
  const markAllPresent = (d: number) => {
    const day = grid.days[d]
    if (day.future) return
    const scheduled = grid.rows.filter((_, r) => isScheduled(grid, r, d))
    const rows = scheduled.length > 0 ? scheduled : grid.rows
    onChange(rows.map((row) => ({ studentId: row.studentId, date: day.date, value: 'PRESENT' as const })))
  }

  const stickyId = 'sticky left-0 z-20 bg-card min-w-[5.5rem] w-[5.5rem]'
  const stickyName = 'sticky left-[5.5rem] z-20 bg-card min-w-[11rem] border-r'

  return (
    <div className="max-h-[70vh] overflow-auto rounded-lg border bg-card" data-testid="attendance-grid">
      <table role="grid" aria-label={`Attendance for ${grid.month}`} className="border-separate border-spacing-0 text-sm">
        <thead className="sticky top-0 z-30 bg-muted">
          <tr>
            <th className={cn(stickyId, 'bg-muted')} />
            <th className={cn(stickyName, 'bg-muted')} />
            {grid.days.map((day) => (
              <th key={day.day} scope="col" className={cn('w-9 min-w-9 py-1 text-center text-xs font-medium text-muted-foreground', day.weekend && 'bg-slate-200/70')}>
                {day.dayLetter}
              </th>
            ))}
            <th colSpan={4} className="border-l bg-muted" />
          </tr>
          <tr>
            <th scope="col" className={cn(stickyId, 'bg-muted px-2 py-1 text-left text-xs font-semibold uppercase')}>Student ID</th>
            <th scope="col" className={cn(stickyName, 'bg-muted px-2 py-1 text-left text-xs font-semibold uppercase')}>Name</th>
            {grid.days.map((day, d) => (
              <th key={day.day} scope="col" className={cn('px-0 py-1 text-center text-xs font-semibold', day.weekend && 'bg-slate-200/70', day.future && 'text-muted-foreground/50')}>
                <div>{day.day}</div>
                <button
                  type="button"
                  className="mx-auto mt-0.5 flex rounded p-0.5 text-muted-foreground hover:bg-primary/10 hover:text-primary disabled:opacity-30"
                  title={`Mark all present on ${day.date}`}
                  aria-label={`Mark all present on ${day.date}`}
                  disabled={day.future || grid.rows.length === 0}
                  onClick={() => markAllPresent(d)}
                  data-testid={`mark-all-${day.day}`}
                >
                  <CheckCheck className="h-3.5 w-3.5" />
                </button>
              </th>
            ))}
            <th scope="col" className="border-l px-2 text-xs font-semibold">Present</th>
            <th scope="col" className="px-2 text-xs font-semibold">Absent</th>
            <th scope="col" className="px-2 text-xs font-semibold">Held</th>
            <th scope="col" className="px-2 text-xs font-semibold">Att. %</th>
          </tr>
        </thead>
        <tbody>
          {grid.rows.map((row, r) => (
            <tr key={row.studentId} data-testid={`grid-row-${row.studentCode}`}>
              <th scope="row" className={cn(stickyId, 'border-t px-2 py-1 text-left font-mono text-xs font-normal')}>{row.studentCode}</th>
              <td className={cn(stickyName, 'border-t px-2 py-1 font-medium')}>{row.fullName}</td>
              {grid.days.map((day, d) => {
                const value = effectiveCell(grid, edits, r, d)
                const scheduled = isScheduled(grid, r, d)
                const toMark = scheduled && value === null && !day.future
                const dirty = edits.has(editKey(row.studentId, day.date))
                const isFocus = focus.r === r && focus.d === d
                return (
                  <td
                    key={day.day}
                    role="gridcell"
                    ref={(el) => {
                      if (el) cellRefs.current.set(`${r}:${d}`, el)
                      else cellRefs.current.delete(`${r}:${d}`)
                    }}
                    tabIndex={isFocus ? 0 : -1}
                    aria-label={`${row.fullName}, ${day.date}: ${value === 'PRESENT' ? 'present' : value === 'ABSENT' ? 'absent' : toMark ? 'scheduled, not marked' : scheduled ? 'scheduled' : 'no class'}`}
                    aria-disabled={day.future || undefined}
                    data-testid={`cell-${row.studentCode}-${day.day}`}
                    data-value={cellLabel(value, scheduled)}
                    onClick={() => {
                      setFocus({ r, d })
                      if (!day.future) setCell(r, d, nextStatus(value))
                    }}
                    onFocus={() => setFocus({ r, d })}
                    onKeyDown={(e) => onKeyDown(e, r, d)}
                    className={cn(
                      'h-8 w-9 min-w-9 select-none border-l border-t text-center text-xs font-bold outline-none focus:ring-2 focus:ring-inset focus:ring-primary',
                      day.weekend && 'bg-slate-100',
                      day.future ? 'cursor-not-allowed bg-slate-50 text-muted-foreground/40' : 'cursor-pointer hover:bg-primary/10',
                      scheduled && value === null && (toMark ? 'bg-amber-100 text-amber-800' : 'bg-sky-50 text-sky-700'),
                      value === 'PRESENT' && 'bg-green-100 text-green-800',
                      value === 'ABSENT' && 'bg-red-100 text-red-800',
                      dirty && 'ring-1 ring-inset ring-amber-500',
                    )}
                  >
                    {cellLabel(value, scheduled)}
                  </td>
                )
              })}
              <td className="border-l border-t px-2 text-center" data-testid={`total-present-${row.studentCode}`}>{rowTotals[r].present}</td>
              <td className="border-t px-2 text-center" data-testid={`total-absent-${row.studentCode}`}>{rowTotals[r].absent}</td>
              <td className="border-t px-2 text-center font-semibold" data-testid={`total-held-${row.studentCode}`}>{rowTotals[r].held}</td>
              <td className="border-t px-2 text-center" data-testid={`total-pct-${row.studentCode}`}>{formatPercent(rowTotals[r].pct)}</td>
            </tr>
          ))}
        </tbody>
        <tfoot className="sticky bottom-0 z-30 bg-muted font-semibold">
          <tr>
            <th scope="row" colSpan={2} className="sticky left-0 z-20 border-t bg-muted px-2 py-1.5 text-left text-xs uppercase">
              Present per day
            </th>
            {presentPerDay.map((n, d) => (
              <td key={d} className={cn('border-t text-center text-xs', grid.days[d].weekend && 'bg-slate-200/70')} data-testid={`present-day-${d + 1}`}>
                {n || ''}
              </td>
            ))}
            <td className="border-l border-t px-2 text-center" data-testid="grid-total-present">{totals.present}</td>
            <td className="border-t px-2 text-center" data-testid="grid-total-absent">{totals.absent}</td>
            <td className="border-t px-2 text-center" data-testid="grid-total-held">{totals.held}</td>
            <td className="border-t px-2 text-center" data-testid="grid-total-pct">{formatPercent(totals.pct)}</td>
          </tr>
        </tfoot>
      </table>
    </div>
  )
}
