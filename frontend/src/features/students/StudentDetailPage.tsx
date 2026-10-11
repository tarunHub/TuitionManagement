import { useState, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Pencil, Power, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { attendanceApi } from '@/api/attendanceApi'
import { feedbackApi } from '@/api/feedbackApi'
import { feesApi } from '@/api/feesApi'
import { studentsApi } from '@/api/studentsApi'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { PageHeader } from '@/components/PageHeader'
import { EmptyState, ErrorState, TableSkeleton } from '@/components/States'
import { FeeStatusBadge, StudentStatusBadge } from '@/components/StatusBadge'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { formatDate, formatMonth } from '@/lib/dates'
import { errorMessage } from '@/lib/errors'
import { formatMoney, formatPercent } from '@/lib/money'
import type { Student } from '@/types'

function Info({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div>
      <dt className="text-xs font-medium uppercase tracking-wide text-muted-foreground">{label}</dt>
      <dd className="mt-0.5 text-sm">{children || '—'}</dd>
    </div>
  )
}

function Profile({ s }: { s: Student }) {
  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Card>
        <CardHeader><CardTitle>Student</CardTitle></CardHeader>
        <CardContent>
          <dl className="grid grid-cols-2 gap-4">
            <Info label="Date of birth">{formatDate(s.dateOfBirth)}</Info>
            <Info label="Grade">{s.grade}</Info>
            <Info label="School">{s.school}</Info>
            <Info label="Subjects">{s.subjects.join(', ')}</Info>
            <Info label="Address">{s.address}</Info>
          </dl>
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle>Guardian & Contacts</CardTitle></CardHeader>
        <CardContent>
          <dl className="grid grid-cols-2 gap-4">
            <Info label="Guardian">{s.guardianName}</Info>
            <Info label="Email">{s.email}</Info>
            <Info label="Primary contact">{s.primaryContact}</Info>
            <Info label="SMS reminders">{s.smsOptIn ? 'Yes (opted in)' : 'No'}</Info>
            <Info label="Alternate contact">{s.alternateContact}</Info>
            <Info label="Emergency contact">{s.emergencyContactName}</Info>
            <Info label="Emergency phone">{s.emergencyContactPhone}</Info>
          </dl>
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle>Enrollment & Billing</CardTitle></CardHeader>
        <CardContent>
          <dl className="grid grid-cols-2 gap-4">
            <Info label="Enrolled">{formatDate(s.enrollmentDate)}</Info>
            <Info label="Class group">{s.classGroupName}</Info>
            <Info label="Per-class rate">{formatMoney(s.perClassRate)}</Info>
            <Info label="Status"><StudentStatusBadge status={s.status} /></Info>
          </dl>
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle>Notes</CardTitle></CardHeader>
        <CardContent>
          <p className="whitespace-pre-wrap text-sm">{s.notes || '—'}</p>
        </CardContent>
      </Card>
    </div>
  )
}

