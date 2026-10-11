import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { toast } from 'sonner'
import { authApi } from '@/api/authApi'
import { FormField } from '@/components/FormField'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { applyServerErrors, errorMessage } from '@/lib/errors'

const schema = z
  .object({
    currentPassword: z.string().min(1, 'Required'),
    newPassword: z.string().min(8, 'At least 8 characters').max(100),
    confirm: z.string(),
  })
  .refine((v) => v.newPassword === v.confirm, { path: ['confirm'], message: 'Passwords do not match' })

type Values = z.infer<typeof schema>

export function ChangePasswordDialog({ open, onOpenChange }: { open: boolean; onOpenChange: (o: boolean) => void }) {
  const { register, handleSubmit, setError, reset, formState } = useForm<Values>({ resolver: zodResolver(schema) })

  const onSubmit = handleSubmit(async (v) => {
    try {
      await authApi.changePassword(v.currentPassword, v.newPassword)
      toast.success('Password changed')
      reset()
      onOpenChange(false)
    } catch (e) {
      if (!applyServerErrors(e, setError, ['currentPassword', 'newPassword'])) toast.error(errorMessage(e))
    }
  })

  return (
    <Dialog open={open} onOpenChange={(o) => { if (!o) reset(); onOpenChange(o) }}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Change password</DialogTitle>
          <DialogDescription>Use at least 8 characters.</DialogDescription>
        </DialogHeader>
        <form onSubmit={onSubmit} className="space-y-3" noValidate>
          <FormField id="currentPassword" label="Current password" error={formState.errors.currentPassword?.message} required>
            <Input id="currentPassword" type="password" autoComplete="current-password" {...register('currentPassword')} />
          </FormField>
          <FormField id="newPassword" label="New password" error={formState.errors.newPassword?.message} required>
            <Input id="newPassword" type="password" autoComplete="new-password" {...register('newPassword')} />
          </FormField>
          <FormField id="confirm" label="Confirm new password" error={formState.errors.confirm?.message} required>
            <Input id="confirm" type="password" autoComplete="new-password" {...register('confirm')} />
          </FormField>
          <DialogFooter>
            <Button type="submit" disabled={formState.isSubmitting}>Change password</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
