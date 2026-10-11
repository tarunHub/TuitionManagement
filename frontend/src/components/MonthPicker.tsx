import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { shiftMonth, type MonthParam } from '@/lib/dates'

export function MonthPicker({ month, onChange }: { month: MonthParam; onChange: (m: MonthParam) => void }) {
  return (
    <div className="flex items-center gap-1" data-testid="month-picker">
      <Button variant="outline" size="icon" aria-label="Previous month" onClick={() => onChange(shiftMonth(month, -1))} data-testid="month-prev">
        <ChevronLeft />
      </Button>
      <label className="sr-only" htmlFor="month-input">Month</label>
      <input
        id="month-input"
        type="month"
        value={month}
        onChange={(e) => e.target.value && onChange(e.target.value)}
        className="h-9 rounded-md border border-input bg-background px-2 text-sm"
        data-testid="month-input"
      />
      <Button variant="outline" size="icon" aria-label="Next month" onClick={() => onChange(shiftMonth(month, 1))} data-testid="month-next">
        <ChevronRight />
      </Button>
    </div>
  )
}
