import { Link, useParams, useSearchParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Printer } from 'lucide-react'
import { attendanceApi } from '@/api/attendanceApi'
import { feedbackApi } from '@/api/feedbackApi'
import { studentsApi } from '@/api/studentsApi'
import { EmptyState, ErrorState, TableSkeleton } from '@/components/States'
import { Button } from '@/components/ui/button'
import { currentMonth, formatMonth, isMonthParam } from '@/lib/dates'
import { formatPercent } from '@/lib/money'

/** Printable per-student monthly report (use the browser's "Save as PDF"). */
export function FeedbackReportPage() {
  const studentId = Number(useParams().studentId)
  const [params] = useSearchParams()
  const raw = params.get('month')
  const month = isMonthParam(raw) ? raw : currentMonth()

  const student = useQuery({ queryKey: ['student', studentId], queryFn: () => studentsApi.get(studentId) })
  const history = useQuery({ queryKey: ['feedback-history', studentId], queryFn: () => feedbackApi.studentHistory(studentId) })
  const attendance = useQuery({ queryKey: ['attendance-history', studentId], queryFn: () => attendanceApi.history(studentId) })

  if (student.isLoading || history.isLoading) return <TableSkeleton />
  if (student.isError) return <ErrorState error={student.error} />
  const feedback = history.data?.find((f) => f.month === month)
  const att = attendance.data?.months.find((m) => m.month === month)
  const s = student.data!

  return (
    <div className="mx-auto max-w-3xl">
      <div className="no-print mb-4 flex justify-between">
        <Button variant="outline" asChild>
          <Link to={`/feedback?month=${month}`}><ArrowLeft /> Back</Link>
        </Button>
        <Button onClick={() => window.print()} disabled={!feedback} data-testid="print-report"><Printer /> Print / Save as PDF</Button>
      </div>
      {!feedback ? (
        <EmptyState title="No feedback for this month" description={`Nothing has been entered for ${s.fullName} in ${formatMonth(month)}.`} />
      ) : (
        <article className="rounded-lg border bg-white p-8 shadow-sm print:border-0 print:shadow-none" data-testid="feedback-report">
          <header className="mb-6 border-b pb-4">
            <p className="text-sm font-semibold uppercase tracking-wide text-primary">Smart Start Tutoring</p>
            <h1 className="mt-1 text-2xl font-bold">Monthly Progress Report</h1>
            <p className="text-muted-foreground">{formatMonth(month)}</p>
          </header>
          <dl className="mb-6 grid grid-cols-2 gap-3 text-sm">
            <div><dt className="text-muted-foreground">Student</dt><dd className="font-medium">{s.fullName} ({s.studentCode})</dd></div>
            <div><dt className="text-muted-foreground">Grade</dt><dd className="font-medium">{s.grade}</dd></div>
            <div><dt className="text-muted-foreground">Parent/Guardian</dt><dd className="font-medium">{s.guardianName}</dd></div>
            <div>
              <dt className="text-muted-foreground">Attendance</dt>
              <dd className="font-medium">{att ? `${att.present} of ${att.held} classes (${formatPercent(att.attendancePct)})` : '—'}</dd>
            </div>
          </dl>
          {[
            ['Topics taught', feedback.topicsTaught],
            ['Areas for improvement', feedback.improvementAreas],
            ['Achievements', feedback.achievements],
          ].map(([title, text]) => (
            <section key={title} className="mb-5">
              <h2 className="mb-1 text-lg font-semibold">{title}</h2>
              <p className="whitespace-pre-wrap text-sm leading-relaxed">{text || '—'}</p>
            </section>
          ))}
        </article>
      )}
    </div>
  )
}
