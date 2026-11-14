import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { KeyRound, Loader2, Plus, Trash2, UserCheck, UserX } from 'lucide-react'
import { toast } from 'sonner'
import { platformApi } from '@/api/platformApi'
import { ConfirmDialog } from '@/components/ConfirmDialog'
import { FormField } from '@/components/FormField'
import { ErrorState, TableSkeleton } from '@/components/States'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { useAuth } from '@/hooks/useAuth'
import { formatDate } from '@/lib/dates'
import { errorMessage } from '@/lib/errors'
import type { PortalUserRow, UserInput } from '@/types'

/** The username / name / password fields for a new login. */
export function NewUserFields({ value, onChange, idPrefix }: { value: UserInput; onChange: (v: UserInput) => void; idPrefix: string }) {
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      <FormField id={`${idPrefix}-username`} label="Username" required hint="Used to sign in">
        <Input id={`${idPrefix}-username`} autoComplete="off" value={value.username} onChange={(e) => onChange({ ...value, username: e.target.value })} data-testid={`${idPrefix}-username`} />
      </FormField>
      <FormField id={`${idPrefix}-name`} label="Full name">
        <Input id={`${idPrefix}-name`} value={value.fullName ?? ''} onChange={(e) => onChange({ ...value, fullName: e.target.value })} data-testid={`${idPrefix}-name`} />
      </FormField>
      <FormField id={`${idPrefix}-password`} label="Password" required hint="At least 8 characters. Share it with them; they can change it after signing in." className="sm:col-span-2">
        <Input id={`${idPrefix}-password`} type="text" autoComplete="new-password" value={value.password} onChange={(e) => onChange({ ...value, password: e.target.value })} data-testid={`${idPrefix}-password`} />
      </FormField>
    </div>
  )
}

const EMPTY: UserInput = { username: '', fullName: '', password: '' }

