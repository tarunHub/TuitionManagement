import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Eye, Loader2, Save, Send, Timer } from 'lucide-react'
import { toast } from 'sonner'
import { notificationsApi, templatesApi } from '@/api/notificationsApi'
import { studentsApi } from '@/api/studentsApi'
import { FormField } from '@/components/FormField'
import { PageHeader } from '@/components/PageHeader'
import { ErrorState, TableSkeleton } from '@/components/States'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { NativeSelect } from '@/components/ui/native-select'
import { Textarea } from '@/components/ui/textarea'
import { useClassGroups } from '@/hooks/useReferenceData'
import { currentMonth, isMonthParam } from '@/lib/dates'
import { apiError, errorMessage } from '@/lib/errors'
import { NOTIFICATION_TYPES, type ComposeAction, type ComposeInput, type NotificationChannel, type NotificationType, type PreviewResult, type RecipientMode } from '@/types'
import { typeLabel } from './NotificationsPage'

const MODES: { value: RecipientMode; label: string }[] = [
  { value: 'ALL_ACTIVE', label: 'All active parents' },
  { value: 'CLASS_GROUPS', label: 'By class group' },
  { value: 'STUDENTS', label: 'Individual students' },
  { value: 'NONE', label: 'Only the extras below' },
]

/** Rough SMS length: 160 chars per message (153 when split), or 70/67 once any non-GSM character (é, –, emoji…) appears. */
export function smsSegments(text: string) {
  const unicode = /[^\n\r\x20-\x7E£¥èéùìòÇØøÅåΔ_ΦΓΛΩΠΨΣΘΞÆæßÉ¤¡ÄÖÑÜ§¿äöñüà]/.test(text)
  const [single, multi] = unicode ? [70, 67] : [160, 153]
  const length = [...text].length
  return { length, unicode, segments: length <= single ? 1 : Math.ceil(length / multi) }
}

