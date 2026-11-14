import { FormField } from '@/components/FormField'
import { Input } from '@/components/ui/input'
import type { CentreInput } from '@/types'

export const EMPTY_CENTRE: CentreInput = { name: '', studentCodePrefix: '', contactEmail: '', contactPhone: '', address: '' }

/** Name, student code prefix and contact details of a centre. */
export function CentreFields({ value, onChange, prefixLocked }: { value: CentreInput; onChange: (v: CentreInput) => void; prefixLocked?: boolean }) {
  const set = <K extends keyof CentreInput>(k: K, v: CentreInput[K]) => onChange({ ...value, [k]: v })
  const prefixOk = /^[A-Za-z]{1,10}$/.test(value.studentCodePrefix.trim())
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      <FormField id="centre-name" label="Centre name" required>
        <Input id="centre-name" value={value.name} onChange={(e) => set('name', e.target.value)} data-testid="centre-name" />
      </FormField>
      <FormField
        id="centre-prefix"
        label="Student code prefix"
        required
        error={value.studentCodePrefix && !prefixOk ? '1-10 letters only' : undefined}
        hint={prefixLocked ? 'New students get this prefix; existing codes stay as they are.' : `Students will be ${(value.studentCodePrefix.trim() || 'AB').toUpperCase()}001, ${(value.studentCodePrefix.trim() || 'AB').toUpperCase()}002…`}
      >
        <Input id="centre-prefix" maxLength={10} value={value.studentCodePrefix} onChange={(e) => set('studentCodePrefix', e.target.value.toUpperCase())} data-testid="centre-prefix" />
      </FormField>
      <FormField id="centre-email" label="Contact email">
        <Input id="centre-email" type="email" value={value.contactEmail ?? ''} onChange={(e) => set('contactEmail', e.target.value)} />
      </FormField>
      <FormField id="centre-phone" label="Contact phone">
        <Input id="centre-phone" value={value.contactPhone ?? ''} onChange={(e) => set('contactPhone', e.target.value)} />
      </FormField>
      <FormField id="centre-address" label="Address" className="sm:col-span-2">
        <Input id="centre-address" value={value.address ?? ''} onChange={(e) => set('address', e.target.value)} />
      </FormField>
    </div>
  )
}