function AttendanceTab({ studentId }: { studentId: number }) {
  const q = useQuery({ queryKey: ['attendance-history', studentId], queryFn: () => attendanceApi.history(studentId) })
  if (q.isLoading) return <TableSkeleton />
  if (q.isError) return <ErrorState error={q.error} />
  if (!q.data?.months.length) return <EmptyState title="No attendance recorded" />
  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Card>
        <CardHeader><CardTitle>By month</CardTitle></CardHeader>
        <CardContent>
          <Table>
            <TableHeader><TableRow><TableHead>Month</TableHead><TableHead>Present</TableHead><TableHead>Absent</TableHead><TableHead>Held</TableHead><TableHead>Attendance</TableHead></TableRow></TableHeader>
            <TableBody>
              {q.data.months.map((m) => (
                <TableRow key={m.month}>
                  <TableCell>{formatMonth(m.month)}</TableCell>
                  <TableCell>{m.present}</TableCell>
                  <TableCell>{m.absent}</TableCell>
                  <TableCell>{m.held}</TableCell>
                  <TableCell>{formatPercent(m.attendancePct)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
      <Card>
        <CardHeader><CardTitle>Recent classes</CardTitle></CardHeader>
        <CardContent className="max-h-96 overflow-y-auto">
          <ul className="divide-y text-sm">
            {q.data.entries.slice(0, 60).map((e) => (
              <li key={e.date} className="flex justify-between py-1.5">
                <span>{formatDate(e.date, 'EEE, MMM d, yyyy')}</span>
                <Badge variant={e.status === 'PRESENT' ? 'green' : 'red'}>{e.status === 'PRESENT' ? 'Present' : 'Absent'}</Badge>
              </li>
            ))}
          </ul>
        </CardContent>
      </Card>
    </div>
  )
}

function FeesTab({ studentId }: { studentId: number }) {
  const q = useQuery({ queryKey: ['fees-history', studentId], queryFn: () => feesApi.studentHistory(studentId) })
  if (q.isLoading) return <TableSkeleton />
  if (q.isError) return <ErrorState error={q.error} />
  if (!q.data?.length) return <EmptyState title="No fees generated yet" />
  return (
    <div className="rounded-lg border bg-card">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Month</TableHead><TableHead>Rate</TableHead><TableHead>Held</TableHead><TableHead className="text-right">Due</TableHead>
            <TableHead className="text-right">Paid</TableHead><TableHead className="text-right">Balance</TableHead><TableHead>Status</TableHead><TableHead>Last payment</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {q.data.map((f) => (
            <TableRow key={f.id}>
              <TableCell>{formatMonth(f.month)}</TableCell>
              <TableCell>{formatMoney(f.rate)}</TableCell>
              <TableCell>{f.classesHeld}</TableCell>
              <TableCell className="text-right">{formatMoney(f.amountDue)}</TableCell>
              <TableCell className="text-right">{formatMoney(f.amountPaid)}</TableCell>
              <TableCell className="text-right">{formatMoney(f.balance)}</TableCell>
              <TableCell><FeeStatusBadge status={f.status} /></TableCell>
              <TableCell>{formatDate(f.lastPaymentDate)}</TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}

function FeedbackTab({ studentId }: { studentId: number }) {
  const q = useQuery({ queryKey: ['feedback-history', studentId], queryFn: () => feedbackApi.studentHistory(studentId) })
  if (q.isLoading) return <TableSkeleton />
  if (q.isError) return <ErrorState error={q.error} />
  if (!q.data?.length) return <EmptyState title="No feedback recorded" />
  return (
    <div className="space-y-3">
      {q.data.map((f) => (
        <Card key={f.id}>
          <CardHeader className="flex-row items-center justify-between space-y-0">
            <CardTitle>{formatMonth(f.month)}</CardTitle>
            <Button variant="outline" size="sm" asChild>
              <Link to={`/feedback/${studentId}/report?month=${f.month}`}>Printable report</Link>
            </Button>
          </CardHeader>
          <CardContent className="grid gap-3 text-sm md:grid-cols-3">
            <div><p className="font-medium">Topics taught</p><p className="whitespace-pre-wrap text-muted-foreground">{f.topicsTaught}</p></div>
            <div><p className="font-medium">Improvement areas</p><p className="whitespace-pre-wrap text-muted-foreground">{f.improvementAreas || '—'}</p></div>
            <div><p className="font-medium">Achievements</p><p className="whitespace-pre-wrap text-muted-foreground">{f.achievements || '—'}</p></div>
          </CardContent>
        </Card>
      ))}
    </div>
  )
}

export function StudentDetailPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [confirmOpen, setConfirmOpen] = useState(false)
  const q = useQuery({ queryKey: ['student', id], queryFn: () => studentsApi.get(id) })

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['students'] })
    queryClient.invalidateQueries({ queryKey: ['student', id] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const statusMutation = useMutation({
    mutationFn: (status: 'ACTIVE' | 'INACTIVE') => studentsApi.setStatus(id, status),
    onSuccess: (s) => {
      toast.success(s.status === 'ACTIVE' ? 'Student activated' : 'Student deactivated')
      setConfirmOpen(false)
      invalidate()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const deleteMutation = useMutation({
    mutationFn: () => studentsApi.remove(id),
    onSuccess: () => {
      toast.success('Student deleted')
      setConfirmOpen(false)
      invalidate()
      navigate('/students')
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  if (q.isLoading) return <TableSkeleton />
  if (q.isError || !q.data) return <ErrorState error={q.error} onRetry={() => q.refetch()} />
  const s = q.data
  const hasHistory = !!s.hasHistory
  const canDeactivate = s.status === 'ACTIVE'

  return (
    <div>
      <PageHeader
        title={s.fullName}
        description={
          <span className="flex items-center gap-2">
            <span data-testid="student-code">{s.studentCode}</span> · {s.grade} <StudentStatusBadge status={s.status} />
          </span>
        }
        actions={
          <>
            <Button variant="outline" asChild data-testid="edit-student">
              <Link to={`/students/${id}/edit`}><Pencil /> Edit</Link>
            </Button>
            {s.status === 'INACTIVE' && (
              <Button variant="outline" onClick={() => statusMutation.mutate('ACTIVE')} disabled={statusMutation.isPending} data-testid="activate-student">
                <Power /> Activate
              </Button>
            )}
            {(!hasHistory || canDeactivate) && (
              <Button variant="destructive" onClick={() => setConfirmOpen(true)} data-testid="delete-student">
                <Trash2 /> {hasHistory ? 'Deactivate' : 'Delete'}
              </Button>
            )}
          </>
        }
      />
      <Tabs defaultValue="profile">
        <TabsList>
          <TabsTrigger value="profile">Profile</TabsTrigger>
          <TabsTrigger value="attendance" data-testid="tab-attendance">Attendance history</TabsTrigger>
          <TabsTrigger value="fees" data-testid="tab-fees">Fees history</TabsTrigger>
          <TabsTrigger value="feedback" data-testid="tab-feedback">Feedback history</TabsTrigger>
        </TabsList>
        <TabsContent value="profile"><Profile s={s} /></TabsContent>
        <TabsContent value="attendance"><AttendanceTab studentId={id} /></TabsContent>
        <TabsContent value="fees"><FeesTab studentId={id} /></TabsContent>
        <TabsContent value="feedback"><FeedbackTab studentId={id} /></TabsContent>
      </Tabs>

      <ConfirmDialog
        open={confirmOpen}
        onOpenChange={setConfirmOpen}
        destructive
        title={hasHistory ? `Deactivate ${s.fullName}?` : `Delete ${s.fullName}?`}
        description={
          hasHistory ? (
            <p data-testid="delete-explanation">
              {s.fullName} has attendance, fee or feedback history, so the record cannot be deleted. The student will be
              <strong> deactivated</strong> instead: their history is kept, and they no longer appear in attendance, fees or
              notifications. You can reactivate them later.
            </p>
          ) : (
            <p data-testid="delete-explanation">
              {s.fullName} has no attendance, fee or feedback history, so the record will be <strong>permanently deleted</strong>.
              Their student ID ({s.studentCode}) will not be reused.
            </p>
          )
        }
        confirmLabel={hasHistory ? 'Deactivate' : 'Delete'}
        pending={statusMutation.isPending || deleteMutation.isPending}
        onConfirm={() => (hasHistory ? statusMutation.mutate('INACTIVE') : deleteMutation.mutate())}
      />
    </div>
  )
}
