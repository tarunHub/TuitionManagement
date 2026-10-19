import { addMonths, format, isValid, parse, parseISO } from 'date-fns'

/** Month params are always YYYY-MM. */
export type MonthParam = string

const MONTH_RE = /^\d{4}-(0[1-9]|1[0-2])$/

export function isMonthParam(value: string | null | undefined): value is MonthParam {
  return !!value && MONTH_RE.test(value)
}

export function currentMonth(): MonthParam {
  return format(new Date(), 'yyyy-MM')
}

/** Months before the current one are closed: their schedule, feedback and fee bills can't be changed. */
export function isPastMonth(month: MonthParam): boolean {
  return month < currentMonth()
}

export function shiftMonth(month: MonthParam, delta: number): MonthParam {
  return format(addMonths(monthToDate(month), delta), 'yyyy-MM')
}

export function monthToDate(month: MonthParam): Date {
  return parse(`${month}-01`, 'yyyy-MM-dd', new Date())
}

export function formatMonth(month: MonthParam): string {
  return format(monthToDate(month), 'MMMM yyyy')
}

/** Formats an ISO date (YYYY-MM-DD) or timestamp for display. */
export function formatDate(value: string | null | undefined, pattern = 'MMM d, yyyy'): string {
  if (!value) return '—'
  const d = value.length === 10 ? parse(value, 'yyyy-MM-dd', new Date()) : parseISO(value)
  return isValid(d) ? format(d, pattern) : '—'
}

export function formatDateTime(value: string | null | undefined): string {
  return formatDate(value, 'MMM d, yyyy h:mm a')
}

export function todayIso(): string {
  return format(new Date(), 'yyyy-MM-dd')
}
