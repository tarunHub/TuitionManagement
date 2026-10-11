import { useEffect, type ReactNode } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'
import { studentsApi } from '@/api/studentsApi'
import { FormField } from '@/components/FormField'
import { PageHeader } from '@/components/PageHeader'
import { ErrorState, TableSkeleton } from '@/components/States'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'
import { useClassGroups, useGradeRates } from '@/hooks/useReferenceData'
import { applyServerErrors, errorMessage } from '@/lib/errors'
import { emptyStudent, STUDENT_FIELDS, studentSchema, toFormValues, toInput, type StudentFormValues } from './studentSchema'

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent className="grid gap-4 sm:grid-cols-2">{children}</CardContent>
    </Card>
  )
}

export function StudentFormPage() {
  const { id } = useParams()
  const editing = !!id
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const grades = useGradeRates()
  const groups = useClassGroups()

  const student = useQuery({ queryKey: ['student', Number(id)], queryFn: () => studentsApi.get(Number(id)), enabled: editing })

  const form = useForm<StudentFormValues>({ resolver: zodResolver(studentSchema), defaultValues: emptyStudent })
  const { register, handleSubmit, setError, reset, setValue, watch, formState } = form
  const errors = formState.errors

  useEffect(() => {
    if (student.data) reset(toFormValues(student.data))
  }, [student.data, reset])

  // Rule 3: a new student's rate defaults to the grade's rate (still editable as an override).
  const grade = watch('grade')
  useEffect(() => {
    if (editing || !grade) return
    const rate = grades.data?.find((g) => g.grade === grade)?.perClassRate
    if (rate !== undefined && !formState.dirtyFields.perClassRate) setValue('perClassRate', rate)
  }, [grade, grades.data, editing, setValue, formState.dirtyFields.perClassRate])

  const mutation = useMutation({
    mutationFn: (values: StudentFormValues) =>
      editing ? studentsApi.update(Number(id), toInput(values)) : studentsApi.create(toInput(values)),
    onSuccess: (s) => {
      toast.success(editing ? 'Student updated' : `Student ${s.studentCode} created`)
      queryClient.invalidateQueries({ queryKey: ['students'] })
      queryClient.invalidateQueries({ queryKey: ['student', s.id] })
      queryClient.invalidateQueries({ queryKey: ['dashboard'] })
      queryClient.invalidateQueries({ queryKey: ['grade-rates'] })
      queryClient.invalidateQueries({ queryKey: ['class-groups'] })
      navigate(`/students/${s.id}`)
    },
    onError: (e) => {
      if (!applyServerErrors(e, setError, STUDENT_FIELDS)) toast.error(errorMessage(e))
      else toast.error('Please fix the highlighted fields')
    },
  })

  if (editing && student.isLoading) return <TableSkeleton />
  if (editing && student.isError) return <ErrorState error={student.error} onRetry={() => student.refetch()} />

  const field = (name: keyof StudentFormValues) => ({
    id: name,
    'aria-invalid': errors[name] ? true : undefined,
    'aria-describedby': errors[name] ? `${name}-error` : undefined,
    'data-testid': `student-${name}`,
  })

  return (
    <div>
      <PageHeader
        title={editing ? `Edit ${student.data?.fullName ?? 'student'}` : 'Add student'}
        description={editing ? `Student ID ${student.data?.studentCode} (cannot be changed)` : 'The student ID is generated automatically.'}
      />
      <form onSubmit={handleSubmit((v) => mutation.mutate(v))} noValidate className="space-y-4" data-testid="student-form">
        <Section title="Student">
          <FormField id="fullName" label="Full name" required error={errors.fullName?.message}>
            <Input {...field('fullName')} {...register('fullName')} />
          </FormField>
          <FormField id="dateOfBirth" label="Date of birth" error={errors.dateOfBirth?.message}>
            <Input type="date" {...field('dateOfBirth')} {...register('dateOfBirth')} />
          </FormField>
          <FormField id="grade" label="Grade" required error={errors.grade?.message}>
            <NativeSelect {...field('grade')} {...register('grade')}>
              <option value="">Select a grade</option>
              {grades.data?.map((g) => (
                <option key={g.id} value={g.grade}>{g.grade}</option>
              ))}
            </NativeSelect>
          </FormField>
          <FormField id="school" label="School" error={errors.school?.message}>
            <Input {...field('school')} {...register('school')} />
          </FormField>
          <FormField id="subjectsText" label="Subjects enrolled" hint="Comma separated, e.g. Math, English" error={errors.subjectsText?.message} className="sm:col-span-2">
            <Input {...field('subjectsText')} {...register('subjectsText')} />
          </FormField>
          <FormField id="address" label="Address" error={errors.address?.message} className="sm:col-span-2">
            <Textarea rows={2} {...field('address')} {...register('address')} />
          </FormField>
        </Section>

        <Section title="Guardian & Contacts">
          <FormField id="guardianName" label="Guardian name" required error={errors.guardianName?.message}>
            <Input {...field('guardianName')} {...register('guardianName')} />
          </FormField>
          <FormField id="email" label="Parent email" hint="Used for email notifications" error={errors.email?.message}>
            <Input type="email" {...field('email')} {...register('email')} />
          </FormField>
          <FormField id="primaryContact" label="Primary contact" hint="Used for SMS reminders" error={errors.primaryContact?.message}>
            <Input type="tel" {...field('primaryContact')} {...register('primaryContact')} />
          </FormField>
          <FormField id="alternateContact" label="Alternate contact" error={errors.alternateContact?.message}>
            <Input type="tel" {...field('alternateContact')} {...register('alternateContact')} />
          </FormField>
          <label className="flex items-start gap-2 text-sm sm:col-span-2">
            <input type="checkbox" className="mt-0.5" {...register('smsOptIn')} data-testid="student-sms-opt-in" />
            <span>
              Send SMS reminders to the primary contact
              <span className="block text-xs text-muted-foreground">Only tick this when the parent has agreed to receive text messages.</span>
            </span>
          </label>
        </Section>

        <Section title="Emergency">
          <FormField id="emergencyContactName" label="Emergency contact name" error={errors.emergencyContactName?.message}>
            <Input {...field('emergencyContactName')} {...register('emergencyContactName')} />
          </FormField>
          <FormField id="emergencyContactPhone" label="Emergency contact phone" error={errors.emergencyContactPhone?.message}>
            <Input type="tel" {...field('emergencyContactPhone')} {...register('emergencyContactPhone')} />
          </FormField>
        </Section>

        <Section title="Enrollment & Billing">
          <FormField id="enrollmentDate" label="Enrollment date" error={errors.enrollmentDate?.message}>
            <Input type="date" {...field('enrollmentDate')} {...register('enrollmentDate')} />
          </FormField>
          <FormField id="classGroupId" label="Class group" error={errors.classGroupId?.message}>
            <NativeSelect {...field('classGroupId')} {...register('classGroupId')}>
              <option value="">No group</option>
              {groups.data?.map((g) => (
                <option key={g.id} value={g.id}>{g.name}</option>
              ))}
            </NativeSelect>
          </FormField>
          <FormField id="perClassRate" label="Per-class rate ($)" required hint={editing ? 'Changing this does not affect fees already generated' : 'Defaults from the grade rate'} error={errors.perClassRate?.message}>
            <Input type="number" step="0.01" min="0" {...field('perClassRate')} {...register('perClassRate')} />
          </FormField>
          <FormField id="status" label="Status" required error={errors.status?.message}>
            <NativeSelect {...field('status')} {...register('status')}>
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </NativeSelect>
          </FormField>
        </Section>

        <Section title="Notes">
          <FormField id="notes" label="Notes" error={errors.notes?.message} className="sm:col-span-2">
            <Textarea rows={3} {...field('notes')} {...register('notes')} />
          </FormField>
        </Section>

        <div className="flex justify-end gap-2">
          <Button variant="outline" asChild>
            <Link to={editing ? `/students/${id}` : '/students'}>Cancel</Link>
          </Button>
          <Button type="submit" disabled={mutation.isPending} data-testid="student-submit">
            {mutation.isPending && <Loader2 className="animate-spin" />}
            {editing ? 'Save changes' : 'Create student'}
          </Button>
        </div>
      </form>
    </div>
  )
}
