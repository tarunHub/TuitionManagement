import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { ColumnDef, PaginationState, SortingState } from '@tanstack/react-table'
import { Download, Pencil, Plus, Trash2 } from 'lucide-react'
import { toast } from 'sonner'
import { studentsApi } from '@/api/studentsApi'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { DataTable } from '@/components/DataTable'
import { PageHeader } from '@/components/PageHeader'
import { ErrorState } from '@/components/States'
import { StudentStatusBadge } from '@/components/StatusBadge'
import { Button } from '@/components/ui/button'
import { NativeSelect } from '@/components/ui/native-select'
import { useClassGroups, useGradeRates } from '@/hooks/useReferenceData'
import { errorMessage } from '@/lib/errors'
import { formatMoney } from '@/lib/money'
import type { Student, StudentStatus } from '@/types'

const buildColumns = (onEdit: (s: Student) => void, onDelete: (s: Student) => void): ColumnDef<Student>[] => [
  { accessorKey: 'studentCode', header: 'Student ID', enableHiding: false },
  {
    accessorKey: 'fullName',
    header: 'Full Name',
    enableHiding: false,
    cell: ({ row }) => (
      <Link to={`/students/${row.original.id}`} className="font-medium text-primary hover:underline" onClick={(e) => e.stopPropagation()}>
        {row.original.fullName}
      </Link>
    ),
  },
  { accessorKey: 'grade', header: 'Grade' },
  { id: 'classGroupName', accessorKey: 'classGroupName', header: 'Group', cell: ({ getValue }) => getValue<string>() ?? '—' },
  { accessorKey: 'guardianName', header: 'Guardian' },
  { accessorKey: 'primaryContact', header: 'Primary Contact' },
  { accessorKey: 'email', header: 'Email' },
  { accessorKey: 'perClassRate', header: 'Rate', cell: ({ getValue }) => formatMoney(getValue<number>()), meta: { className: 'text-right' } },
  { accessorKey: 'status', header: 'Status', cell: ({ getValue }) => <StudentStatusBadge status={getValue<StudentStatus>()} /> },
  {
    id: 'actions',
    header: 'Actions',
    enableSorting: false,
    enableHiding: false,
    cell: ({ row }) => (
      <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8"
          aria-label={`Edit ${row.original.fullName}`}
          title="Edit"
          onClick={() => onEdit(row.original)}
          data-testid={`edit-student-${row.original.studentCode}`}
        >
          <Pencil />
        </Button>
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-destructive hover:text-destructive"
          aria-label={`Delete ${row.original.fullName}`}
          title="Delete"
          onClick={() => onDelete(row.original)}
          data-testid={`delete-student-${row.original.studentCode}`}
        >
          <Trash2 />
        </Button>
      </div>
    ),
  },
]