/** datetime-local value (local time) → ISO instant. */
const toInstant = (local: string) => new Date(local).toISOString()
const toLocalInput = (iso: string) => {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export function ComposeNotificationPage() {
  const { id } = useParams()
  const editingId = id ? Number(id) : null
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const groups = useClassGroups()
  const templates = useQuery({ queryKey: ['templates'], queryFn: templatesApi.list })
  const placeholders = useQuery({ queryKey: ['placeholders'], queryFn: notificationsApi.placeholders, staleTime: Infinity })
  const channels = useQuery({ queryKey: ['notification-channels'], queryFn: notificationsApi.channels })
  const students = useQuery({ queryKey: ['students', 'active-all'], queryFn: () => studentsApi.list({ status: 'ACTIVE', size: 200, sort: 'fullName,asc' }) })
  const existing = useQuery({ queryKey: ['notification', editingId], queryFn: () => notificationsApi.get(editingId!), enabled: !!editingId })

  const [type, setType] = useState<NotificationType>('GENERAL')
  const [channel, setChannel] = useState<NotificationChannel>('EMAIL')
  const [templateId, setTemplateId] = useState<number | ''>('')
  const [subject, setSubject] = useState('')
  const [body, setBody] = useState('')
  const [mode, setMode] = useState<RecipientMode>('ALL_ACTIVE')
  const [groupIds, setGroupIds] = useState<number[]>([])
  const [studentIds, setStudentIds] = useState<number[]>([])
  const [studentSearch, setStudentSearch] = useState('')
  const [extraEmails, setExtraEmails] = useState('')
  const [extraPhones, setExtraPhones] = useState('')
  const [month, setMonth] = useState(currentMonth())
  const [scheduledAt, setScheduledAt] = useState('')
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [preview, setPreview] = useState<PreviewResult | null>(null)

  // Editing a draft: its stored text is already rendered, and its recipients become extra emails (or phones).
  useEffect(() => {
    const n = existing.data
    if (!n) return
    setType(n.type)
    setChannel(n.channel)
    setTemplateId(n.templateId ?? '')
    setSubject(n.subject)
    setBody(n.body)
    setMode('NONE')
    if (n.channel === 'SMS') setExtraPhones(n.recipients.join(', '))
    else setExtraEmails(n.recipients.join(', '))
    if (n.scheduledAt) setScheduledAt(toLocalInput(n.scheduledAt))
  }, [existing.data])

  const applyTemplate = (value: string) => {
    const tid = value ? Number(value) : ''
    setTemplateId(tid)
    const t = templates.data?.find((x) => x.id === tid)
    if (t) {
      setType(t.type)
      setSubject(t.subject)
      setBody(t.body)
      setPreview(null)
    }
  }

  const isSms = channel === 'SMS'
  const recipients = useMemo(
    () => ({
      mode,
      classGroupIds: mode === 'CLASS_GROUPS' ? groupIds : undefined,
      studentIds: mode === 'STUDENTS' ? studentIds : undefined,
      extraEmails: !isSms ? extraEmails.trim() || undefined : undefined,
      extraPhones: isSms ? extraPhones.trim() || undefined : undefined,
    }),
    [mode, groupIds, studentIds, extraEmails, extraPhones, isSms],
  )
  const sms = smsSegments(body)

  const validate = (action: ComposeAction | 'PREVIEW') => {
    const e: Record<string, string> = {}
    if (!subject.trim()) e.subject = isSms ? 'A title is required' : 'Subject is required'
    if (!body.trim()) e.body = 'Body is required'
    if (mode === 'CLASS_GROUPS' && groupIds.length === 0) e.recipients = 'Select at least one class group'
    if (mode === 'STUDENTS' && studentIds.length === 0) e.recipients = 'Select at least one student'
    if (mode === 'NONE' && !(isSms ? extraPhones : extraEmails).trim() && action !== 'DRAFT')
      e.recipients = isSms ? 'Enter at least one phone number' : 'Enter at least one email address'
    if (isSms && action !== 'DRAFT' && channels.data && !channels.data.sms) e.channel = 'SMS is not configured; save a draft or use email'
    if (action === 'SCHEDULE' && (!scheduledAt || new Date(scheduledAt) <= new Date())) e.scheduledAt = 'Pick a time in the future'
    setErrors(e)
    return Object.keys(e).length === 0
  }

  const previewMutation = useMutation({
    mutationFn: () => notificationsApi.preview({ subject, body, channel, recipients, month }),
    onSuccess: setPreview,
    onError: (e) => toast.error(errorMessage(e)),
  })

  const submit = useMutation({
    mutationFn: (action: ComposeAction) => {
      const input: ComposeInput = {
        type,
        templateId: templateId || null,
        subject,
        body,
        channel,
        recipients,
        action,
        scheduledAt: action === 'SCHEDULE' ? toInstant(scheduledAt) : null,
        month,
      }
      return editingId ? notificationsApi.update(editingId, input) : notificationsApi.create(input)
    },
    onSuccess: (n) => {
      if (n.status === 'SENT') toast.success(`Sent to ${n.recipientCount} recipient(s)`)
      else if (n.status === 'FAILED') toast.error(`Sending failed: ${n.errorMessage} — you can retry from the list`)
      else if (n.status === 'SCHEDULED') toast.success('Notification scheduled')
      else toast.success('Draft saved')
      queryClient.invalidateQueries({ queryKey: ['notifications'] })
      navigate('/notifications')
    },
    onError: (e) => {
      const api = apiError(e)
      const fe: Record<string, string> = {}
      api?.fieldErrors?.forEach((f) => (fe[f.field.startsWith('recipients') ? 'recipients' : f.field] = f.message))
      if (Object.keys(fe).length) setErrors(fe)
      toast.error(errorMessage(e))
    },
  })

  const run = (action: ComposeAction) => validate(action) && submit.mutate(action)

  const filteredStudents = (students.data?.content ?? []).filter((s) =>
    `${s.studentCode} ${s.fullName} ${s.guardianName}`.toLowerCase().includes(studentSearch.toLowerCase()),
  )
  const toggle = (list: number[], value: number) => (list.includes(value) ? list.filter((x) => x !== value) : [...list, value])

  if (editingId && existing.isLoading) return <TableSkeleton />
  if (editingId && existing.isError) return <ErrorState error={existing.error} />

  return (
    <div>
      <PageHeader title={editingId ? 'Edit notification' : 'Compose notification'} description="Placeholders like {{guardianName}} are filled in when the message is created." />
      <div className="grid gap-4 xl:grid-cols-[1fr_24rem]">
        <div className="space-y-4">
          <Card>
            <CardHeader><CardTitle>Message</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              <fieldset className="space-y-1" aria-label="Channel">
                <div className="flex flex-wrap gap-4">
                  {(['EMAIL', 'SMS'] as const).map((c) => (
                    <label key={c} className="flex items-center gap-2 text-sm">
                      <input type="radio" name="channel" value={c} checked={channel === c} onChange={() => { setChannel(c); setPreview(null) }} data-testid={`channel-${c}`} />
                      {c === 'EMAIL' ? 'Email' : 'SMS (text message)'}
                    </label>
                  ))}
                </div>
                {isSms && channels.data && !channels.data.sms && (
                  <p className="text-xs text-amber-700" data-testid="sms-not-configured">{channels.data.smsMessage}</p>
                )}
                {errors.channel && <p role="alert" className="text-xs font-medium text-destructive">{errors.channel}</p>}
              </fieldset>
              <div className="grid gap-3 sm:grid-cols-3">
                <FormField id="n-type" label="Type" required>
                  <NativeSelect id="n-type" value={type} onChange={(e) => setType(e.target.value as NotificationType)} data-testid="compose-type">
                    {NOTIFICATION_TYPES.map((t) => <option key={t} value={t}>{typeLabel(t)}</option>)}
                  </NativeSelect>
                </FormField>
                <FormField id="n-template" label="Template">
                  <NativeSelect id="n-template" value={templateId} onChange={(e) => applyTemplate(e.target.value)} data-testid="compose-template">
                    <option value="">None</option>
                    {templates.data?.map((t) => <option key={t.id} value={t.id}>{t.name}</option>)}
                  </NativeSelect>
                </FormField>
                <FormField id="n-month" label="Month for {{month}} / fees">
                  <Input id="n-month" type="month" value={month} onChange={(e) => isMonthParam(e.target.value) && setMonth(e.target.value)} />
                </FormField>
              </div>
              <FormField id="n-subject" label={isSms ? 'Title' : 'Subject'} required error={errors.subject} hint={isSms ? 'Shown in the notification history only; not part of the text' : undefined}>
                <Input id="n-subject" value={subject} maxLength={200} onChange={(e) => setSubject(e.target.value)} data-testid="compose-subject" />
              </FormField>
              <FormField id="n-body" label="Body" required error={errors.body}>
                <Textarea id="n-body" rows={isSms ? 5 : 12} value={body} onChange={(e) => setBody(e.target.value)} className="font-mono text-xs" data-testid="compose-body" />
              </FormField>
              {isSms && (
                <p className="text-xs text-muted-foreground" data-testid="sms-length">
                  {sms.length} characters · about {sms.segments} SMS per parent{sms.unicode ? ' (special characters shorten each SMS to 70)' : ''}. Placeholders change the final length.
                </p>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>Recipients</CardTitle></CardHeader>
            <CardContent className="space-y-3">
              <fieldset className="flex flex-wrap gap-4" aria-label="Recipient mode">
                {MODES.map((m) => (
                  <label key={m.value} className="flex items-center gap-2 text-sm">
                    <input type="radio" name="mode" value={m.value} checked={mode === m.value} onChange={() => setMode(m.value)} data-testid={`mode-${m.value}`} />
                    {m.label}
                  </label>
                ))}
              </fieldset>
              {mode === 'CLASS_GROUPS' && (
                <div className="flex flex-wrap gap-3 rounded-md border p-3">
                  {groups.data?.map((g) => (
                    <label key={g.id} className="flex items-center gap-2 text-sm">
                      <input type="checkbox" checked={groupIds.includes(g.id)} onChange={() => setGroupIds(toggle(groupIds, g.id))} />
                      {g.name} <span className="text-muted-foreground">({g.activeStudentCount})</span>
                    </label>
                  ))}
                </div>
              )}
              {mode === 'STUDENTS' && (
                <div className="space-y-2 rounded-md border p-3">
                  <Input aria-label="Search students" placeholder="Search students" value={studentSearch} onChange={(e) => setStudentSearch(e.target.value)} />
                  <div className="max-h-56 space-y-1 overflow-y-auto">
                    {filteredStudents.map((s) => (
                      <label key={s.id} className="flex items-center gap-2 text-sm">
                        <input type="checkbox" checked={studentIds.includes(s.id)} onChange={() => setStudentIds(toggle(studentIds, s.id))} data-testid={`pick-student-${s.studentCode}`} />
                        {s.studentCode} {s.fullName} <span className="text-muted-foreground">— {s.guardianName}</span>
                      </label>
                    ))}
                  </div>
                  <p className="text-xs text-muted-foreground">{studentIds.length} selected. With exactly one student, their name, guardian and fee values fill the placeholders.</p>
                </div>
              )}
              {isSms ? (
                <>
                  <p className="text-xs text-muted-foreground">Texts go to each student's primary contact, only for parents who opted in to SMS on the student form.</p>
                  <FormField id="n-extra-phones" label="Extra phone numbers" hint="Comma separated (optional), e.g. +1 416 555 0101">
                    <Input id="n-extra-phones" value={extraPhones} onChange={(e) => setExtraPhones(e.target.value)} data-testid="compose-extra-phones" />
                  </FormField>
                </>
              ) : (
                <FormField id="n-extra" label="Extra emails" hint="Comma separated (optional)">
                  <Input id="n-extra" value={extraEmails} onChange={(e) => setExtraEmails(e.target.value)} data-testid="compose-extra-emails" />
                </FormField>
              )}
              {errors.recipients && <p role="alert" className="text-xs font-medium text-destructive">{errors.recipients}</p>}
            </CardContent>
          </Card>

          <Card>
            <CardContent className="flex flex-wrap items-end gap-2 p-4">
              <Button variant="outline" onClick={() => validate('PREVIEW') && previewMutation.mutate()} disabled={previewMutation.isPending} data-testid="compose-preview">
                {previewMutation.isPending ? <Loader2 className="animate-spin" /> : <Eye />} Preview
              </Button>
              <Button variant="outline" onClick={() => run('DRAFT')} disabled={submit.isPending} data-testid="compose-draft"><Save /> Save draft</Button>
              <div className="ml-auto flex flex-wrap items-end gap-2">
                <FormField id="n-when" label="Schedule for" error={errors.scheduledAt}>
                  <Input id="n-when" type="datetime-local" value={scheduledAt} onChange={(e) => setScheduledAt(e.target.value)} className="w-56" />
                </FormField>
                <Button variant="outline" onClick={() => run('SCHEDULE')} disabled={submit.isPending} data-testid="compose-schedule"><Timer /> Schedule</Button>
                <Button onClick={() => run('SEND_NOW')} disabled={submit.isPending} data-testid="compose-send">
                  {submit.isPending ? <Loader2 className="animate-spin" /> : <Send />} Send now
                </Button>
              </div>
            </CardContent>
          </Card>
          <Button variant="link" asChild><Link to="/notifications">← Back to notifications</Link></Button>
        </div>

        <div className="space-y-4">
          <Card>
            <CardHeader><CardTitle>Preview</CardTitle></CardHeader>
            <CardContent data-testid="preview-panel">
              {!preview ? (
                <p className="text-sm text-muted-foreground">Click Preview to see the rendered {isSms ? 'text' : 'email'}.</p>
              ) : (
                <div className="space-y-2 text-sm">
                  <p><span className="text-muted-foreground">To:</span> {preview.recipients.length} recipient(s){preview.recipients.length ? ` — ${preview.recipients.slice(0, 5).join(', ')}${preview.recipients.length > 5 ? '…' : ''}` : ''}</p>
                  <p><span className="text-muted-foreground">{isSms ? 'Title:' : 'Subject:'}</span> <strong data-testid="preview-subject">{preview.subject}</strong></p>
                  <pre className="max-h-96 overflow-auto whitespace-pre-wrap rounded-md border bg-muted/40 p-3 font-mono text-xs" data-testid="preview-body">{preview.body}</pre>
                </div>
              )}
            </CardContent>
          </Card>
          <Card>
            <CardHeader><CardTitle>Placeholders</CardTitle></CardHeader>
            <CardContent>
              <ul className="space-y-1 text-xs">
                {placeholders.data?.map((p) => (
                  <li key={p.name}>
                    <button type="button" className="font-mono text-primary hover:underline" onClick={() => setBody((b) => `${b}{{${p.name}}}`)}>{`{{${p.name}}}`}</button>
                    <span className="text-muted-foreground"> — {p.description}</span>
                  </li>
                ))}
              </ul>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}
