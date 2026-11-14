import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Loader2, Mail, MessageSquare, Send } from 'lucide-react'
import { toast } from 'sonner'
import { centreSettingsApi } from '@/api/platformApi'
import { FormField } from '@/components/FormField'
import { PageHeader } from '@/components/PageHeader'
import { ErrorState, TableSkeleton } from '@/components/States'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { errorMessage } from '@/lib/errors'
import type { MessagingSettings, MessagingSettingsInput } from '@/types'

function toInput(s: MessagingSettings): MessagingSettingsInput {
  return {
    mailFrom: s.mailFrom, mailFromName: s.mailFromName, smtpHost: s.smtpHost, smtpPort: s.smtpPort, smtpUsername: s.smtpUsername,
    smtpPassword: '', clearSmtpPassword: false, smtpAuth: s.smtpAuth, smtpStarttls: s.smtpStarttls,
    smsEnabled: s.smsEnabled, smsGatewayUrl: s.smsGatewayUrl, smsUsername: s.smsUsername, smsPassword: '', clearSmsPassword: false,
    smsCountryCode: s.smsCountryCode,
  }
}

/** Common providers, so most centres only type their address and app password. */
const PRESETS = [
  { label: 'Gmail', host: 'smtp.gmail.com', port: 587 },
  { label: 'Outlook / Microsoft 365', host: 'smtp.office365.com', port: 587 },
  { label: 'Yahoo', host: 'smtp.mail.yahoo.com', port: 587 },
]