export function StudentsListPage() {
  const navigate = useNavigate()
  const [search, setSearch] = useState('')
  const [debounced, setDebounced] = useState('')
  const [status, setStatus] = useState<StudentStatus | ''>('ACTIVE')
  const [grade, setGrade] = useState('')
  const [groupId, setGroupId] = useState<number | ''>('')
  const [pagination, setPagination] = useState<PaginationState>({ pageIndex: 0, pageSize: 20 })
  const [sorting, setSorting] = useState<SortingState>([{ id: 'studentCode', desc: false }])
  const grades = useGradeRates()
  const groups = useClassGroups()
  const queryClient = useQueryClient()
  const [toDelete, setToDelete] = useState<Student | null>(null)

  const columns = useMemo(
    () => buildColumns((s) => navigate(`/students/${s.id}/edit`), setToDelete),
    [navigate],
  )

  // The list response has no history flag, so load the full record to decide delete vs deactivate.
  const deleteTarget = useQuery({
    queryKey: ['student', toDelete?.id],
    queryFn: () => studentsApi.get(toDelete!.id),
    enabled: !!toDelete,
  })
  const hasHistory = !!deleteTarget.data?.hasHistory

  const invalidate = (id: number) => {
    queryClient.invalidateQueries({ queryKey: ['students'] })
    queryClient.invalidateQueries({ queryKey: ['student', id] })
    queryClient.invalidateQueries({ queryKey: ['dashboard'] })
  }

  const deleteMutation = useMutation({
    mutationFn: (s: Student) =>
      hasHistory ? studentsApi.setStatus(s.id, 'INACTIVE').then(() => 'deactivated') : studentsApi.remove(s.id).then(() => 'deleted'),
    onSuccess: (result, s) => {
      toast.success(`Student ${result}`)
      setToDelete(null)
      invalidate(s.id)
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  useEffect(() => {
    const t = setTimeout(() => setDebounced(search), 300)
    return () => clearTimeout(t)
  }, [search])

  const filters = useMemo(() => ({ search: debounced, status, grade, groupId }), [debounced, status, grade, groupId])
  const sort = sorting.map((s) => `${s.id},${s.desc ? 'desc' : 'asc'}`)[0]

  const query = useQuery({
    queryKey: ['students', filters, pagination, sort],
    queryFn: () => studentsApi.list({ ...filters, page: pagination.pageIndex, size: pagination.pageSize, sort }),
    placeholderData: keepPreviousData,
  })

  const resetPage = () => setPagination((p) => ({ ...p, pageIndex: 0 }))

  const exportCsv = async () => {
    try {
      await studentsApi.exportCsv(filters)
    } catch (e) {
      toast.error(errorMessage(e))
    }
  }

  return (
    <div>
      <PageHeader
        title="Students"
        description="Search, filter and manage enrolled students."
        actions={
          <>
            <Button variant="outline" onClick={exportCsv} data-testid="export-students">
              <Download /> Export CSV
            </Button>
            <Button asChild data-testid="add-student">
              <Link to="/students/new">
                <Plus /> Add student
              </Link>
            </Button>
          </>
        }
      />
      {query.isError ? (
        <ErrorState error={query.error} onRetry={() => query.refetch()} />
      ) : (
        <DataTable
          testId="students-table"
          columns={columns}
          data={query.data?.content ?? []}
          loading={query.isLoading}
          searchValue={search}
          onSearchChange={(v) => {
            setSearch(v)
            resetPage()
          }}
          searchPlaceholder="Search name, ID, guardian, email"
          emptyTitle="No students found"
          emptyDescription="Try changing the filters or add a new student."
          rowTestId={(s) => `student-row-${s.studentCode}`}
          onRowClick={(s) => navigate(`/students/${s.id}`)}
          server={{
            pageCount: query.data?.totalPages ?? 0,
            totalElements: query.data?.totalElements ?? 0,
            pagination,
            onPaginationChange: setPagination,
            sorting,
            onSortingChange: (u) => {
              setSorting(u)
              resetPage()
            },
          }}
          toolbar={
            <>
              <NativeSelect aria-label="Filter by status" className="w-36" value={status} onChange={(e) => { setStatus(e.target.value as StudentStatus | ''); resetPage() }} data-testid="filter-status">
                <option value="">All statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="INACTIVE">Inactive</option>
              </NativeSelect>
              <NativeSelect aria-label="Filter by grade" className="w-36" value={grade} onChange={(e) => { setGrade(e.target.value); resetPage() }} data-testid="filter-grade">
                <option value="">All grades</option>
                {grades.data?.map((g) => <option key={g.id} value={g.grade}>{g.grade}</option>)}
              </NativeSelect>
              <NativeSelect aria-label="Filter by group" className="w-40" value={groupId} onChange={(e) => { setGroupId(e.target.value ? Number(e.target.value) : ''); resetPage() }} data-testid="filter-group">
                <option value="">All groups</option>
                {groups.data?.map((g) => <option key={g.id} value={g.id}>{g.name}</option>)}
              </NativeSelect>
            </>
          }
        />
      )}
      <ConfirmDialog
        open={!!toDelete}
        onOpenChange={(open) => !open && setToDelete(null)}
        destructive
        title={toDelete ? `${hasHistory ? 'Deactivate' : 'Delete'} ${toDelete.fullName}?` : ''}
        description={
          !toDelete ? null : deleteTarget.isLoading ? (
            <p>Checking student records…</p>
          ) : deleteTarget.isError ? (
            <p>{errorMessage(deleteTarget.error)}</p>
          ) : hasHistory ? (
            toDelete.status === 'INACTIVE' ? (
              <p data-testid="delete-explanation">
                {toDelete.fullName} has attendance, fee or feedback history, so the record cannot be deleted. They are already inactive.
              </p>
            ) : (
              <p data-testid="delete-explanation">
                {toDelete.fullName} has attendance, fee or feedback history, so the record cannot be deleted. The student will be
                <strong> deactivated</strong> instead: their history is kept, and they no longer appear in attendance, fees or
                notifications. You can reactivate them later.
              </p>
            )
          ) : (
            <p data-testid="delete-explanation">
              {toDelete.fullName} has no attendance, fee or feedback history, so the record will be <strong>permanently deleted</strong>.
              Their student ID ({toDelete.studentCode}) will not be reused.
            </p>
          )
        }
        confirmLabel={hasHistory ? 'Deactivate' : 'Delete'}
        pending={deleteMutation.isPending}
        confirmDisabled={deleteTarget.isLoading || deleteTarget.isError || (hasHistory && toDelete?.status === 'INACTIVE')}
        onConfirm={() => toDelete && deleteMutation.mutate(toDelete)}
      />
    </div>
  )
}
