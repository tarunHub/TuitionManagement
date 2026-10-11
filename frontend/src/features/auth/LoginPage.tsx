import { useState } from 'react'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Eye, EyeOff, Loader2 } from 'lucide-react'
import { FormField } from '@/components/FormField'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/hooks/useAuth'
import { errorMessage } from '@/lib/errors'

const schema = z.object({
  username: z.string().trim().min(1, 'Username is required'),
  password: z.string().min(1, 'Password is required'),
})

type Values = z.infer<typeof schema>

export function LoginPage() {
  const { token, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const { register, handleSubmit, formState } = useForm<Values>({ resolver: zodResolver(schema) })

  const from = (location.state as { from?: string } | null)?.from ?? '/dashboard'
  if (token) return <Navigate to={from} replace />

  const onSubmit = handleSubmit(async (v) => {
    setError(null)
    try {
      await login(v.username, v.password)
      navigate(from, { replace: true })
    } catch (e) {
      setError(errorMessage(e, 'Login failed'))
    }
  })

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-100 p-4">
      <Card className="w-full max-w-sm">
        <CardHeader className="p-6 pb-2">
          <CardTitle className="text-xl">Smart Start</CardTitle>
          <CardDescription>Sign in to the student portal</CardDescription>
        </CardHeader>
        <CardContent className="p-6 pt-2">
          <form onSubmit={onSubmit} className="space-y-4" noValidate data-testid="login-form">
            {error && (
              <div role="alert" className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-sm text-destructive" data-testid="login-error">
                {error}
              </div>
            )}
            <FormField id="username" label="Username" error={formState.errors.username?.message}>
              <Input id="username" autoComplete="username" autoFocus {...register('username')} data-testid="login-username" />
            </FormField>
            <FormField id="password" label="Password" error={formState.errors.password?.message}>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  className="pr-10"
                  {...register('password')}
                  data-testid="login-password"
                />
                <button
                  type="button"
                  className="absolute right-2 top-2 text-muted-foreground hover:text-foreground"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  onClick={() => setShowPassword((s) => !s)}
                  data-testid="toggle-password"
                >
                  {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
            </FormField>
            <Button type="submit" className="w-full" disabled={formState.isSubmitting} data-testid="login-submit">
              {formState.isSubmitting && <Loader2 className="animate-spin" />}
              Sign in
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  )
}
