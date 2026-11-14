import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Loader2, LogIn, Power } from 'lucide-react'
import { toast } from 'sonner'
import { platformApi } from '@/api/platformApi'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { PageHeader } from '@/components/PageHeader'
import { ErrorState, TableSkeleton } from '@/components/States'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { useAuth } from '@/hooks/useAuth'
import { errorMessage } from '@/lib/errors'
import type { CentreInput } from '@/types'
import { CentreFields, EMPTY_CENTRE } from './CentreFields'
import { UsersPanel } from './UsersPanel'

/** One centre: its details, on/off switch and staff logins. */
export function CentreDetailPage() {
  const id = Number(useParams().id)
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const { switchCentre } = useAuth()
  const q = useQuery({ queryKey: ['platform', 'centre', id], queryFn: () => platformApi.centre(id) })
  const [form, setForm] = useState<CentreInput>(EMPTY_CENTRE)
  const [error, setError] = useState<string | null>(null)
  const [confirmToggle, setConfirmToggle] = useState(false)

  useEffect(() => {
    if (q.data) {
      setForm({ name: q.data.name, studentCodePrefix: q.data.studentCodePrefix, contactEmail: q.data.contactEmail ?? '', contactPhone: q.data.contactPhone ?? '', address: q.data.address ?? '' })
    }
  }, [q.data])

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: ['platform', 'centre', id] })
    queryClient.invalidateQueries({ queryKey: ['platform', 'centres'] })
  }

  const save = useMutation({
    mutationFn: (input: CentreInput) => platformApi.updateCentre(id, input),
    onSuccess: (c) => {
      toast.success(`${c.name} saved`)
      setError(null)
      invalidate()
    },
    onError: (e) => setError(errorMessage(e)),
  })

  const toggle = useMutation({
    mutationFn: () => platformApi.updateCentre(id, { ...form, active: !q.data!.active }),
    onSuccess: (c) => {
      toast.success(c.active ? `${c.name} is active again` : `${c.name} is now inactive; its staff can't sign in`)
      setConfirmToggle(false)
      invalidate()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  if (q.isLoading) return <TableSkeleton rows={6} />
  if (q.isError) return <ErrorState error={q.error} onRetry={() => q.refetch()} />
  const c = q.data!

  return (
    <div className="space-y-4">
      <Link to="/platform/centres" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground">
        <ArrowLeft className="size-4" /> All centres
      </Link>
      <PageHeader
        title={c.name}
        description={
          <span className="flex flex-wrap items-center gap-2">
            {c.active ? <Badge variant="green">Active</Badge> : <Badge variant="red">Inactive</Badge>}
            <span>{c.studentCount} student{c.studentCount === 1 ? '' : 's'} · {c.userCount} login{c.userCount === 1 ? '' : 's'}</span>
          </span>
        }
        actions={
          <>
            <Button variant="outline" onClick={() => setConfirmToggle(true)} data-testid="centre-toggle"><Power /> {c.active ? 'Deactivate' : 'Activate'}</Button>
            <Button onClick={() => { switchCentre({ id: c.id, name: c.name }); navigate('/dashboard') }} data-testid="centre-open"><LogIn /> Open centre</Button>
          </>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Details</CardTitle>
          <CardDescription>Email and SMS settings are set inside the centre, under Centre settings.</CardDescription>
        </CardHeader>
        <CardContent>
          <form noValidate onSubmit={(e) => { e.preventDefault(); save.mutate({ ...form, name: form.name.trim(), studentCodePrefix: form.studentCodePrefix.trim() }) }} className="space-y-3">
            <CentreFields value={form} onChange={setForm} prefixLocked />
            {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
            <div className="flex justify-end">
              <Button type="submit" disabled={save.isPending || !form.name.trim() || !/^[A-Za-z]{1,10}$/.test(form.studentCodePrefix.trim())} data-testid="centre-details-save">
                {save.isPending && <Loader2 className="animate-spin" />} Save details
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Staff logins</CardTitle>
          <CardDescription>Everyone here can use every feature of {c.name}, and only {c.name}.</CardDescription>
        </CardHeader>
        <CardContent>
          <UsersPanel
            queryKey={['platform', 'centre-users', id]}
            load={() => platformApi.centreUsers(id)}
            add={(u) => platformApi.addCentreUser(id, u)}
            addLabel="Add staff login"
            emptyText="No logins yet. Add one so the centre's staff can sign in."
          />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={confirmToggle}
        onOpenChange={setConfirmToggle}
        title={c.active ? `Deactivate ${c.name}?` : `Activate ${c.name}?`}
        description={c.active
          ? 'Its staff will be signed out and can no longer sign in, and its automatic reminders stop. No data is deleted; you can activate it again any time.'
          : 'Its staff can sign in again and automatic reminders resume.'}
        confirmLabel={c.active ? 'Deactivate' : 'Activate'}
        destructive={c.active}
        pending={toggle.isPending}
        onConfirm={() => toggle.mutate()}
      />
    </div>
  )
}
