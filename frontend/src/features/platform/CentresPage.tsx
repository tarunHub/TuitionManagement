import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router-dom'
import { Building2, LogIn, Loader2, Plus } from 'lucide-react'
import { toast } from 'sonner'
import { platformApi } from '@/api/platformApi'
import { PageHeader } from '@/components/PageHeader'
import { EmptyState, ErrorState, TableSkeleton } from '@/components/States'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useAuth } from '@/hooks/useAuth'
import { errorMessage } from '@/lib/errors'
import type { Centre, CentreInput, UserInput } from '@/types'
import { CentreFields, EMPTY_CENTRE } from './CentreFields'
import { NewUserFields } from './UsersPanel'

/** Platform admin home: every centre, onboarding a new one, and switching into one. */
export function CentresPage() {
  const q = useQuery({ queryKey: ['platform', 'centres'], queryFn: platformApi.centres })
  const { switchCentre } = useAuth()
  const navigate = useNavigate()
  const [creating, setCreating] = useState(false)

  const open = (c: Centre) => {
    switchCentre({ id: c.id, name: c.name })
    navigate('/dashboard')
  }

  return (
    <div>
      <PageHeader
        title="Centres"
        description="Every tuition centre on the platform. Open one to work inside it, or manage its details and logins."
        actions={<Button onClick={() => setCreating(true)} data-testid="centre-add"><Plus /> Add centre</Button>}
      />
      {q.isLoading ? (
        <TableSkeleton rows={4} />
      ) : q.isError ? (
        <ErrorState error={q.error} onRetry={() => q.refetch()} />
      ) : q.data!.length === 0 ? (
        <EmptyState title="No centres yet" description="Add the first tuition centre to get started." />
      ) : (
        <div className="overflow-x-auto rounded-lg border bg-card">
          <Table data-testid="centres-table">
            <TableHeader>
              <TableRow>
                <TableHead>Centre</TableHead>
                <TableHead>Codes</TableHead>
                <TableHead className="text-right">Students</TableHead>
                <TableHead className="text-right">Logins</TableHead>
                <TableHead>Messaging</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {q.data!.map((c) => (
                <TableRow key={c.id} data-testid={`centre-row-${c.id}`}>
                  <TableCell>
                    <Link to={`/platform/centres/${c.id}`} className="font-medium text-primary hover:underline">{c.name}</Link>
                    {c.contactEmail && <div className="text-xs text-muted-foreground">{c.contactEmail}</div>}
                  </TableCell>
                  <TableCell className="font-mono text-xs">{c.studentCodePrefix}001…</TableCell>
                  <TableCell className="text-right">{c.studentCount}</TableCell>
                  <TableCell className="text-right">{c.userCount}</TableCell>
                  <TableCell>
                    <div className="flex gap-1">
                      <Badge variant={c.emailConfigured ? 'green' : 'gray'}>Email {c.emailConfigured ? 'on' : 'off'}</Badge>
                      <Badge variant={c.smsConfigured ? 'green' : 'gray'}>SMS {c.smsConfigured ? 'on' : 'off'}</Badge>
                    </div>
                  </TableCell>
                  <TableCell>{c.active ? <Badge variant="green">Active</Badge> : <Badge variant="red">Inactive</Badge>}</TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-1">
                      <Button size="sm" variant="outline" asChild>
                        <Link to={`/platform/centres/${c.id}`}><Building2 /> Manage</Link>
                      </Button>
                      <Button size="sm" onClick={() => open(c)} data-testid={`centre-open-${c.id}`}><LogIn /> Open</Button>
                    </div>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
      <CreateCentreDialog open={creating} onOpenChange={setCreating} />
    </div>
  )
}

function CreateCentreDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (o: boolean) => void }) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [centre, setCentre] = useState<CentreInput>(EMPTY_CENTRE)
  const [withUser, setWithUser] = useState(true)
  const [user, setUser] = useState<UserInput>({ username: '', fullName: '', password: '' })
  const [error, setError] = useState<string | null>(null)

  const reset = () => {
    setCentre(EMPTY_CENTRE)
    setWithUser(true)
    setUser({ username: '', fullName: '', password: '' })
    setError(null)
  }

  const create = useMutation({
    mutationFn: () => platformApi.createCentre(
      { ...centre, name: centre.name.trim(), studentCodePrefix: centre.studentCodePrefix.trim() },
      withUser ? { username: user.username.trim(), fullName: user.fullName?.trim() || null, password: user.password } : null,
    ),
    onSuccess: (c) => {
      toast.success(`${c.name} added`)
      queryClient.invalidateQueries({ queryKey: ['platform', 'centres'] })
      onOpenChange(false)
      reset()
      navigate(`/platform/centres/${c.id}`)
    },
    onError: (e) => setError(errorMessage(e)),
  })

  const valid = centre.name.trim() && /^[A-Za-z]{1,10}$/.test(centre.studentCodePrefix.trim())
    && (!withUser || (user.username.trim().length >= 3 && user.password.length >= 8))

  return (
    <Dialog open={open} onOpenChange={(o) => { onOpenChange(o); if (!o) reset() }}>
      <DialogContent className="max-h-[90vh] max-w-xl overflow-y-auto" data-testid="centre-create-dialog">
        <DialogHeader>
          <DialogTitle>Add a tuition centre</DialogTitle>
          <DialogDescription>The centre starts empty. Its staff add their own students, batches, fee rates and class calendar.</DialogDescription>
        </DialogHeader>
        <form noValidate onSubmit={(e) => { e.preventDefault(); create.mutate() }} className="space-y-4">
          <CentreFields value={centre} onChange={setCentre} />
          <fieldset className="space-y-3 rounded-md border p-3">
            <label className="flex items-center gap-2 text-sm font-medium">
              <input type="checkbox" checked={withUser} onChange={(e) => setWithUser(e.target.checked)} data-testid="centre-with-user" />
              Create the first staff login now
            </label>
            {withUser && <NewUserFields value={user} onChange={setUser} idPrefix="first-user" />}
          </fieldset>
          {error && <p role="alert" className="text-sm text-destructive">{error}</p>}
          <DialogFooter>
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>Cancel</Button>
            <Button type="submit" disabled={!valid || create.isPending} data-testid="centre-save">
              {create.isPending && <Loader2 className="animate-spin" />} Add centre
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
