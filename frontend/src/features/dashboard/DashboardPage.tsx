import { useEffect, useState, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Bar, BarChart, CartesianGrid, Legend, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { CalendarDays, ChevronLeft, ChevronRight, DollarSign, MessageSquareText, UserCheck, Users } from 'lucide-react'
import { dashboardApi } from '@/api/dashboardApi'
import { ErrorState, TableSkeleton } from '@/components/States'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { Table, TableBody, TableCell, TableFooter, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useMonthParam } from '@/hooks/useMonthParam'
import { formatDate, formatMonth } from '@/lib/dates'
import { formatMoney, formatPercent } from '@/lib/money'

// Categorical slots 1 and 2 of the validated reference palette (adjacent pair passes CVD checks).
const SERIES = { billed: '#2a78d6', collected: '#eb6834' }

function Stat({ label, value, testId, sub }: { label: string; value: ReactNode; testId: string; sub?: ReactNode }) {
  return (
    <div>
      <p className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{label}</p>
      <p className="mt-1 text-2xl font-semibold tabular-nums" data-testid={testId}>{value}</p>
      {sub && <p className="text-xs text-muted-foreground">{sub}</p>}
    </div>
  )
}

function SectionCard({ title, icon, children }: { title: string; icon: ReactNode; children: ReactNode }) {
  return (
    <Card>
      <CardHeader className="flex-row items-center gap-2 space-y-0">
        <span className="text-muted-foreground" aria-hidden>{icon}</span>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>{children}</CardContent>
    </Card>
  )
}

const shortMonth = (m: string) => formatMonth(m).slice(0, 3)

function MonthlySummary({ year, setYear }: { year: number; setYear: (y: number) => void }) {
  const q = useQuery({ queryKey: ['dashboard', 'summary', year], queryFn: () => dashboardApi.monthlySummary(year) })
  const chartData = q.data?.months.map((m) => ({ month: shortMonth(m.month), Billed: m.billed, Collected: m.collected }))

  return (
    <Card className="lg:col-span-3">
      <CardHeader className="flex-row items-center justify-between space-y-0">
        <CardTitle>Monthly summary</CardTitle>
        <div className="flex items-center gap-1">
          <Button variant="outline" size="icon" aria-label="Previous year" onClick={() => setYear(year - 1)}><ChevronLeft /></Button>
          <span className="w-14 text-center text-sm font-medium" data-testid="summary-year">{year}</span>
          <Button variant="outline" size="icon" aria-label="Next year" onClick={() => setYear(year + 1)}><ChevronRight /></Button>
        </div>
      </CardHeader>
      <CardContent className="grid gap-6 xl:grid-cols-2">
        {q.isLoading ? <TableSkeleton rows={12} /> : q.isError ? <ErrorState error={q.error} /> : (
          <>
            <div className="h-80" role="img" aria-label={`Billed versus collected by month for ${year}; values are listed in the table`}>
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} barGap={2} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                  <CartesianGrid vertical={false} stroke="hsl(var(--border))" />
                  <XAxis dataKey="month" tickLine={false} axisLine={{ stroke: 'hsl(var(--border))' }} tick={{ fill: 'hsl(var(--muted-foreground))', fontSize: 12 }} />
                  <YAxis tickLine={false} axisLine={false} width={64} tick={{ fill: 'hsl(var(--muted-foreground))', fontSize: 12 }}
                    tickFormatter={(v: number) => `$${v.toLocaleString('en-CA')}`} />
                  <Tooltip
                    cursor={{ fill: 'hsl(var(--muted))' }}
                    formatter={(v: number, name: string) => [formatMoney(v), name]}
                    contentStyle={{ borderRadius: 8, border: '1px solid hsl(var(--border))', fontSize: 12, color: 'hsl(var(--foreground))' }}
                  />
                  <Legend iconType="square" wrapperStyle={{ fontSize: 12, color: 'hsl(var(--foreground))' }} />
                  <Bar dataKey="Billed" fill={SERIES.billed} radius={[4, 4, 0, 0]} maxBarSize={24} />
                  <Bar dataKey="Collected" fill={SERIES.collected} radius={[4, 4, 0, 0]} maxBarSize={24} />
                </BarChart>
              </ResponsiveContainer>
            </div>
            <div className="rounded-md border" data-testid="monthly-summary-table">
              <Table>
                <TableHeader>
                  <TableRow><TableHead>Month</TableHead><TableHead className="text-right">Billed</TableHead><TableHead className="text-right">Collected</TableHead><TableHead className="text-right">Outstanding</TableHead></TableRow>
                </TableHeader>
                <TableBody>
                  {q.data!.months.map((m) => (
                    <TableRow key={m.month} data-testid={`summary-row-${m.month}`}>
                      <TableCell>{formatMonth(m.month)}</TableCell>
                      <TableCell className="text-right tabular-nums">{formatMoney(m.billed)}</TableCell>
                      <TableCell className="text-right tabular-nums">{formatMoney(m.collected)}</TableCell>
                      <TableCell className="text-right tabular-nums">{formatMoney(m.outstanding)}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
                <TableFooter>
                  <TableRow data-testid="year-total-row">
                    <TableCell>YEAR TOTAL</TableCell>
                    <TableCell className="text-right tabular-nums">{formatMoney(q.data!.total.billed)}</TableCell>
                    <TableCell className="text-right tabular-nums">{formatMoney(q.data!.total.collected)}</TableCell>
                    <TableCell className="text-right tabular-nums">{formatMoney(q.data!.total.outstanding)}</TableCell>
                  </TableRow>
                </TableFooter>
              </Table>
            </div>
          </>
        )}
      </CardContent>
    </Card>
  )
}

export function DashboardPage() {
  const [month] = useMonthParam()
  const monthYear = Number(month.slice(0, 4))
  const [year, setYear] = useState(monthYear)
  useEffect(() => setYear(monthYear), [monthYear])
  const q = useQuery({ queryKey: ['dashboard', month], queryFn: () => dashboardApi.get(month) })
  const d = q.data

  return (
    <div>
      <div className="mb-4">
        <h1 className="text-2xl font-semibold tracking-tight">Dashboard</h1>
        <p className="mt-1 text-sm text-muted-foreground">{formatMonth(month)}</p>
      </div>
      {q.isError ? <ErrorState error={q.error} onRetry={() => q.refetch()} /> : (
        <div className="grid gap-4 lg:grid-cols-3">
          <SectionCard title="Students" icon={<Users className="h-4 w-4" />}>
            {!d ? <Skeleton className="h-16" /> : (
              <div className="grid grid-cols-2 gap-4">
                <Stat label="Active" value={d.students.active} testId="stat-active-students" />
                <Stat label="Inactive" value={d.students.inactive} testId="stat-inactive-students" />
              </div>
            )}
          </SectionCard>

          <SectionCard title={`Fees — ${formatMonth(month)}`} icon={<DollarSign className="h-4 w-4" />}>
            {!d ? <Skeleton className="h-16" /> : (
              <div className="grid grid-cols-2 gap-4">
                <Stat label="Fees billed" value={formatMoney(d.fees.billed)} testId="stat-billed" />
                <Stat label="Fees collected" value={formatMoney(d.fees.collected)} testId="stat-collected" />
                <Stat label="Outstanding" value={formatMoney(d.fees.outstanding)} testId="stat-outstanding" />
                <Stat label="Collection rate" value={formatPercent(d.fees.collectionRatePct)} testId="stat-collection-rate" />
              </div>
            )}
          </SectionCard>

          <SectionCard title={`Attendance — ${formatMonth(month)}`} icon={<UserCheck className="h-4 w-4" />}>
            {!d ? <Skeleton className="h-16" /> : (
              <div className="grid grid-cols-2 gap-4">
                <Stat label="Classes held (max)" value={d.attendance.classesHeldMax} testId="stat-classes-held" />
                <Stat
                  label="Average attendance"
                  value={formatPercent(d.attendance.averageAttendancePct)}
                  testId="stat-avg-attendance"
                  sub={`${d.attendance.totalPresent} present / ${d.attendance.totalPresent + d.attendance.totalAbsent} held`}
                />
              </div>
            )}
          </SectionCard>

          <MonthlySummary year={year} setYear={setYear} />

          <SectionCard title="Upcoming classes this week" icon={<CalendarDays className="h-4 w-4" />}>
            {!d ? <Skeleton className="h-24" /> : d.upcomingClasses.length === 0 ? (
              <p className="text-sm text-muted-foreground">No more classes this week.</p>
            ) : (
              <ul className="divide-y text-sm" data-testid="upcoming-classes">
                {d.upcomingClasses.map((c) => (
                  <li key={`${c.slotId}-${c.date}`} className="py-2">
                    <p className="font-medium">{c.classGroupName}{c.subject ? ` — ${c.subject}` : ''}</p>
                    <p className="text-muted-foreground">{formatDate(c.date, 'EEE, MMM d')} · {c.startTime}–{c.endTime}{c.location ? ` · ${c.location}` : ''}</p>
                  </li>
                ))}
              </ul>
            )}
          </SectionCard>

          <SectionCard title="Outstanding balances" icon={<DollarSign className="h-4 w-4" />}>
            {!d ? <Skeleton className="h-24" /> : d.outstandingStudents.length === 0 ? (
              <p className="text-sm text-muted-foreground">No students have an outstanding balance.</p>
            ) : (
              <ul className="divide-y text-sm" data-testid="outstanding-students">
                {d.outstandingStudents.map((s) => (
                  <li key={s.studentId} className="flex items-center justify-between py-2">
                    <Link to={`/students/${s.studentId}`} className="text-primary hover:underline">{s.studentCode} {s.fullName}</Link>
                    <span className="font-medium tabular-nums">{formatMoney(s.balance)}</span>
                  </li>
                ))}
              </ul>
            )}
          </SectionCard>

          <SectionCard title="Feedback" icon={<MessageSquareText className="h-4 w-4" />}>
            {!d ? <Skeleton className="h-16" /> : (
              <div className="space-y-3">
                <Stat label={`Not yet entered for ${formatMonth(month)}`} value={d.feedbackMissing.count} testId="stat-feedback-missing" />
                <Button variant="outline" size="sm" asChild>
                  <Link to={`/feedback?month=${month}`}>Go to feedback</Link>
                </Button>
              </div>
            )}
          </SectionCard>
        </div>
      )}
    </div>
  )
}
