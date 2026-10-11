import { useCallback } from 'react'
import { useSearchParams } from 'react-router-dom'
import { currentMonth, isMonthParam, type MonthParam } from '@/lib/dates'

/** The `?month=YYYY-MM` URL parameter, defaulting to the current month. */
export function useMonthParam(): [MonthParam, (month: MonthParam) => void] {
  const [params, setParams] = useSearchParams()
  const raw = params.get('month')
  const month = isMonthParam(raw) ? raw : currentMonth()
  const setMonth = useCallback(
    (next: MonthParam) =>
      setParams(
        (prev) => {
          const p = new URLSearchParams(prev)
          p.set('month', next)
          return p
        },
        { replace: true },
      ),
    [setParams],
  )
  return [month, setMonth]
}