/** A list of logins with add, reset password, disable/enable and delete. */
export function UsersPanel({ queryKey, load, add, addLabel, emptyText }: {
  queryKey: unknown[]
  load: () => Promise<PortalUserRow[]>
  add: (u: UserInput) => Promise<PortalUserRow>
  addLabel: string
  emptyText: string
}) {
  const { user: me } = useAuth()
  const queryClient = useQueryClient()
  const q = useQuery({ queryKey, queryFn: load })
  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey })
    queryClient.invalidateQueries({ queryKey: ['platform', 'centres'] })
  }

  const [adding, setAdding] = useState(false)
  const [draft, setDraft] = useState<UserInput>(EMPTY)
  const [addError, setAddError] = useState<string | null>(null)
  const [resetting, setResetting] = useState<PortalUserRow | null>(null)
  const [newPassword, setNewPassword] = useState('')
  const [deleting, setDeleting] = useState<PortalUserRow | null>(null)

  const create = useMutation({
    mutationFn: () => add({ username: draft.username.trim(), fullName: draft.fullName?.trim() || null, password: draft.password }),
    onSuccess: (u) => {
      toast.success(`Login '${u.username}' created`)
      setAdding(false)
      invalidate()
    },
    onError: (e) => setAddError(errorMessage(e)),
  })

  const toggle = useMutation({
    mutationFn: (u: PortalUserRow) => platformApi.updateUser(u.id, { enabled: !u.enabled }),
    onSuccess: (u) => {
      toast.success(u.enabled ? `${u.username} can sign in again` : `${u.username} can no longer sign in`)
      invalidate()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const reset = useMutation({
    mutationFn: () => platformApi.resetPassword(resetting!.id, newPassword),
    onSuccess: () => {
      toast.success(`Password changed for ${resetting!.username}`)
      setResetting(null)
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const remove = useMutation({
    mutationFn: () => platformApi.deleteUser(deleting!.id),
    onSuccess: () => {
      toast.success(`Login '${deleting!.username}' deleted`)
      setDeleting(null)
      invalidate()
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  return (
    <div className="space-y-3">
      <div className="flex justify-end">
        <Button onClick={() => { setDraft(EMPTY); setAddError(null); setAdding(true) }} data-testid="user-add"><Plus /> {addLabel}</Button>
      </div>
      {q.isLoading ? (
        <TableSkeleton rows={3} />
      ) : q.isError ? (
        <ErrorState error={q.error} onRetry={() => q.refetch()} />
      ) : q.data!.length === 0 ? (
        <p className="rounded-lg border border-dashed p-6 text-center text-sm text-muted-foreground">{emptyText}</p>
      ) : (
        <div className="overflow-x-auto rounded-lg border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Username</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>Status</TableHead>
                <TableHead>Added</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {q.data!.map((u) => {
                const self = u.username.toLowerCase() === me?.username.toLowerCase()
                return (
                  <TableRow key={u.id} data-testid={`user-row-${u.username}`}>
                    <TableCell className="font-medium">{u.username}{self && <span className="ml-1 text-xs text-muted-foreground">(you)</span>}</TableCell>
                    <TableCell>{u.fullName ?? '—'}</TableCell>
                    <TableCell>{u.enabled ? <Badge variant="green">Active</Badge> : <Badge variant="gray">Disabled</Badge>}</TableCell>
                    <TableCell className="text-muted-foreground">{formatDate(u.createdAt.slice(0, 10))}</TableCell>
                    <TableCell>
                      <div className="flex justify-end gap-1">
                        <Button size="sm" variant="ghost" onClick={() => { setNewPassword(''); setResetting(u) }} aria-label={`Reset password for ${u.username}`}>
                          <KeyRound /> <span className="hidden md:inline">Reset password</span>
                        </Button>
                        {!self && (
                          <Button size="sm" variant="ghost" onClick={() => toggle.mutate(u)} disabled={toggle.isPending} aria-label={u.enabled ? `Disable ${u.username}` : `Enable ${u.username}`}>
                            {u.enabled ? <UserX /> : <UserCheck />} <span className="hidden md:inline">{u.enabled ? 'Disable' : 'Enable'}</span>
                          </Button>
                        )}
                        {!self && (
                          <Button size="sm" variant="ghost" className="text-destructive" onClick={() => setDeleting(u)} aria-label={`Delete ${u.username}`}>
                            <Trash2 />
                          </Button>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                )
              })}
            </TableBody>
          </Table>
        </div>
      )}

      <Dialog open={adding} onOpenChange={setAdding}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>{addLabel}</DialogTitle>
            <DialogDescription>They sign in with this username and password.</DialogDescription>
          </DialogHeader>
          <form noValidate onSubmit={(e) => { e.preventDefault(); create.mutate() }} className="space-y-3">
            <NewUserFields value={draft} onChange={setDraft} idPrefix="new-user" />
            {addError && <p role="alert" className="text-sm text-destructive">{addError}</p>}
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setAdding(false)}>Cancel</Button>
              <Button type="submit" disabled={create.isPending || !draft.username.trim() || draft.password.length < 8} data-testid="user-save">
                {create.isPending && <Loader2 className="animate-spin" />} Create login
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <Dialog open={!!resetting} onOpenChange={(o) => !o && setResetting(null)}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>Reset password for {resetting?.username}</DialogTitle>
            <DialogDescription>Their old password stops working straight away.</DialogDescription>
          </DialogHeader>
          <form noValidate onSubmit={(e) => { e.preventDefault(); reset.mutate() }} className="space-y-3">
            <FormField id="reset-password" label="New password" hint="At least 8 characters">
              <Input id="reset-password" type="text" autoComplete="new-password" value={newPassword} onChange={(e) => setNewPassword(e.target.value)} data-testid="reset-password" />
            </FormField>
            <DialogFooter>
              <Button type="button" variant="outline" onClick={() => setResetting(null)}>Cancel</Button>
              <Button type="submit" disabled={reset.isPending || newPassword.length < 8}>
                {reset.isPending && <Loader2 className="animate-spin" />} Set password
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={!!deleting}
        onOpenChange={(o) => !o && setDeleting(null)}
        title={`Delete login '${deleting?.username}'?`}
        description="They will no longer be able to sign in. The centre's data is not affected. To pause access instead, use Disable."
        confirmLabel="Delete"
        destructive
        pending={remove.isPending}
        onConfirm={() => remove.mutate()}
      />
    </div>
  )
}