/** This centre's own email (SMTP) and SMS gateway, used for every message it sends to parents. */
export function CentreSettingsPage() {
  const queryClient = useQueryClient()
  const q = useQuery({ queryKey: ['centre-settings'], queryFn: centreSettingsApi.get })
  const [form, setForm] = useState<MessagingSettingsInput | null>(null)
  const [testTo, setTestTo] = useState('')

  useEffect(() => {
    if (q.data) setForm(toInput(q.data))
  }, [q.data])

  const save = useMutation({
    mutationFn: (input: MessagingSettingsInput) => centreSettingsApi.update(input),
    onSuccess: (s) => {
      queryClient.setQueryData(['centre-settings'], s)
      queryClient.invalidateQueries({ queryKey: ['notification-channels'] })
      queryClient.invalidateQueries({ queryKey: ['reminder-settings'] })
      toast.success('Settings saved')
    },
    onError: (e) => toast.error(errorMessage(e)),
  })

  const test = useMutation({
    mutationFn: () => centreSettingsApi.testEmail(testTo.trim()),
    onSuccess: () => toast.success(`Test email sent to ${testTo.trim()}. Check the inbox (and spam).`),
    onError: (e) => toast.error(errorMessage(e)),
  })

  if (q.isLoading || (!form && !q.isError)) return <TableSkeleton rows={8} />
  if (q.isError) return <ErrorState error={q.error} onRetry={() => q.refetch()} />
  const s = q.data!
  const f = form!
  const set = <K extends keyof MessagingSettingsInput>(k: K, v: MessagingSettingsInput[K]) => setForm({ ...f, [k]: v })
  const text = (v: string) => (v.trim() === '' ? null : v)
  const dirty = JSON.stringify(f) !== JSON.stringify(toInput(s))

  return (
    <div className="space-y-4">
      <PageHeader
        title="Centre settings"
        description={`How ${s.centreName} sends email and text messages to parents. Student codes start with ${s.studentCodePrefix}.`}
        actions={
          <Button onClick={() => save.mutate(f)} disabled={!dirty || save.isPending} data-testid="settings-save">
            {save.isPending && <Loader2 className="animate-spin" />} Save settings
          </Button>
        }
      />

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <Mail className="size-4" /> Email
            <Badge variant={s.emailConfigured ? 'green' : 'gray'}>{s.emailConfigured ? 'Set up' : 'Not set up'}</Badge>
          </CardTitle>
          <CardDescription>
            Parents receive emails from this address. For Gmail, turn on 2-step verification and create an
            app password (Google Account → Security → App passwords); use that instead of your normal password.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          <div className="flex flex-wrap items-center gap-2 text-sm">
            <span className="text-muted-foreground">Quick fill:</span>
            {PRESETS.map((p) => (
              <Button key={p.label} type="button" size="sm" variant="outline"
                onClick={() => setForm({ ...f, smtpHost: p.host, smtpPort: p.port, smtpAuth: true, smtpStarttls: true, smtpUsername: f.smtpUsername ?? f.mailFrom })}>
                {p.label}
              </Button>
            ))}
          </div>
          <div className="grid gap-3 sm:grid-cols-2">
            <FormField id="mail-from" label="From address" hint="e.g. office@yourcentre.com">
              <Input id="mail-from" type="email" value={f.mailFrom ?? ''} onChange={(e) => set('mailFrom', text(e.target.value))} data-testid="mail-from" />
            </FormField>
            <FormField id="mail-from-name" label="From name" hint={`Shown to parents; defaults to ${s.centreName}`}>
              <Input id="mail-from-name" value={f.mailFromName ?? ''} onChange={(e) => set('mailFromName', text(e.target.value))} />
            </FormField>
            <FormField id="smtp-host" label="SMTP server">
              <Input id="smtp-host" value={f.smtpHost ?? ''} onChange={(e) => set('smtpHost', text(e.target.value))} placeholder="smtp.gmail.com" data-testid="smtp-host" />
            </FormField>
            <FormField id="smtp-port" label="Port">
              <Input id="smtp-port" inputMode="numeric" value={f.smtpPort ?? ''} onChange={(e) => set('smtpPort', e.target.value ? Number(e.target.value.replace(/\D/g, '')) || null : null)} placeholder="587" data-testid="smtp-port" />
            </FormField>
            <FormField id="smtp-username" label="Username">
              <Input id="smtp-username" autoComplete="off" value={f.smtpUsername ?? ''} onChange={(e) => set('smtpUsername', text(e.target.value))} />
            </FormField>
            <FormField id="smtp-password" label="Password" hint={s.smtpPasswordSet ? 'A password is saved. Leave blank to keep it.' : 'No password saved'}>
              <Input id="smtp-password" type="password" autoComplete="new-password" value={f.smtpPassword ?? ''} onChange={(e) => set('smtpPassword', e.target.value)} placeholder={s.smtpPasswordSet ? '••••••••' : ''} />
            </FormField>
          </div>
          <div className="flex flex-wrap gap-4 text-sm">
            <label className="flex items-center gap-2"><input type="checkbox" checked={f.smtpAuth} onChange={(e) => set('smtpAuth', e.target.checked)} /> Server needs sign-in</label>
            <label className="flex items-center gap-2"><input type="checkbox" checked={f.smtpStarttls} onChange={(e) => set('smtpStarttls', e.target.checked)} /> Use STARTTLS (port 587)</label>
            {s.smtpPasswordSet && (
              <label className="flex items-center gap-2"><input type="checkbox" checked={f.clearSmtpPassword} onChange={(e) => set('clearSmtpPassword', e.target.checked)} /> Remove saved password</label>
            )}
          </div>
          <div className="flex flex-wrap items-end gap-2 border-t pt-3">
            <FormField id="test-to" label="Send a test email to" className="min-w-0 flex-1 sm:max-w-xs">
              <Input id="test-to" type="email" value={testTo} onChange={(e) => setTestTo(e.target.value)} placeholder="you@example.com" data-testid="test-to" />
            </FormField>
            <Button variant="outline" onClick={() => test.mutate()} disabled={!s.emailConfigured || dirty || !testTo.includes('@') || test.isPending} data-testid="test-email"
              title={dirty ? 'Save your changes first' : undefined}>
              {test.isPending ? <Loader2 className="animate-spin" /> : <Send />} Send test
            </Button>
            {dirty && <span className="pb-2 text-xs text-muted-foreground">Save first to test the new settings.</span>}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-base">
            <MessageSquare className="size-4" /> SMS
            <Badge variant={s.smsConfigured ? 'green' : 'gray'}>{s.smsConfigured ? 'On' : 'Off'}</Badge>
          </CardTitle>
          <CardDescription>
            Texts go out from the centre's own Android phone using the free "SMS Gateway" app (sms-gate.app). Copy the
            address, username and password shown in the app.
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          <label className="flex items-center gap-2 text-sm font-medium">
            <input type="checkbox" checked={f.smsEnabled} onChange={(e) => set('smsEnabled', e.target.checked)} data-testid="sms-enabled" /> Send text messages
          </label>
          <div className="grid gap-3 sm:grid-cols-2">
            <FormField id="sms-url" label="Gateway address" hint="Local (e.g. 10.0.0.12:8080) or cloud (https://api.sms-gate.app/3rdparty/v1/messages)" className="sm:col-span-2">
              <Input id="sms-url" value={f.smsGatewayUrl ?? ''} onChange={(e) => set('smsGatewayUrl', text(e.target.value))} data-testid="sms-url" />
            </FormField>
            <FormField id="sms-username" label="Username">
              <Input id="sms-username" autoComplete="off" value={f.smsUsername ?? ''} onChange={(e) => set('smsUsername', text(e.target.value))} />
            </FormField>
            <FormField id="sms-password" label="Password" hint={s.smsPasswordSet ? 'A password is saved. Leave blank to keep it.' : 'No password saved'}>
              <Input id="sms-password" type="password" autoComplete="new-password" value={f.smsPassword ?? ''} onChange={(e) => set('smsPassword', e.target.value)} placeholder={s.smsPasswordSet ? '••••••••' : ''} />
            </FormField>
            <FormField id="sms-country" label="Default country code" hint="Added to numbers saved without +, e.g. 1 (Canada/US) or 91 (India)">
              <Input id="sms-country" inputMode="numeric" value={f.smsCountryCode} onChange={(e) => set('smsCountryCode', e.target.value.replace(/\D/g, ''))} />
            </FormField>
          </div>
          {s.smsPasswordSet && (
            <label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={f.clearSmsPassword} onChange={(e) => set('clearSmsPassword', e.target.checked)} /> Remove saved password</label>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
